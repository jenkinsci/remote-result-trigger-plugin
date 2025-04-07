package io.jenkins.plugins.remote.result.trigger.model;

/**
 * @author heweisc@dingtalk.com
 */
public class JobResultDisplayInfo {
    private String remoteJobUrl;
    private String result;
    private String resultJson;
    private String buildUrl;

    public String getRemoteJobUrl() {
        return remoteJobUrl;
    }

    public void setRemoteJobUrl(String remoteJobUrl) {
        this.remoteJobUrl = remoteJobUrl;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getResultJson() {
        return resultJson;
    }

    public void setResultJson(String resultJson) {
        this.resultJson = resultJson;
    }

    public String getBuildUrl() {
        return buildUrl;
    }

    public void setBuildUrl(String buildUrl) {
        this.buildUrl = buildUrl;
    }
}
