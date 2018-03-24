package org.apache.cloudstack.api.command;

import com.cloud.alert.AlertManager;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Network;
import com.cloud.network.Network.IpAddresses;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.ResourceLimitService;
import com.cloud.uservm.UserVm;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.UserVmDao;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.affinity.AffinityGroupResponse;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCustomIdCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DiskOfferingResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.SecurityGroupResponse;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.VolumeOrchestrationService;
import org.apache.cloudstack.exttools.ApiExtToolsServiceImpl;
import org.apache.cloudstack.exttools.UserVmServiceModified;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@APICommand(
        name = "deployHiddenVm",
        description = "Creates and automatically starts a virtual machine based on a service offering, disk offering, and template.",
        responseObject = UserVmResponse.class,
        responseView = ResponseView.Restricted,
        entityType = {VirtualMachine.class},
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = true
)
public class DeployHiddenVMCmd extends BaseAsyncCreateCustomIdCmd {

    @Inject
    private ApiExtToolsServiceImpl _apiExtToolsService;
    @Inject
    protected UserVmDao _vmDao = null;
    @Inject
    protected VMTemplateDao _templateDao = null;
    @Inject
    protected VirtualMachineManager _itMgr;
    @Inject
    protected VolumeDao _volsDao = null;
    @Inject
    VolumeOrchestrationService volumeMgr;
    @Inject
    protected AlertManager _alertMgr = null;
    @Inject
    protected ServiceOfferingDao _serviceOfferingDao;
    @Inject
    protected ResourceLimitService _resourceLimitMgr;
    //@Inject
    protected UserVmServiceModified _userVmServiceModified = new UserVmServiceModified();
    public static final Logger s_logger = Logger.getLogger(DeployHiddenVMCmd.class.getName());
    private static final String s_name = "deployvirtualmachineresponse";

