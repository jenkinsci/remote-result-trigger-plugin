package io.jenkins.plugins.remote.result.trigger;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.remote.result.trigger.model.ResultCheck;
import io.jenkins.plugins.remote.result.trigger.utils.RemoteJenkinsServerUtils;
import jenkins.model.Jenkins;
import lombok.Getter;
import lombok.Setter;
import net.sf.json.util.JSONUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Remote Job Configuration
 *
 * @author HW
 */
public class RemoteJobInfo implements Describable<RemoteJobInfo>, Serializable {
    @Serial
    private static final long serialVersionUID = -7232627326475916056L;
    /**
     * All job build results
     */
    private static final String[] ALL_BUILD_RESULT = new String[]{"SUCCESS", "UNSTABLE", "FAILURE", "NOT_BUILT", "ABORTED"};

    @Setter
    @Getter
    private String id;
    @Getter
    private String remoteServer;
    @Deprecated
    private String remoteJobName;
    private String remoteJobUrl;
    @Getter
    private String uid;
    @Getter
    private List<String> triggerResults = new ArrayList<>();
    @Getter
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

    @DataBoundSetter
    public void setRemoteServer(String remoteServer) {
        this.remoteServer = remoteServer;
    }

    @Deprecated
    public String getRemoteJobName() {
        return remoteJobName;
    }

    public String getRemoteJobUrl() {
        // 兼容老版本数据
        if (remoteJobUrl == null && remoteJobName != null && remoteServer != null) {
            RemoteJenkinsServer remoteServer = RemoteJenkinsServerUtils
                    .getRemoteJenkinsServer(getRemoteServer());
            return remoteServer.getUrl() +
                    (remoteServer.getUrl().endsWith("/") ? "" : "/") +
                    remoteJobName;
        }
        return remoteJobUrl;
    }

    @DataBoundSetter
    public void setRemoteJobUrl(String remoteJobUrl) {
        this.remoteJobUrl = remoteJobUrl;
    }

    @DataBoundSetter
    public void setRemoteJobName(String remoteJobName) {
        this.remoteJobName = remoteJobName;
    }

    @DataBoundSetter
    public void setUid(String uid) {
        this.uid = StringUtils.isEmpty(uid) ? RandomStringUtils.randomAlphabetic(32) : uid;
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

    public void updateId() {
        this.setId(DigestUtils.sha256Hex(
                remoteServer + getRemoteJobUrl() + uid
                        + JSONUtils.valueToString(triggerResults)
                        + JSONUtils.valueToString(resultChecks)
        ));
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
         * @param remoteJobUrl Remote Job Url
         * @return FormValidation object
         */
        @POST
        @Restricted(NoExternalUse.class)
        public FormValidation doCheckRemoteJobUrl(@QueryParameter String remoteJobUrl) {
            if (StringUtils.isEmpty(remoteJobUrl)) {
                return FormValidation.error("Please enter a remote job url");
            }
            return FormValidation.ok();
        }

        /**
         * Validates the uid
         *
         * @param uid Unique Identifier
         * @return FormValidation object
         */
        @POST
        @Restricted(NoExternalUse.class)
        public FormValidation doCheckUid(@QueryParameter String uid) {
            if (StringUtils.isNotEmpty(uid)) {
                if (!uid.matches("[a-zA-Z0-9_-]*")) {
                    return FormValidation.error("Only support [a-zA-Z0-9_-] characters");
                }
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
                    if (Jenkins.get().hasPermission(Jenkins.READ) || StringUtils.equals(server.getId(), remoteJenkinsServer)) {
                        model.add(key, server.getId());
                    }
                }
            }

            return model;
        }
    }
}
