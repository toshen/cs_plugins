package org.apache.cloudstack.api.command;

import org.apache.cloudstack.api.response.GetTimeOfDayResponse;
import org.apache.log4j.Logger;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.acl.RoleType;

@APICommand(name = "getTimeOfDay", description="Get Cloudstack's time of day", responseObject = GetTimeOfDayResponse.class,
        includeInApiDoc=true, authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class GetTimeOfDayCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(GetTimeOfDayCmd.class.getName());
    private static final String s_name = "gettimeofdayresponse";

    @Override
    public void execute()
    {
        GetTimeOfDayResponse response = new GetTimeOfDayResponse();
        response.setObjectName("timeofday"); // the inner part of the json structure
        response.setResponseName(getCommandName()); // the outer part of the json structure
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