    @Parameter(
            name = "zoneid",
            type = CommandType.UUID,
            entityType = {ZoneResponse.class},
            //required = true,
            description = "availability zone for the virtual machine"
    )
    private Long zoneId;
    @ACL
    @Parameter(
            name = "serviceofferingid",
            type = CommandType.UUID,
            entityType = {ServiceOfferingResponse.class},
            //required = true,
            description = "the ID of the service offering for the virtual machine"
    )
    private Long serviceOfferingId;
    @ACL
    @Parameter(
            name = "templateid",
            type = CommandType.UUID,
            entityType = {TemplateResponse.class},
            //required = true,
            description = "the ID of the template for the virtual machine"
    )
    private Long templateId;
    @Parameter(
            name = "name",
            type = CommandType.STRING,
            description = "host name for the virtual machine"
    )
    private String name;
    @Parameter(
            name = "displayname",
            type = CommandType.STRING,
            description = "an optional user generated name for the virtual machine"
    )
    private String displayName;
    @Parameter(
            name = "account",
            type = CommandType.STRING,
            description = "an optional account for the virtual machine. Must be used with domainId."
    )
    private String accountName;
    @Parameter(
            name = "domainid",
            type = CommandType.UUID,
            entityType = {DomainResponse.class},
            description = "an optional domainId for the virtual machine. If the account parameter is used, domainId must also be used."
    )
    private Long domainId;
    @Parameter(
            name = "networkids",
            type = CommandType.LIST,
            collectionType = CommandType.UUID,
            entityType = {NetworkResponse.class},
            description = "list of network ids used by virtual machine. Can't be specified with ipToNetworkList parameter"
    )
    private List<Long> networkIds;
    @ACL
    @Parameter(
            name = "diskofferingid",
            type = CommandType.UUID,
            entityType = {DiskOfferingResponse.class},
            description = "the ID of the disk offering for the virtual machine. If the template is of ISO format, the diskOfferingId is for the root disk volume. Otherwise this parameter is used to indicate the offering for the data disk volume. If the templateId parameter passed is from a Template object, the diskOfferingId refers to a DATA Disk Volume created. If the templateId parameter passed is from an ISO object, the diskOfferingId refers to a ROOT Disk Volume created."
    )
    private Long diskOfferingId;
    @Parameter(
            name = "size",
            type = CommandType.LONG,
            description = "the arbitrary size for the DATADISK volume. Mutually exclusive with diskOfferingId"
    )
    private Long size;
    @Parameter(
            name = "rootdisksize",
            type = CommandType.LONG,
            description = "Optional field to resize root disk on deploy. Value is in GB. Only applies to template-based deployments. Analogous to details[0].rootdisksize, which takes precedence over this parameter if both are provided",
            since = "4.4"
    )
    private Long rootdisksize;
    @Parameter(
            name = "group",
            type = CommandType.STRING,
            description = "an optional group for the virtual machine"
    )
    private String group;
    @Parameter(
            name = "hypervisor",
            type = CommandType.STRING,
            description = "the hypervisor on which to deploy the virtual machine. The parameter is required and respected only when hypervisor info is not set on the ISO/Template passed to the call"
    )
    private String hypervisor;
    @Parameter(
            name = "userdata",
            type = CommandType.STRING,
            description = "an optional binary data that can be sent to the virtual machine upon a successful deployment. This binary data must be base64 encoded before adding it to the request. Using HTTP GET (via querystring), you can send up to 2KB of data after base64 encoding. Using HTTP POST(via POST body), you can send up to 32K of data after base64 encoding.",
            length = 32768
    )
    private String userData;
    @Parameter(
            name = "keypair",
            type = CommandType.STRING,
            description = "name of the ssh key pair used to login to the virtual machine"
    )
    private String sshKeyPairName;
    @Parameter(
            name = "hostid",
            type = CommandType.UUID,
            entityType = {HostResponse.class},
            description = "destination Host ID to deploy the VM to - parameter available for root admin only"
    )
    private Long hostId;
    @ACL
    @Parameter(
            name = "securitygroupids",
            type = CommandType.LIST,
            collectionType = CommandType.UUID,
            entityType = {SecurityGroupResponse.class},
            description = "comma separated list of security groups id that going to be applied to the virtual machine. Should be passed only when vm is created from a zone with Basic Network support. Mutually exclusive with securitygroupnames parameter"
    )
    private List<Long> securityGroupIdList;
    @ACL
    @Parameter(
            name = "securitygroupnames",
            type = CommandType.LIST,
            collectionType = CommandType.STRING,
            entityType = {SecurityGroupResponse.class},
            description = "comma separated list of security groups names that going to be applied to the virtual machine. Should be passed only when vm is created from a zone with Basic Network support. Mutually exclusive with securitygroupids parameter"
    )
    private List<String> securityGroupNameList;
    @Parameter(
            name = "iptonetworklist",
            type = CommandType.MAP,
            description = "ip to network mapping. Can't be specified with networkIds parameter. Example: iptonetworklist[0].ip=10.10.10.11&iptonetworklist[0].ipv6=fc00:1234:5678::abcd&iptonetworklist[0].networkid=uuid - requests to use ip 10.10.10.11 in network id=uuid"
    )
    private Map ipToNetworkList;
    @Parameter(
            name = "ipaddress",
            type = CommandType.STRING,
            description = "the ip address for default vm's network"
    )
    private String ipAddress;
    @Parameter(
            name = "ip6address",
            type = CommandType.STRING,
            description = "the ipv6 address for default vm's network"
    )
    private String ip6Address;
    @Parameter(
            name = "keyboard",
            type = CommandType.STRING,
            description = "an optional keyboard device type for the virtual machine. valid value can be one of de,de-ch,es,fi,fr,fr-be,fr-ch,is,it,jp,nl-be,no,pt,uk,us"
    )
    private String keyboard;
    @Parameter(
            name = "projectid",
            type = CommandType.UUID,
            entityType = {ProjectResponse.class},
            description = "Deploy vm for the project"
    )
    private Long projectId;
    @Parameter(
            name = "startvm",
            type = CommandType.BOOLEAN,
            description = "true if start vm after creating; defaulted to true if not specified"
    )
    private Boolean startVm;
    @ACL
    @Parameter(
            name = "affinitygroupids",
            type = CommandType.LIST,
            collectionType = CommandType.UUID,
            entityType = {AffinityGroupResponse.class},
            description = "comma separated list of affinity groups id that are going to be applied to the virtual machine. Mutually exclusive with affinitygroupnames parameter"
    )
    private List<Long> affinityGroupIdList;
    @ACL
    @Parameter(
            name = "affinitygroupnames",
            type = CommandType.LIST,
            collectionType = CommandType.STRING,
            entityType = {AffinityGroupResponse.class},
            description = "comma separated list of affinity groups names that are going to be applied to the virtual machine.Mutually exclusive with affinitygroupids parameter"
    )
    private List<String> affinityGroupNameList;
    @Parameter(
            name = "displayvm",
            type = CommandType.BOOLEAN,
            since = "4.2",
            description = "an optional field, whether to the display the vm to the end user or not.",
            authorized = {RoleType.Admin}
    )
    private Boolean displayVm;
    @Parameter(
            name = "details",
            type = CommandType.MAP,
            since = "4.3",
            description = "used to specify the custom parameters."
    )
    private Map details;
    @Parameter(
            name = "deploymentplanner",
            type = CommandType.STRING,
            description = "Deployment planner to use for vm allocation. Available to ROOT admin only",
            since = "4.4",
            authorized = {RoleType.Admin}
    )
    private String deploymentPlanner;

