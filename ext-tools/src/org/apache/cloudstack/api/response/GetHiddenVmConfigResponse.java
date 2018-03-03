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

//    @SerializedName("testMap") @Param(description = "testing")
//    private List<Map> testMap;//private List<Map<String, String>> testMap;

    public GetHiddenVmConfigResponse(List<Map> systemTemplates, List<Map> serviceOfferings, List<Map> userNetworks) {
        this.isAsync   = false;
        this.systemTemplates = systemTemplates;
        this.serviceOfferings = serviceOfferings;
        this.userNetworks = userNetworks;

//        testMap = new ArrayList<>();
//        Map<String, String> testMapNode0 = new HashMap<String, String>();
//        testMapNode0.put("key1", "value1");
//        testMapNode0.put("key2", "value2");
//        this.testMap.add(testMapNode0);
//        Map<String, String> testMapNode1 = new HashMap<String, String>();
//        testMapNode1.put("key1", "value3");
//        testMapNode1.put("key2", "value4");
//        this.testMap.add(testMapNode1);

    }

    public void setAsync(Boolean isAsync) {
        this.isAsync = isAsync;
    }

//    public void setTemplates(List<VMTemplateVO> templates) {
//        this.templates = templates;
//    }
//
//    public void setSystemOfferings(List<ServiceOfferingVO> systemOfferings) {
//        this.systemOfferings = systemOfferings;
//    }
//
//    public void setNetworks(List<NetworkVO> networks) {
//        this.networks = networks;
//    }

    public boolean getAsync() {
        return isAsync;
    }

}
