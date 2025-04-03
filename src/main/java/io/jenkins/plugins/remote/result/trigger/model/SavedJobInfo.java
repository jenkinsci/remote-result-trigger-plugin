package io.jenkins.plugins.remote.result.trigger.model;

import java.util.Map;

/**
 * Info
 */
public class SavedJobInfo {
    private String remoteServer;
    private String remoteJob;
    @Deprecated
    private String remoteJobName;
    private String remoteJobUrl;
    private String uid;
    @Deprecated
    private Integer triggerNumber;
    private Integer triggeredNumber;
    private Integer checkedNumber;
    private Map<String, Object> result;
    private Map<String, Object> resultJson;

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

    @Deprecated
    public String getRemoteJobName() {
        return remoteJobName;
    }

    @Deprecated
    public void setRemoteJobName(String remoteJobName) {
        this.remoteJobName = remoteJobName;
    }

    public String getRemoteJobUrl() {
        return remoteJobUrl;
    }

    public String getBuildUrl() {
        Object object = result.get("url");
        return object != null ? object.toString() : null;
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

    @Deprecated
    public Integer getTriggerNumber() {
        return triggerNumber;
    }

    public Integer getCheckedNumber() {
        return checkedNumber;
    }

    public void setCheckedNumber(Integer checkedNumber) {
        this.checkedNumber = checkedNumber;
    }

    public Map<String, Object> getResult() {
        return result;
    }

    public void setResult(Map<String, Object> result) {
        this.result = result;
    }

    public Map<String, Object> getResultJson() {
        return resultJson;
    }

    public void setResultJson(Map<String, Object> resultJson) {
        this.resultJson = resultJson;
    }
}