    public DeployHiddenVMCmd() {
    }

    private void SetGlobalSettingsValues() {
        ConfigKey<?>[] configKeys = _apiExtToolsService.getConfigKeys();
        String configKeyZoneUuid = (String)configKeys[0].value();

        //TODO: replace zone to template
        DataCenter zone = _entityMgr.findByUuid(DataCenter.class, configKeyZoneUuid);
        //s_logger.debug("!!! zoneId=" + this.zoneId + ", config zoneId=" + zone.getId() + ", config zoneUuid=" + configKeyZoneUuid);

        if (this.zoneId == null) {
            DataCenter dataCenter = _entityMgr.findByUuid(DataCenter.class, configKeyZoneUuid); //zone
            this.zoneId = dataCenter.getId();
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////

    public String getAccountName() {
        return this.accountName == null ? CallContext.current().getCallingAccount().getAccountName() : this.accountName;
    }

    public Long getDiskOfferingId() {
        return this.diskOfferingId;
    }

    public String getDeploymentPlanner() {
        return this.deploymentPlanner;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public Long getDomainId() {
        return this.domainId == null ? CallContext.current().getCallingAccount().getDomainId() : this.domainId;
    }

    public Map<String, String> getDetails() {
        Map<String, String> customparameterMap = new HashMap();
        if (this.details != null && this.details.size() != 0) {
            Collection parameterCollection = this.details.values();
            Iterator iter = parameterCollection.iterator();

            while(iter.hasNext()) {
                HashMap<String, String> value = (HashMap)iter.next();
                Iterator var5 = value.entrySet().iterator();

                while(var5.hasNext()) {
                    Entry<String, String> entry = (Entry)var5.next();
                    customparameterMap.put(entry.getKey(), entry.getValue());
                }
            }
        }

        if (this.rootdisksize != null && !customparameterMap.containsKey("rootdisksize")) {
            customparameterMap.put("rootdisksize", this.rootdisksize.toString());
        }

        return customparameterMap;
    }

    public String getGroup() {
        return this.group;
    }

    public HypervisorType getHypervisor() {
        return HypervisorType.getType(this.hypervisor);
    }

    public Boolean getDisplayVm() {
        return this.displayVm;
    }

    public boolean isDisplay() {
        return this.displayVm == null ? true : this.displayVm;
    }

    public List<Long> getSecurityGroupIdList() {
        if (this.securityGroupNameList != null && this.securityGroupIdList != null) {
            throw new InvalidParameterValueException("securitygroupids parameter is mutually exclusive with securitygroupnames parameter");
        } else if (this.securityGroupNameList != null) {
            List<Long> securityGroupIds = new ArrayList();
            Iterator var2 = this.securityGroupNameList.iterator();

            while(var2.hasNext()) {
                String groupName = (String)var2.next();
                Long groupId = this._responseGenerator.getSecurityGroupId(groupName, this.getEntityOwnerId());
                if (groupId == null) {
                    throw new InvalidParameterValueException("Unable to find group by name " + groupName);
                }

                securityGroupIds.add(groupId);
            }

            return securityGroupIds;
        } else {
            return this.securityGroupIdList;
        }
    }

    public Long getServiceOfferingId() {
        return this.serviceOfferingId;
    }

    public Long getSize() {
        return this.size;
    }

    public Long getTemplateId() {
        return this.templateId;
    }

    public String getUserData() {
        return this.userData;
    }

    public Long getZoneId() {
        return this.zoneId;
    }

    public List<Long> getNetworkIds() {
        if (this.ipToNetworkList != null && !this.ipToNetworkList.isEmpty()) {
            if ((this.networkIds == null || this.networkIds.isEmpty()) && this.ipAddress == null && this.getIp6Address() == null) {
                List<Long> networks = new ArrayList();
                networks.addAll(this.getIpToNetworkMap().keySet());
                return networks;
            } else {
                throw new InvalidParameterValueException("ipToNetworkMap can't be specified along with networkIds or ipAddress");
            }
        } else {
            return this.networkIds;
        }
    }

    public String getName() {
        return this.name;
    }

    public String getSSHKeyPairName() {
        return this.sshKeyPairName;
    }

    public Long getHostId() {
        return this.hostId;
    }

    public boolean getStartVm() {
        return this.startVm == null ? true : this.startVm;
    }

    private Map<Long, IpAddresses> getIpToNetworkMap() {
        if ((this.networkIds != null || this.ipAddress != null || this.getIp6Address() != null) && this.ipToNetworkList != null) {
            throw new InvalidParameterValueException("NetworkIds and ipAddress can't be specified along with ipToNetworkMap parameter");
        } else {
            LinkedHashMap<Long, IpAddresses> ipToNetworkMap = null;
            if (this.ipToNetworkList != null && !this.ipToNetworkList.isEmpty()) {
                ipToNetworkMap = new LinkedHashMap();
                Collection ipsCollection = this.ipToNetworkList.values();
                Iterator iter = ipsCollection.iterator();

                while(iter.hasNext()) {
                    HashMap<String, String> ips = (HashMap)iter.next();
                    Network network = this._networkService.getNetwork((String)ips.get("networkid"));
                    Long networkId;
                    if (network != null) {
                        networkId = network.getId();
                    } else {
                        try {
                            networkId = Long.parseLong((String)ips.get("networkid"));
                        } catch (NumberFormatException var10) {
                            throw new InvalidParameterValueException("Unable to translate and find entity with networkId: " + (String)ips.get("networkid"));
                        }
                    }

                    String requestedIp = (String)ips.get("ip");
                    String requestedIpv6 = (String)ips.get("ipv6");
                    if (requestedIpv6 != null) {
                        requestedIpv6 = NetUtils.standardizeIp6Address(requestedIpv6);
                    }

                    IpAddresses addrs = new IpAddresses(requestedIp, requestedIpv6);
                    ipToNetworkMap.put(networkId, addrs);
                }
            }

            return ipToNetworkMap;
        }
    }

    public String getIp6Address() {
        return this.ip6Address == null ? null : NetUtils.standardizeIp6Address(this.ip6Address);
    }

    public List<Long> getAffinityGroupIdList() {
        if (this.affinityGroupNameList != null && this.affinityGroupIdList != null) {
            throw new InvalidParameterValueException("affinitygroupids parameter is mutually exclusive with affinitygroupnames parameter");
        } else if (this.affinityGroupNameList != null) {
            List<Long> affinityGroupIds = new ArrayList();
            Iterator var2 = this.affinityGroupNameList.iterator();

            while(var2.hasNext()) {
                String groupName = (String)var2.next();
                Long groupId = this._responseGenerator.getAffinityGroupId(groupName, this.getEntityOwnerId());
                if (groupId == null) {
                    throw new InvalidParameterValueException("Unable to find affinity group by name " + groupName);
                }

                affinityGroupIds.add(groupId);
            }

            return affinityGroupIds;
        } else {
            return this.affinityGroupIdList;
        }
    }

    public String getCommandName() {
        return "deployvirtualmachineresponse";
    }

    public static String getResultObjectName() {
        return "virtualmachine";
    }

    public long getEntityOwnerId() {
        Long accountId = this._accountService.finalyzeAccountId(this.accountName, this.domainId, this.projectId, true);
        return accountId == null ? CallContext.current().getCallingAccount().getId() : accountId;
    }

    public String getEventType() {
        return "VM.CREATE";
    }

    public String getCreateEventType() {
        return "VM.CREATE";
    }

    public String getCreateEventDescription() {
        return "creating Vm";
    }

    public String getEventDescription() {
        return "starting Vm. Vm Id: " + this.getEntityId();
    }

    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.VirtualMachine;
    }

    public void execute() {
        //SetGlobalSettingsValues();

        UserVm result;
        if (this.getStartVm()) {
            try {
                CallContext.current().setEventDetails("Vm Id: " + this.getEntityId());
                result = this._userVmServiceModified.startVirtualMachine(this);
            } catch (ResourceUnavailableException var4) {
                s_logger.warn("Exception: ", var4);
                throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, var4.getMessage());
            } catch (ConcurrentOperationException var5) {
                s_logger.warn("Exception: ", var5);
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, var5.getMessage());
            } catch (InsufficientCapacityException var6) {
                StringBuilder message = new StringBuilder(var6.getMessage());
                if (var6 instanceof InsufficientServerCapacityException && ((InsufficientServerCapacityException)var6).isAffinityApplied()) {
                    message.append(", Please check the affinity groups provided, there may not be sufficient capacity to follow them");
                }

                s_logger.info(var6);
                s_logger.info(message.toString(), var6);
                throw new ServerApiException(ApiErrorCode.INSUFFICIENT_CAPACITY_ERROR, message.toString());
            }
        } else {
            result = this._userVmService.getUserVm(this.getEntityId());
        }

        if (result != null) {
            UserVmResponse response = (UserVmResponse)this._responseGenerator.createUserVmResponse(ResponseView.Restricted, "virtualmachine", new UserVm[]{result}).get(0);
            response.setResponseName(this.getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to deploy vm uuid:" + this.getEntityUuid());
        }
    }

    private void verifyDetails() {
        Map<String, String> map = this.getDetails();
        if (map != null) {
            String minIops = (String)map.get("minIops");
            String maxIops = (String)map.get("maxIops");
            this.verifyMinAndMaxIops(minIops, maxIops);
            minIops = (String)map.get("minIopsDo");
            maxIops = (String)map.get("maxIopsDo");
            this.verifyMinAndMaxIops(minIops, maxIops);
        }

    }

    private void verifyMinAndMaxIops(String minIops, String maxIops) {
        if (minIops != null && maxIops == null || minIops == null && maxIops != null) {
            throw new InvalidParameterValueException("Either 'Min IOPS' and 'Max IOPS' must both be specified or neither be specified.");
        } else {
            long lMinIops;
            try {
                if (minIops != null) {
                    lMinIops = Long.parseLong(minIops);
                } else {
                    lMinIops = 0L;
                }
            } catch (NumberFormatException var9) {
                throw new InvalidParameterValueException("'Min IOPS' must be a whole number.");
            }

            long lMaxIops;
            try {
                if (maxIops != null) {
                    lMaxIops = Long.parseLong(maxIops);
                } else {
                    lMaxIops = 0L;
                }
            } catch (NumberFormatException var8) {
                throw new InvalidParameterValueException("'Max IOPS' must be a whole number.");
            }

            if (lMinIops > lMaxIops) {
                throw new InvalidParameterValueException("'Min IOPS' must be less than or equal to 'Max IOPS'.");
            }
        }
    }

    public void create() throws ResourceAllocationException {
        SetGlobalSettingsValues();

        try {
            Account owner = this._accountService.getActiveAccountById(this.getEntityOwnerId());
            this.verifyDetails();
            DataCenter zone = (DataCenter)this._entityMgr.findById(DataCenter.class, this.zoneId);
            if (zone == null) {
                throw new InvalidParameterValueException("Unable to find zone by id=" + this.zoneId);
            } else {
                ServiceOffering serviceOffering = (ServiceOffering)this._entityMgr.findById(ServiceOffering.class, this.serviceOfferingId);
                if (serviceOffering == null) {
                    throw new InvalidParameterValueException("Unable to find service offering: " + this.serviceOfferingId);
                } else {
                    VirtualMachineTemplate template = (VirtualMachineTemplate)this._entityMgr.findById(VirtualMachineTemplate.class, this.templateId);
                    if (template == null) {
                        throw new InvalidParameterValueException("Unable to find the template " + this.templateId);
                    } else {
                        DiskOffering diskOffering = null;
                        if (this.diskOfferingId != null) {
                            diskOffering = (DiskOffering)this._entityMgr.findById(DiskOffering.class, this.diskOfferingId);
                            if (diskOffering == null) {
                                throw new InvalidParameterValueException("Unable to find disk offering " + this.diskOfferingId);
                            }
                        }

                        if (!zone.isLocalStorageEnabled()) {
                            if (serviceOffering.getUseLocalStorage()) {
                                throw new InvalidParameterValueException("Zone is not configured to use local storage but service offering " + serviceOffering.getName() + " uses it");
                            }

                            if (diskOffering != null && diskOffering.getUseLocalStorage()) {
                                throw new InvalidParameterValueException("Zone is not configured to use local storage but disk offering " + diskOffering.getName() + " uses it");
                            }
                        }

                        UserVm vm = null;
                        IpAddresses addrs = new IpAddresses(this.ipAddress, this.getIp6Address());
                        if (zone.getNetworkType() == NetworkType.Basic) {
                            if (this.getNetworkIds() != null) {
                                throw new InvalidParameterValueException("Can't specify network Ids in Basic zone");
                            }

                            vm = this._userVmService.createBasicSecurityGroupVirtualMachine(zone, serviceOffering, template, this.getSecurityGroupIdList(), owner, this.name, this.displayName, this.diskOfferingId, this.size, this.group, this.getHypervisor(), this.getHttpMethod(), this.userData, this.sshKeyPairName, this.getIpToNetworkMap(), addrs, this.displayVm, this.keyboard, this.getAffinityGroupIdList(), this.getDetails(), this.getCustomId());
                        } else if (zone.isSecurityGroupEnabled()) {
                            vm = this._userVmService.createAdvancedSecurityGroupVirtualMachine(zone, serviceOffering, template, this.getNetworkIds(), this.getSecurityGroupIdList(), owner, this.name, this.displayName, this.diskOfferingId, this.size, this.group, this.getHypervisor(), this.getHttpMethod(), this.userData, this.sshKeyPairName, this.getIpToNetworkMap(), addrs, this.displayVm, this.keyboard, this.getAffinityGroupIdList(), this.getDetails(), this.getCustomId());
                        } else {
                            if (this.getSecurityGroupIdList() != null && !this.getSecurityGroupIdList().isEmpty()) {
                                throw new InvalidParameterValueException("Can't create vm with security groups; security group feature is not enabled per zone");
                            }

                            vm = this._userVmService.createAdvancedVirtualMachine(zone, serviceOffering, template, this.getNetworkIds(), owner, this.name, this.displayName, this.diskOfferingId, this.size, this.group, this.getHypervisor(), this.getHttpMethod(), this.userData, this.sshKeyPairName, this.getIpToNetworkMap(), addrs, this.displayVm, this.keyboard, this.getAffinityGroupIdList(), this.getDetails(), this.getCustomId());
                        }

                        if (vm != null) {
                            this.setEntityId(vm.getId());
                            this.setEntityUuid(vm.getUuid());
                        } else {
                            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to deploy vm");
                        }
                    }
                }
            }
        } catch (InsufficientCapacityException var8) {
            s_logger.info(var8);
            s_logger.trace(var8.getMessage(), var8);
            throw new ServerApiException(ApiErrorCode.INSUFFICIENT_CAPACITY_ERROR, var8.getMessage());
        } catch (ResourceUnavailableException var9) {
            s_logger.warn("Exception: ", var9);
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, var9.getMessage());
        } catch (ConcurrentOperationException var10) {
            s_logger.warn("Exception: ", var10);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, var10.getMessage());
        } catch (ResourceAllocationException var11) {
            s_logger.warn("Exception: ", var11);
            throw new ServerApiException(ApiErrorCode.RESOURCE_ALLOCATION_ERROR, var11.getMessage());
        }
    }

    //////////////////////////////////////////////////////////////////////////////////

}
