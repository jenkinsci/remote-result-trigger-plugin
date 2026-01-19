package io.jenkins.plugins.remote.result.trigger.model;

import lombok.Data;

/**
 * @author heweisc@dingtalk.com
 */
@Data
public class JobResultDisplayInfo {
    private String remoteJobUrl;
    private String result;
    private String resultJson;
    private String buildUrl;
}
