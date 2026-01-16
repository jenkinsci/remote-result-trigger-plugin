package io.jenkins.plugins.remote.result.trigger;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import io.jenkins.plugins.remote.result.trigger.auth2.Auth2;
import io.jenkins.plugins.remote.result.trigger.auth2.NoneAuth;
import jenkins.model.Jenkins;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

import java.io.Serial;
import java.io.Serializable;
import java.net.URL;
import java.util.List;

/**
 * Holds everything regarding the remote server we wish to connect to, including validations and what not.
 *
 * @author Maurice W.
 */
@Getter
public class RemoteJenkinsServer implements Describable<RemoteJenkinsServer>, Serializable {

    @Serial
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
        return Jenkins.get().getDescriptor(getClass());
    }

    private String id;
    private String displayName;
    private boolean trustAllCertificates;
    private Auth2 auth2;
    private String url;

    @DataBoundConstructor
    public RemoteJenkinsServer() {
    }

    @DataBoundSetter
    public void setId(String id) {
        this.id = id;
    }

    @DataBoundSetter
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @DataBoundSetter
    public void setTrustAllCertificates(boolean trustAllCertificates) {
        this.trustAllCertificates = trustAllCertificates;
    }

    @DataBoundSetter
    public void setAuth2(Auth2 auth2) {
        this.auth2 = auth2;
    }

    @DataBoundSetter
    public void setUrl(String url) {
        this.url = url;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<RemoteJenkinsServer> {

        /**
         * Validates the given address to see that it's well-formed, and is reachable.
         *
         * @param url Remote Jenkins Server address to be validated
         * @return FormValidation object
         */
        @POST
        @Restricted(NoExternalUse.class)
        public FormValidation doCheckUrl(@QueryParameter String url) {
            if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                return FormValidation.ok();
            }
            // no empty addresses allowed
            if (StringUtils.isEmpty(url)) {
                return FormValidation.error("The remote address can not be empty.");
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
