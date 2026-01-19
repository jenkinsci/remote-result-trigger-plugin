package io.jenkins.plugins.remote.result.trigger.model;

import lombok.Data;

import java.util.Map;

/**
 * Info
 *
 * @author heweisc@dingtalk.com
 */
@Data
public class JobResultInfo {
    private String remoteServer;
    private String remoteJob;
    private String remoteJobUrl;
    private String uid;
    private Integer triggeredNumber;
    private Integer checkedNumber;
    private Map<String, Object> buildResult;
    private Map<String, Object> remoteResult;

    public String getBuildUrl() {
        if (buildResult != null && buildResult.get("url") != null) {
            return buildResult.get("url").toString();
        }
        return null;
    }
}
