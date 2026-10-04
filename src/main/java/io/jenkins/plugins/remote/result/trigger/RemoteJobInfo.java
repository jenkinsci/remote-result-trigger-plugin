package io.jenkins.plugins.remote.result.trigger;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.remote.result.trigger.model.ResultCheck;
import io.jenkins.plugins.remote.result.trigger.utils.RemoteJenkinsServerUtils;
import jenkins.model.Jenkins;
import lombok.Getter;
import lombok.Setter;
import net.sf.json.util.JSONUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.AncestorInPath;
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
    /**
     * Whether this remote job trigger checking is enabled.
     * Uses a nullable Boolean so that old configurations (without this field) default to enabled.
     */
    private Boolean enable = Boolean.TRUE;
    @Getter
    private String remoteServer;
    @Getter
    private String remoteJobUrl;
    @Getter
    private String uid;
    @Getter
    private List<String> triggerResults = new ArrayList<>(List.of(ALL_BUILD_RESULT[0]));
    @Getter
    private List<ResultCheck> resultChecks = new ArrayList<>();

    @DataBoundConstructor
    public RemoteJobInfo() {
    }

    /**
     * Whether this remote job trigger checking is enabled, null-safe.
     *
     * @return true when enabled (default), false only when explicitly disabled
     */
    public boolean isEnable() {
        return enable == null || enable;
    }

    @DataBoundSetter
    public void setEnable(boolean enable) {
        this.enable = enable;
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

    @DataBoundSetter
    public void setRemoteJobUrl(String remoteJobUrl) {
        this.remoteJobUrl = remoteJobUrl;
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
         * @param item         the ancestor item (job) whose configuration page hosts this form, may be null
         * @param remoteServer Remote Jenkins Server to be validated
         * @return FormValidation object
         */
        @POST
        @Restricted(NoExternalUse.class)
        public FormValidation doCheckRemoteServer(@AncestorInPath Item item,
                                                  @QueryParameter String remoteServer) {
            if (!hasConfigurePermission(item)) {
                return FormValidation.ok();
            }
            if (StringUtils.isEmpty(remoteServer)) {
                return FormValidation.error("Please select a remote Jenkins Server");
            }
            return FormValidation.ok();
        }

        /**
         * Validates the jobName
         *
         * @param item         the ancestor item (job) whose configuration page hosts this form, may be null
         * @param remoteJobUrl Remote Job Url
         * @return FormValidation object
         */
        @POST
        @Restricted(NoExternalUse.class)
        public FormValidation doCheckRemoteJobUrl(@AncestorInPath Item item,
                                                  @QueryParameter String remoteJobUrl) {
            if (!hasConfigurePermission(item)) {
                return FormValidation.ok();
            }
            if (StringUtils.isEmpty(remoteJobUrl)) {
                return FormValidation.error("Please enter a remote job url");
            }
            return FormValidation.ok();
        }

        /**
         * Validates the uid
         *
         * @param item the ancestor item (job) whose configuration page hosts this form, may be null
         * @param uid  Unique Identifier
         * @return FormValidation object
         */
        @POST
        @Restricted(NoExternalUse.class)
        public FormValidation doCheckUid(@AncestorInPath Item item,
                                         @QueryParameter String uid) {
            if (!hasConfigurePermission(item)) {
                return FormValidation.ok();
            }
            if (StringUtils.isNotEmpty(uid)) {
                if (!uid.matches("[a-zA-Z0-9./_-]*")) {
                    return FormValidation.error("Only support [a-zA-Z0-9./_-] characters");
                }
            }
            return FormValidation.ok();
        }

        /**
         * fill remoteServer select
         *
         * @param item         the ancestor item (job) whose configuration page hosts this form, may be null
         * @param remoteServer currently selected remote server id, kept when the user may not list servers
         * @return fill list model
         */
        @POST
        @Restricted(NoExternalUse.class)
        public ListBoxModel doFillRemoteServerItems(@AncestorInPath Item item,
                                                    @QueryParameter String remoteServer) {
            ListBoxModel model = new ListBoxModel();

            model.add("");

            if (!hasReadPermission(item)) {
                // Without read permission we cannot list servers, but keep the currently
                // selected value so that an existing configuration is not lost.
                if (StringUtils.isNotEmpty(remoteServer)) {
                    model.add(remoteServer, remoteServer);
                }
                return model;
            }

            RemoteJenkinsServer[] servers = RemoteJenkinsServerUtils.getRemoteServers();
            for (RemoteJenkinsServer server : servers) {
                String key = StringUtils.isNotEmpty(server.getDisplayName()) ? server.getDisplayName() : server.getUrl();
                if (server.getId() != null && key != null) {
                    model.add(key, server.getId());
                }
            }

            return model;
        }

        /**
         * Whether the current user may configure this form.
         * <p>
         * These validators run on a job configuration page, so the natural permission is
         * {@link Item#CONFIGURE} on the ancestor item. When there is no item context (e.g. the
         * snippet generator), fall back to the global {@link Jenkins#ADMINISTER}.
         *
         * @param item the ancestor item, may be null
         * @return true when the current user is allowed to configure
         */
        private static boolean hasConfigurePermission(Item item) {
            if (item == null) {
                return Jenkins.get().hasPermission(Jenkins.ADMINISTER);
            }
            return item.hasPermission(Item.CONFIGURE);
        }

        /**
         * Whether the current user may read the values offered by this form's dropdowns.
         * <p>
         * Falls back to the global {@link Jenkins#ADMINISTER} when there is no item context.
         *
         * @param item the ancestor item, may be null
         * @return true when the current user is allowed to read
         */
        private static boolean hasReadPermission(Item item) {
            if (item == null) {
                return Jenkins.get().hasPermission(Jenkins.ADMINISTER);
            }
            return item.hasPermission(Item.EXTENDED_READ) || item.hasPermission(Item.CONFIGURE);
        }
    }
}
