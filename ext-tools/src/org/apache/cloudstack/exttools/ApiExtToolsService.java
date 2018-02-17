package org.apache.cloudstack.exttools;

import com.cloud.utils.component.PluggableService;
import org.apache.cloudstack.api.response.GetVmConfigResponse;

public interface ApiExtToolsService extends PluggableService {
    public GetVmConfigResponse getVmConfigCmd(String accountUuid);
}
