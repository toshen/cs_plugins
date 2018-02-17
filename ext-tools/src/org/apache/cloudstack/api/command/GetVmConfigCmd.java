package org.apache.cloudstack.api.command;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.GetVmConfigResponse;
import org.apache.cloudstack.exttools.ApiExtToolsService;
import org.apache.log4j.Logger;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.acl.RoleType;

import javax.inject.Inject;

@APICommand(name = "getVmConfig",
        description="Get vm cfg",
        responseObject = GetVmConfigResponse.class,
        includeInApiDoc=true,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class GetVmConfigCmd extends BaseCmd {
    @Parameter(name = ApiConstants.ACCOUNT_ID, type = CommandType.STRING, description = "CloudStack Account UUID", required = true)
    private String accountUuid;

    public static final Logger s_logger = Logger.getLogger(GetTimeOfDayCmd.class.getName());
    private static final String s_name = "getvmconfigresponse";

    @Inject
    private ApiExtToolsService _apiExtToolsService;

    @Override
    public void execute()
    {
        GetVmConfigResponse response = _apiExtToolsService.getVmConfigCmd(accountUuid);
        response.setResponseName(getCommandName());
        response.setObjectName("vmconfig");
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