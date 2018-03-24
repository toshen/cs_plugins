package org.apache.cloudstack.api.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class GetHiddenVmConfigResponse extends BaseResponse {
    @SerializedName(ApiConstants.IS_ASYNC) @Param(description="true if api is asynchronous")
    private Boolean isAsync;
    @SerializedName("systemTemplates") @Param(description = "templates desc")
    private List<Map> systemTemplates;
    @SerializedName("serviceOfferings") @Param(description = "offerings desc")
    private List<Map> serviceOfferings;
    @SerializedName("userNetworks") @Param(description = "networks desc")
    private List<Map> userNetworks;

    public GetHiddenVmConfigResponse(List<Map> systemTemplates, List<Map> serviceOfferings, List<Map> userNetworks) {
        this.isAsync   = false;
        this.systemTemplates = systemTemplates;
        this.serviceOfferings = serviceOfferings;
        this.userNetworks = userNetworks;

    }

    public void setAsync(Boolean isAsync) {
        this.isAsync = isAsync;
    }

    public boolean getAsync() {
        return isAsync;
    }

}
