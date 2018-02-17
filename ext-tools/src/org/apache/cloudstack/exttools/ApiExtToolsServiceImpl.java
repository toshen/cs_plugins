package org.apache.cloudstack.exttools;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.Account;
import com.cloud.user.dao.AccountDao;
import org.apache.cloudstack.acl.APIChecker;
import org.apache.cloudstack.api.command.GetTimeOfDayCmd;
import org.apache.cloudstack.api.command.GetVmConfigCmd;
import org.apache.cloudstack.api.response.GetVmConfigResponse;
import org.springframework.stereotype.Component;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.User;
import com.cloud.utils.component.AdapterBase;

@Component
public class ApiExtToolsServiceImpl extends AdapterBase implements APIChecker, ApiExtToolsService {

    @Inject private AccountDao _accountDao;
    @Inject private VMTemplateDao _vMTemplateDao;
    @Inject private ServiceOfferingDao _serviceOfferingDao;
    @Inject private NetworkDao _networkDao;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        return true;
    }

    @Override
    public GetVmConfigResponse getVmConfigCmd(String accountUuid) {
        Account account = _accountDao.findByUuid(accountUuid);
        long accountId = account.getId();

        List<VMTemplateVO> allSystemVMTemplates = _vMTemplateDao.listAllSystemVMTemplates();
        //List<ServiceOfferingVO> systemOfferings = serviceOfferingDao.findSystemOffering(null, null, null);//Long domainId, Boolean isSystem, String vmType
        List<ServiceOfferingVO> systemOfferings = _serviceOfferingDao.findPublicServiceOfferings();
        List<NetworkVO> networks = _networkDao.listByOwner(accountId);

        return new GetVmConfigResponse(allSystemVMTemplates, systemOfferings, networks);
    }

    @Override
    public boolean checkAccess(User user, String apiCommandName) throws PermissionDeniedException {
        return true;
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();

        cmdList.add(GetTimeOfDayCmd.class);
        cmdList.add(GetVmConfigCmd.class);

        return cmdList;
    }
}
