package org.apache.cloudstack.api.command;

import com.cloud.dc.DataCenter;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.uservm.UserVm;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.affinity.AffinityGroupResponse;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.vm.DeployVMCmd;
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
import org.apache.cloudstack.exttools.ApiExtToolsServiceImpl;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

@APICommand(
        name = "deployHiddenVm",
        description = "Creates and automatically starts a virtual machine based on a service offering, disk offering, and template.",
        responseObject = UserVmResponse.class,
        responseView = ResponseView.Restricted,
        entityType = {VirtualMachine.class},
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = true
)
public class DeployHiddenVMCmd extends DeployVMCmd {

    @Inject
    private ApiExtToolsServiceImpl _apiExtToolsService;

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
            required = true,
            description = "the ID of the service offering for the virtual machine"
    )
    private Long serviceOfferingId;
    @ACL
    @Parameter(
            name = "templateid",
            type = CommandType.UUID,
            entityType = {TemplateResponse.class},
            required = true,
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

    @Override
    public void execute() {
        SetGlobalSettingsValues();

        UserVm result;
        if (this.getStartVm()) {
            try {
                CallContext.current().setEventDetails("Vm Id: " + this.getEntityId());
                result = this._userVmService.startVirtualMachine(this);
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

    private void SetGlobalSettingsValues() {
        ConfigKey<?>[] configKeys = _apiExtToolsService.getConfigKeys();
        String configKeyZoneUuid = (String)configKeys[0].value();

        //TODO: replace zone to template
        DataCenter zone = _entityMgr.findByUuid(DataCenter.class, configKeyZoneUuid);
        s_logger.debug("!!! zoneId=" + this.zoneId + ", config zoneId=" + zone.getId() + ", config zoneUuid=" + configKeyZoneUuid);

//        s_logger.debug("!!!!! zoneId = " + this.zoneId);
//        if (this.zoneId == null) {
//            DataCenter zone = _entityMgr.findByUuid(DataCenter.class, configKeyZoneUuid);
//            if (zone == null) {
//                s_logger.debug("!!!!! Unable to find zone by UUID(config)=" + configKeyZoneUuid);
//                throw new InvalidParameterValueException("Unable to find zone by UUID=" + configKeyZoneUuid);
//            }
//            this.zoneId = zone.getId();
//            s_logger.debug("!!!!! config zoneId = " + zone.getId());
//            s_logger.debug("!!!!! config zoneUuid = " + zone.getUuid());
//        }
    }


}
