package io.jenkins.plugins.remote.result.trigger.model;

import java.util.Map;

/**
 * Info
 *
 * @author heweisc@dingtalk.com
 */
public class JobResultInfo {
    private String remoteServer;
    private String remoteJob;
    private String remoteJobUrl;
    private String uid;
    private Integer triggeredNumber;
    private Integer checkedNumber;
    private Map<String, Object> buildResult;
    private Map<String, Object> remoteResult;

    public String getRemoteServer() {
        return remoteServer;
    }

    public void setRemoteServer(String remoteServer) {
        this.remoteServer = remoteServer;
    }

    public String getRemoteJob() {
        return remoteJob;
    }

    public void setRemoteJob(String remoteJob) {
        this.remoteJob = remoteJob;
    }

    public String getRemoteJobUrl() {
        return remoteJobUrl;
    }

    public String getBuildUrl() {
        if (buildResult != null && buildResult.get("url") != null) {
            return buildResult.get("url").toString();
        }
        return null;
    }

    public void setRemoteJobUrl(String remoteJobUrl) {
        this.remoteJobUrl = remoteJobUrl;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public Integer getCheckedNumber() {
        return checkedNumber;
    }

    public Integer getTriggeredNumber() {
        return triggeredNumber;
    }

    public void setTriggeredNumber(Integer triggeredNumber) {
        this.triggeredNumber = triggeredNumber;
    }

    public void setCheckedNumber(Integer checkedNumber) {
        this.checkedNumber = checkedNumber;
    }

    public Map<String, Object> getBuildResult() {
        return buildResult;
    }

    public void setBuildResult(Map<String, Object> buildResult) {
        this.buildResult = buildResult;
    }

    public Map<String, Object> getRemoteResult() {
        return remoteResult;
    }

    public void setRemoteResult(Map<String, Object> remoteResult) {
        this.remoteResult = remoteResult;
    }
}
