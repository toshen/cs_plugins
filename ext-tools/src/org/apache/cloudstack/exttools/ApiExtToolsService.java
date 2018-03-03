package org.apache.cloudstack.exttools;

import com.cloud.utils.component.PluggableService;
import org.apache.cloudstack.api.response.GetHiddenVmConfigResponse;
import org.apache.cloudstack.framework.config.Configurable;

public interface ApiExtToolsService extends PluggableService, Configurable {
    public GetHiddenVmConfigResponse getVmConfigCmd(String accountUuid);
}
