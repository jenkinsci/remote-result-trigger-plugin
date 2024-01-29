package io.jenkins.plugins.remote.result.trigger.model;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author heweisc@dingtalk.com
 */
public class ResultCheck {
    private final String key;
    private final String expectedValue;

    @DataBoundConstructor
    public ResultCheck(String key, String expectedValue) {
        this.key = key;
        this.expectedValue = expectedValue;
    }

    public String getKey() {
        return key;
    }

    public String getExpectedValue() {
        return expectedValue;
    }
}
