package org.apache.cloudstack.api.response;

import com.cloud.network.dao.NetworkVO;
import com.cloud.serializer.Param;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.VMTemplateVO;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import java.util.List;

@SuppressWarnings("unused")
public class GetVmConfigResponse extends BaseResponse {
    @SerializedName(ApiConstants.IS_ASYNC) @Param(description="true if api is asynchronous")
    private Boolean isAsync;
    @SerializedName("templates") @Param(description = "templates desc")
    private List<VMTemplateVO> templates;
    @SerializedName("systemOfferings") @Param(description = "systemOfferings desc")
    private List<ServiceOfferingVO> systemOfferings;
    @SerializedName("networks") @Param(description = "networks desc")
    private List<NetworkVO> networks;

    public GetVmConfigResponse(List<VMTemplateVO> templates, List<ServiceOfferingVO> systemOfferings, List<NetworkVO> networks) {
        this.isAsync   = false;
        this.templates = templates;
        this.systemOfferings = systemOfferings;
        this.networks = networks;
    }

    public void setAsync(Boolean isAsync) {
        this.isAsync = isAsync;
    }

    public void setTemplates(List<VMTemplateVO> templates) {
        this.templates = templates;
    }

    public void setSystemOfferings(List<ServiceOfferingVO> systemOfferings) {
        this.systemOfferings = systemOfferings;
    }

    public void setNetworks(List<NetworkVO> networks) {
        this.networks = networks;
    }

    public boolean getAsync() {
        return isAsync;
    }

}
