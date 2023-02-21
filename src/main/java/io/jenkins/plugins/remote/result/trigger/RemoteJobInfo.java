package io.jenkins.plugins.remote.result.trigger;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.remote.result.trigger.utils.RemoteJenkinsServerUtils;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

import java.io.Serializable;

/**
 * Remote Job Configuration
 *
 * @author HW
 * @date 2023/02/20 14:31
 */
public class RemoteJobInfo implements Describable<RemoteJobInfo>, Serializable {
    private static final long serialVersionUID = -7232627326475916056L;

    private String id;
    private String remoteJenkinsServer;
    private String remoteJobName;
    private String remoteJobReplacement;

    @DataBoundConstructor
    public RemoteJobInfo() {
    }

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
    public Descriptor<RemoteJobInfo> getDescriptor() {
        return new DescriptorImpl();
    }

    public String getId() {
        return id;
    }

    @DataBoundSetter
    public void setId(String id) {
        this.id = id;
    }

    public String getRemoteJenkinsServer() {
        return remoteJenkinsServer;
    }

    @DataBoundSetter
    public void setRemoteJenkinsServer(String remoteJenkinsServer) {
        this.remoteJenkinsServer = remoteJenkinsServer;
    }

    public String getRemoteJobName() {
        return remoteJobName;
    }

    @DataBoundSetter
    public void setRemoteJobName(String remoteJobName) {
        this.remoteJobName = remoteJobName;
    }

    public String getRemoteJobReplacement() {
        return remoteJobReplacement;
    }

    @DataBoundSetter
    public void setRemoteJobReplacement(String remoteJobReplacement) {
        this.remoteJobReplacement = remoteJobReplacement;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<RemoteJobInfo> {

        /**
         * Validates the remoteJenkinsServer
         *
         * @param remoteJenkinsServer Remote Jenkins Server to be validated
         * @return FormValidation object
         */
        @POST
        @Restricted(NoExternalUse.class)
        public FormValidation doCheckRemoteJenkinsServer(@QueryParameter String remoteJenkinsServer) {
            if (StringUtils.isEmpty(remoteJenkinsServer)) {
                return FormValidation.error("Please select a remote Jenkins Server");
            }
            return FormValidation.ok();
        }

        /**
         * fill remoteJenkinsServer select
         *
         * @return
         */
        @POST
        @Restricted(NoExternalUse.class)
        public ListBoxModel doFillRemoteJenkinsServerItems(@QueryParameter String remoteJenkinsServer) {
            ListBoxModel model = new ListBoxModel();

            model.add("");

            RemoteJenkinsServer[] servers = RemoteJenkinsServerUtils.getRemoteServers();
            for (RemoteJenkinsServer server : servers) {
                String key = StringUtils.isNotEmpty(server.getDisplayName()) ? server.getDisplayName() : server.getUrl();
                if (server.getId() != null && key != null) {
                    if (Jenkins.get().hasPermission(Jenkins.ADMINISTER) || StringUtils.equals(server.getId(), remoteJenkinsServer)) {
                        model.add(key, server.getId());
                    }
                }
            }

            return model;
        }
    }
}
