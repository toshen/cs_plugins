package org.apache.cloudstack.api.command;

import com.cloud.user.AccountService;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.GetHiddenVmConfigResponse;
import org.apache.cloudstack.exttools.ApiExtToolsService;
import org.apache.log4j.Logger;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.acl.RoleType;

import javax.inject.Inject;

@APICommand(name = "getHiddenVmConfig",
        description="Get hidden vm cfg",
        responseObject = GetHiddenVmConfigResponse.class,
        includeInApiDoc=true,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class GetHiddenVmConfigCmd extends BaseCmd {
    @Parameter(
            name = ApiConstants.ACCOUNT_ID,
            type = CommandType.STRING,
            description = "CloudStack Account UUID",
            required = true)
    private String accountUuid;

    public static final Logger s_logger = Logger.getLogger(GetHiddenVmConfigCmd.class.getName());
    private static final String s_name = "gethiddenvmconfigresponse";

    @Inject
    private ApiExtToolsService _apiExtToolsService;
    @Inject
    private AccountService _aAccountService;

    @Override
    public void execute() {
        GetHiddenVmConfigResponse response = _apiExtToolsService.getVmConfigCmd(accountUuid);
        response.setResponseName(getCommandName());
        response.setObjectName("hiddenvmconfig");
        this.setResponseObject(response);
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return 0;
    }
}