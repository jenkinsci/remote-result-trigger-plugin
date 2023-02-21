package io.jenkins.plugins.remote.result.trigger;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import io.jenkins.plugins.remote.result.trigger.auth2.Auth2;
import io.jenkins.plugins.remote.result.trigger.auth2.NoneAuth;
import io.jenkins.plugins.remote.result.trigger.utils.RemoteJenkinsServerUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

import javax.annotation.CheckForNull;
import java.io.Serializable;
import java.net.URL;
import java.util.List;

/**
 * Holds everything regarding the remote server we wish to connect to, including validations and what not.
 *
 * @author Maurice W.
 */
public class RemoteJenkinsServer implements Describable<RemoteJenkinsServer>, Serializable {

    private static final long serialVersionUID = -9211781849078964416L;

    /**
     * Gets the descriptor for this instance.
     *
     * <p>
     * {@link Descriptor} is a singleton for every concrete {@link Describable}
     * implementation, so if {@code a.getClass() == b.getClass()} then by default
     * {@code a.getDescriptor() == b.getDescriptor()} as well.
     * (In rare cases a single implementation class may be used for instances with distinct descriptors.)
     */
    @Override
    public Descriptor<RemoteJenkinsServer> getDescriptor() {
        return new DescriptorImpl();
    }

    @CheckForNull
    private String displayName;
    private boolean trustAllCertificates;

    @CheckForNull
    private Auth2 auth2;
    @CheckForNull
    private String url;

    @DataBoundConstructor
    public RemoteJenkinsServer() {
    }

    @CheckForNull
    public String getDisplayName() {
        return displayName;
    }

    @DataBoundSetter
    public void setDisplayName(@CheckForNull String displayName) {
        this.displayName = displayName;
    }

    public boolean isTrustAllCertificates() {
        return trustAllCertificates;
    }

    @DataBoundSetter
    public void setTrustAllCertificates(boolean trustAllCertificates) {
        this.trustAllCertificates = trustAllCertificates;
    }

    @CheckForNull
    public Auth2 getAuth2() {
        return auth2;
    }

    @DataBoundSetter
    public void setAuth2(@CheckForNull Auth2 auth2) {
        this.auth2 = auth2;
    }

    @CheckForNull
    public String getUrl() {
        return url;
    }

    @DataBoundSetter
    public void setUrl(@CheckForNull String url) {
        this.url = url;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<RemoteJenkinsServer> {
        /**
         * Validates the name to see that it's unique.
         *
         * @param displayName Server display name
         * @return FormValidation object
         */
        @POST
        @Restricted(NoExternalUse.class)
        public FormValidation doDisplayName(@QueryParameter String displayName) {
            if (StringUtils.isNotEmpty(displayName)) {
                List<String> names = RemoteJenkinsServerUtils.getRemoteServerDisplayNames();
                if (names.contains(StringUtils.trim(displayName))) {
                    return FormValidation.error("The remote server display name(" + displayName + ") is used.");
                }
            }
            return FormValidation.ok();
        }

        /**
         * Validates the given address to see that it's well-formed, and is reachable.
         *
         * @param url Remote Jenkins Server address to be validated
         * @return FormValidation object
         */
        @POST
        @Restricted(NoExternalUse.class)
        public FormValidation doCheckUrl(@QueryParameter String url) {
            // no empty addresses allowed
            if (StringUtils.isEmpty(url)) {
                return FormValidation.warning("The remote address can not be empty.");
            }

            // check if we have a valid, well-formed URL
            try {
                URL host = new URL(url);
                host.toURI();
            } catch (Exception e) {
                return FormValidation.error("Malformed address (" + url + "). Remember to indicate the protocol, i.e. http, https, etc.");
            }

            return FormValidation.ok();
        }

        public static List<Auth2.Auth2Descriptor> getAuth2Descriptors() {
            return Auth2.all();
        }

        public static Auth2.Auth2Descriptor getDefaultAuth2Descriptor() {
            return NoneAuth.DESCRIPTOR;
        }
    }
}
