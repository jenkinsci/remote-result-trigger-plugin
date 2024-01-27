package io.jenkins.plugins.remote.result.trigger.auth2;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Item;
import hudson.util.Secret;
import io.jenkins.plugins.remote.result.trigger.exceptions.CredentialsNotFoundException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.nio.charset.StandardCharsets;

/**
 * @author HW
 */
public class TokenAuth extends Auth2 {

    private static final long serialVersionUID = 7912089565969112023L;

    @Extension
    public static final Auth2Descriptor DESCRIPTOR = new TokenAuthDescriptor();

    private String userName;
    private Secret apiToken;

    @DataBoundConstructor
    public TokenAuth() {
        this.userName = null;
        this.apiToken = null;
    }

    @DataBoundSetter
    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return this.userName;
    }

    @DataBoundSetter
    public void setApiToken(Secret apiToken) {
        this.apiToken = apiToken;
    }

    public Secret getApiToken() {
        return this.apiToken;
    }

    /**
     * Get JenkinsClient Credentials Or ApiToken
     */
    @Override
    public String getCredentials(Item item) throws CredentialsNotFoundException {
        if (StringUtils.isNotEmpty(this.userName) && this.apiToken != null) {
            String username = getUserName();
            String token = getApiToken().getPlainText();
            return "Basic " + Base64.encodeBase64String((username + ":" + token).getBytes(StandardCharsets.UTF_8));
        }
        return null;
    }

    @Override
    public String toString() {
        return "'" + getDescriptor().getDisplayName() + "' as user '" + getUserName() + "'";
    }

    @Override
    public String toString(Item item) {
        return toString();
    }

    @Override
    public Auth2Descriptor getDescriptor() {
        return DESCRIPTOR;
    }

    @Symbol("TokenAuth")
    public static class TokenAuthDescriptor extends Auth2Descriptor {
        @NonNull
        @Override
        public String getDisplayName() {
            return "Token Authentication";
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((apiToken == null) ? 0 : apiToken.hashCode());
        result = prime * result + ((userName == null) ? 0 : userName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!this.getClass().isInstance(obj)) {
            return false;
        }
        TokenAuth other = (TokenAuth) obj;
        if (apiToken == null) {
            if (other.apiToken != null) {
                return false;
            }
        } else if (!apiToken.equals(other.apiToken)) {
            return false;
        }
        if (userName == null) {
            return other.userName == null;
        } else if (!userName.equals(other.userName)) {
            return false;
        }
        return true;
    }

}
