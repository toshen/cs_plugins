package org.apache.cloudstack.exttools;


import com.cloud.exception.PermissionDeniedException;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.component.AdapterBase;
import org.apache.cloudstack.acl.APIChecker;
import org.apache.cloudstack.api.command.DeployHiddenVMCmd;
import org.apache.cloudstack.api.command.GetHiddenVmConfigCmd;
import org.apache.cloudstack.api.response.GetHiddenVmConfigResponse;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ApiExtToolsServiceImpl extends AdapterBase implements APIChecker, ApiExtToolsService {

    @Inject
    private AccountDao _accountDao;
    @Inject
    private VMTemplateDao _vMTemplateDao;
    @Inject
    private ServiceOfferingDao _serviceOfferingDao;
    @Inject
    private NetworkDao _networkDao;

    private static final ConfigKey<String> zoneId =
            new ConfigKey<String>(
                    "Advanced",
                    String.class,
                    "extTools.zone.id",
                    "0",
                    "Default zone id for ext-tools methods",
                    true, ConfigKey.Scope.Global);

    @Override
    public String getConfigComponentName(){
        return ApiExtToolsServiceImpl.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys(){
        return new ConfigKey<?>[] { zoneId };
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        return true;
    }

    @Override
    public GetHiddenVmConfigResponse getVmConfigCmd(String accountUuid) {
        Account account = _accountDao.findByUuid(accountUuid);
        long accountId = account.getId();

        List<Map> systemTemplates = new ArrayList<>();
        List<Map> serviceOfferings = new ArrayList<>();
        List<Map> userNetworks = new ArrayList<>();

        List<VMTemplateVO> allSystemVMTemplates = _vMTemplateDao.listAll();
        for (VMTemplateVO allSystemVMTemplate : allSystemVMTemplates) {
            Map<String, String> node = new HashMap<>();
            node.put("name", allSystemVMTemplate.getName());
            node.put("uuid", allSystemVMTemplate.getUuid());
            systemTemplates.add(node);
        }

        List<ServiceOfferingVO> systemOfferings = _serviceOfferingDao.findPublicServiceOfferings();
        for (ServiceOfferingVO systemOffering : systemOfferings) {
            Map<String, String> node = new HashMap<>();
            node.put("name", systemOffering.getName());
            node.put("uuid", systemOffering.getUuid());
            serviceOfferings.add(node);
        }

        List<NetworkVO> networks = _networkDao.listByOwner(accountId);
        for (NetworkVO network : networks) {
            Map<String, String> node = new HashMap<>();
            node.put("name", network.getName());
            node.put("uuid", network.getUuid());
            userNetworks.add(node);
        }

        return new GetHiddenVmConfigResponse(systemTemplates, serviceOfferings, userNetworks);
    }

    @Override
    public boolean checkAccess(User user, String apiCommandName) throws PermissionDeniedException {
        return true;
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();

        cmdList.add(GetHiddenVmConfigCmd.class);
        cmdList.add(DeployHiddenVMCmd.class);

        return cmdList;
    }

//    private void SetGlobalSettingsValues() {
//        ConfigKey<?>[] configKeys = _apiExtToolsService.getConfigKeys();
//        String configKeyZoneUuid = (String)configKeys[0].value();
//
//        //TODO: replace zone to template
//        DataCenter zone = _entityMgr.findByUuid(DataCenter.class, configKeyZoneUuid);
//        s_logger.debug("!!! zoneId=" + this.zoneId + ", config zoneId=" + zone.getId() + ", config zoneUuid=" + configKeyZoneUuid);
//
////        s_logger.debug("!!!!! zoneId = " + this.zoneId);
////        if (this.zoneId == null) {
////            DataCenter zone = _entityMgr.findByUuid(DataCenter.class, configKeyZoneUuid);
////            if (zone == null) {
////                s_logger.debug("!!!!! Unable to find zone by UUID(config)=" + configKeyZoneUuid);
////                throw new InvalidParameterValueException("Unable to find zone by UUID=" + configKeyZoneUuid);
////            }
////            this.zoneId = zone.getId();
////            s_logger.debug("!!!!! config zoneId = " + zone.getId());
////            s_logger.debug("!!!!! config zoneUuid = " + zone.getUuid());
////        }
//    }


}
