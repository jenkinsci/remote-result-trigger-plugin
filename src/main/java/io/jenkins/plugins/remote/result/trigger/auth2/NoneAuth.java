package io.jenkins.plugins.remote.result.trigger.auth2;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jenkins.plugins.remote.result.trigger.exceptions.CredentialsNotFoundException;
import hudson.Extension;
import hudson.model.Item;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author HW
 */
public class NoneAuth extends Auth2 {

    private static final long serialVersionUID = -3128995428538415113L;

    @Extension
    public static final Auth2Descriptor DESCRIPTOR = new NoneAuthDescriptor();

    public static final NoneAuth INSTANCE = new NoneAuth();


    @DataBoundConstructor
    public NoneAuth() {
    }

    /**
     * Get JenkinsClient Credentials Or ApiToken
     */
    @Override
    public String getCredentials(Item item) throws CredentialsNotFoundException {
        return null;
    }

    @Override
    public String toString() {
        return "'" + getDescriptor().getDisplayName() + "'";
    }

    @Override
    public String toString(Item item) {
        return toString();
    }

    @Override
    public Auth2Descriptor getDescriptor() {
        return DESCRIPTOR;
    }

    @Symbol("NoneAuth")
    public static class NoneAuthDescriptor extends Auth2Descriptor {
        @NonNull
        @Override
        public String getDisplayName() {
            return "No Authentication";
        }
    }

    @Override
    public int hashCode() {
        return "NoneAuth".hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this.getClass().isInstance(obj);
    }

}