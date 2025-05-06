package io.jenkins.plugins.remote.result.trigger.model;

import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author heweisc@dingtalk.com
 */
public class ResultCheck implements Serializable {
    @Serial
    private static final long serialVersionUID = 5215261093367652434L;
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
