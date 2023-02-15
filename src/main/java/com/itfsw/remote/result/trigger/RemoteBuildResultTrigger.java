package com.itfsw.remote.result.trigger;

import antlr.ANTLRException;
import com.itfsw.remote.result.trigger.auth2.Auth2;
import com.itfsw.remote.result.trigger.auth2.NoneAuth;
import com.itfsw.remote.result.trigger.utils.HttpClient;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Action;
import hudson.model.BuildableItem;
import hudson.model.Node;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.xtriggerapi.AbstractTrigger;
import org.jenkinsci.plugins.xtriggerapi.XTriggerDescriptor;
import org.jenkinsci.plugins.xtriggerapi.XTriggerException;
import org.jenkinsci.plugins.xtriggerapi.XTriggerLog;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Remote Build Result Trigger
 *
 * @author HW
 * @date 2023/02/13 20:02
 */
public class RemoteBuildResultTrigger extends AbstractTrigger {
    private Auth2 auth2;

    private String url;
    private Boolean trustAllCertificates;
    private String jobName;

    @DataBoundConstructor
    public RemoteBuildResultTrigger(String cronTabSpec, Auth2 auth2, String url, Boolean trustAllCertificates, String jobName) throws ANTLRException {
        super(cronTabSpec);
        this.auth2 = auth2;
        this.url = url;
        this.trustAllCertificates = trustAllCertificates;
        this.jobName = jobName;
    }

    /**
     * Can be overridden if needed
     *
     * @param pollingNode
     * @param project
     * @param newInstance
     * @param log
     */
    @Override
    protected void start(Node pollingNode, BuildableItem project, boolean newInstance, XTriggerLog log) throws XTriggerException {
        if (StringUtils.isNotEmpty(this.url) && StringUtils.isNotEmpty(this.jobName)) {
            HttpClient httpClient = HttpClient.defaultInstance();

            // trustAllCertificates
            if (this.getTrustAllCertificates()) {
                httpClient = httpClient.useUnSafeSsl();
            }

            // get lastSuccessfulBuild
            try {
                HttpClient.Request request = httpClient.request();

                // auth
                if (!(this.auth2 instanceof NoneAuth)) {
                    request = request.header("Authorization", this.auth2.getCredentials(project));
                }

                Map result = request.doGet(
                        new StringBuilder(this.url)
                                .append(this.url.endsWith("/") ? "" : "/")
                                .append("job/")
                                .append(jobName)
                                .append("/lastCompletedBuild/api/json")
                                .toString(),
                        null,
                        Map.class
                );
                // save number and parameter
                if (result != null) {
                    if (result.get("number") != null) {

                    }
                }
            } catch (IOException e) {
                throw new XTriggerException(e);
            }
        }
    }

    @Override
    protected File getLogFile() {
        if (job == null) return null;
        return new File(job.getRootDir(), "trigger-remote-builds.log");
    }

    @Override
    protected boolean requiresWorkspaceForPolling() {
        return false;
    }

    @Override
    protected String getName() {
        return "RemoteResultTrigger";
    }

    @Override
    protected Action[] getScheduledActions(Node pollingNode, XTriggerLog log) {
        return new Action[0];
    }

    @Override
    protected boolean checkIfModified(Node pollingNode, XTriggerLog log) throws XTriggerException {
        return false;
    }

    @Override
    protected String getCause() {
        return null;
    }

    @Override
    public RemoteBuildResultTriggerDescriptor getDescriptor() {
        return (RemoteBuildResultTriggerDescriptor) Jenkins.get().getDescriptorOrDie(getClass());
    }


    // --------------------------------- getter ---------------------------------
    public Auth2 getAuth2() {
        return auth2;
    }

    public String getUrl() {
        return url;
    }

    public String getJobName() {
        return jobName;
    }

    public Boolean getTrustAllCertificates() {
        return trustAllCertificates == null ? true : trustAllCertificates;
    }

    @Extension
    public static class RemoteBuildResultTriggerDescriptor extends XTriggerDescriptor {

        /**
         * Human readable name of this kind of configurable object.
         * Should be overridden for most descriptors, if the display name is visible somehow.
         * As a fallback it uses {@link Class#getSimpleName} on {@link #clazz}, so for example {@code MyThing} from {@code some.pkg.MyThing.DescriptorImpl}.
         * Historically some implementations returned null as a way of hiding the descriptor from the UI,
         * but this is generally managed by an explicit method such as {@code isEnabled} or {@code isApplicable}.
         */
        @NonNull
        @Override
        public String getDisplayName() {
            return "Remote Build Result Trigger";
        }

        public static List<Auth2.Auth2Descriptor> getAuth2Descriptors() {
            return Auth2.all();
        }

        public static Auth2.Auth2Descriptor getDefaultAuth2Descriptor() {
            return NoneAuth.DESCRIPTOR;
        }
    }
}
