package io.jenkins.plugins.remote.result.trigger;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.remote.result.trigger.model.ResultCheck;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Remote Job Configuration
 *
 * @author HW
 */
public class RemoteJobInfo implements Describable<RemoteJobInfo>, Serializable {
    private static final long serialVersionUID = -7232627326475916056L;
    /**
     * All job build results
     */
    private static final String[] ALL_BUILD_RESULT = new String[]{"SUCCESS", "UNSTABLE", "FAILURE", "NOT_BUILT", "ABORTED"};

    private String id;
    private String remoteServer;
    private String remoteJobName;
    private String uid;
    private List<String> triggerResults = new ArrayList<>();
    private List<ResultCheck> resultChecks = new ArrayList<>();

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
        return Jenkins.get().getDescriptor(getClass());
    }

    public String getId() {
        return id;
    }

    @DataBoundSetter
    public void setId(String id) {
        this.id = id;
    }

    public String getRemoteServer() {
        return remoteServer;
    }

    @DataBoundSetter
    public void setRemoteServer(String remoteServer) {
        this.remoteServer = remoteServer;
    }

    public String getRemoteJobName() {
        return remoteJobName;
    }

    @DataBoundSetter
    public void setRemoteJobName(String remoteJobName) {
        this.remoteJobName = remoteJobName;
    }

    public String getUid() {
        return uid;
    }

    @DataBoundSetter
    public void setUid(String uid) {
        this.uid = uid;
    }

    public List<String> getTriggerResults() {
        return triggerResults;
    }

    public List<ResultCheck> getResultChecks() {
        return resultChecks;
    }

    @DataBoundSetter
    public void setResultChecks(List<ResultCheck> resultChecks) {
        this.resultChecks = resultChecks;
    }

    /**
     * Check if selected
     *
     * @param result need check result
     * @return is selected
     */
    public Boolean isTriggerResultChecked(String result) {
        return triggerResults.contains(result);
    }

    /**
     * spec set triggerResults with checked list
     *
     * @param triggerResults trigger result list
     */
    @DataBoundSetter
    public void setTriggerResults(List<Boolean> triggerResults) {
        this.triggerResults = new ArrayList<>();
        for (int i = 0; i < ALL_BUILD_RESULT.length; i++) {
            if (triggerResults.get(i)) {
                this.triggerResults.add(ALL_BUILD_RESULT[i]);
            }
        }
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<RemoteJobInfo> {
        /**
         * get build result types
         *
         * @return all build results
         */
        public static String[] getBuildResults() {
            return ALL_BUILD_RESULT;
        }

        /**
         * Validates the remoteServer
         *
         * @param remoteServer Remote Jenkins Server to be validated
         * @return FormValidation object
         */
        @POST
        @Restricted(NoExternalUse.class)
        public FormValidation doCheckRemoteServer(@QueryParameter String remoteServer) {
            if (StringUtils.isEmpty(remoteServer)) {
                return FormValidation.error("Please select a remote Jenkins Server");
            }
            return FormValidation.ok();
        }

        /**
         * Validates the jobName
         *
         * @param remoteJobName Remote JobName
         * @return FormValidation object
         */
        @POST
        @Restricted(NoExternalUse.class)
        public FormValidation doCheckRemoteJobName(@QueryParameter String remoteJobName) {
            if (StringUtils.isEmpty(remoteJobName)) {
                return FormValidation.error("Please enter a job name");
            }
            return FormValidation.ok();
        }

        /**
         * fill remoteServer select
         *
         * @return fill list model
         */
        @POST
        @Restricted(NoExternalUse.class)
        public ListBoxModel doFillRemoteServerItems(@QueryParameter String remoteJenkinsServer) {
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
