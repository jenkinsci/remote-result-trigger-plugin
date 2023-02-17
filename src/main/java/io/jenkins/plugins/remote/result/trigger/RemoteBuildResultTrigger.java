package io.jenkins.plugins.remote.result.trigger;

import antlr.ANTLRException;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Action;
import hudson.model.Node;
import hudson.util.FormValidation;
import io.jenkins.plugins.remote.result.trigger.auth2.Auth2;
import io.jenkins.plugins.remote.result.trigger.auth2.NoneAuth;
import io.jenkins.plugins.remote.result.trigger.utils.HttpClient;
import io.jenkins.plugins.remote.result.trigger.utils.RemoteJobResultUtils;
import io.jenkins.plugins.remote.result.trigger.utils.SourceMap;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.xtriggerapi.AbstractTrigger;
import org.jenkinsci.plugins.xtriggerapi.XTriggerDescriptor;
import org.jenkinsci.plugins.xtriggerapi.XTriggerException;
import org.jenkinsci.plugins.xtriggerapi.XTriggerLog;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.trimToNull;

/**
 * Remote Build Result Trigger
 *
 * @author HW
 * @date 2023/02/13 20:02
 */
public class RemoteBuildResultTrigger extends AbstractTrigger implements Serializable {
    private static final long serialVersionUID = -4059001060991775146L;

    private Auth2 auth2;

    private String remoteJenkinsUrl;
    private Boolean trustAllCertificates;
    private String jobName;

    @DataBoundConstructor
    public RemoteBuildResultTrigger(String cronTabSpec, Auth2 auth2, String remoteJenkinsUrl, Boolean trustAllCertificates, String jobName) throws ANTLRException {
        super(cronTabSpec);
        this.auth2 = auth2;
        this.remoteJenkinsUrl = trimToNull(remoteJenkinsUrl);
        this.trustAllCertificates = trustAllCertificates;
        this.jobName = trimToNull(jobName);
    }

    @Override
    protected File getLogFile() {
        if (job == null) return null;
        return new File(job.getRootDir(), "remote-build-result.log");
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

        if (StringUtils.isNotEmpty(this.remoteJenkinsUrl) && StringUtils.isNotEmpty(this.jobName)) {
            HttpClient httpClient = HttpClient.defaultInstance();

            // trustAllCertificates
            if (this.trustAllCertificates != null && this.trustAllCertificates) {
                httpClient = httpClient.useUnSafeSsl();
            }

            // get lastSuccessfulBuild
            try {
                HttpClient.Request request = httpClient.request();

                // auth
                if (this.auth2.getCredentials(job) != null) {
                    request = request.header("Authorization", this.auth2.getCredentials(job));
                }

                String buildInfoUrl = new StringBuilder(this.remoteJenkinsUrl)
                        .append(this.remoteJenkinsUrl.endsWith("/") ? "" : "/")
                        .append("job/").append(this.jobName)
                        .append("/lastSuccessfulBuild/api/json")
                        .toString();
                log.info("Last successful build check api: " + buildInfoUrl);

                Map result = request.doGet(buildInfoUrl, null, Map.class);
                // save number and parameter
                if (result != null) {
                    SourceMap sourceMap = SourceMap.of(result);

                    if (sourceMap.integerValue("number") != null) {
                        // set env
                        Integer buildNumber = sourceMap.integerValue("number");
                        Map<String, String> envs = generateRemoteEnvs(sourceMap);

                        // log
                        log.info("Last successful build number: " + buildNumber);
                        log.info("Last successful build url: " + sourceMap.stringValue("url"));

                        // check if change
                        if (RemoteJobResultUtils.checkIfModified(job, buildNumber)) {
                            RemoteJobResultUtils.saveJobRemoteResult(job, buildNumber, envs);
                            return true;
                        }
                    }
                }
            } catch (IOException e) {
                throw new XTriggerException(e);
            }
        }
        return false;
    }

    @Override
    protected String getCause() {
        return "A successful build result within the remote job";
    }

    @Override
    public RemoteBuildResultTriggerDescriptor getDescriptor() {
        return (RemoteBuildResultTriggerDescriptor) Jenkins.get().getDescriptorOrDie(getClass());
    }

    private Map<String, String> generateRemoteEnvs(SourceMap sourceMap) {
        Map<String, String> envs = new HashMap<>();
        String prefix = "REMOTE_";
        // BUILD_NUMBER
        envs.put(prefix + "BUILD_NUMBER", sourceMap.stringValue("number"));
        // TIMESTAMP
        envs.put(prefix + "BUILD_TIMESTAMP", sourceMap.stringValue("timestamp"));
        // BUILD_URL
        envs.put(prefix + "BUILD_URL", sourceMap.stringValue("url"));

        // Parameters
        List<Map> actions = sourceMap.listValue("actions", Map.class);
        if (actions != null) {
            for (Map action : actions) {
                SourceMap actionMap = SourceMap.of(action);
                if (actionMap.stringValue("_class") != null
                        && "hudson.model.ParametersAction".equals(actionMap.stringValue("_class"))) {
                    List<Map> parameters = actionMap.listValue("parameters", Map.class);
                    if (parameters != null) {
                        for (Map parameter : parameters) {
                            SourceMap parameterMap = SourceMap.of(parameter);
                            if (parameterMap.stringValue("name") != null) {
                                envs.put(prefix + parameterMap.stringValue("name"),
                                        parameterMap.stringValue("value"));
                            }
                        }
                    }
                }
            }
        }

        return envs;
    }

    // --------------------------------- getter ---------------------------------
    public Auth2 getAuth2() {
        return auth2;
    }

    public String getRemoteJenkinsUrl() {
        return remoteJenkinsUrl;
    }

    public String getJobName() {
        return jobName;
    }

    public Boolean getTrustAllCertificates() {
        return trustAllCertificates;
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

        /**
         * Returns the resource path to the help screen HTML, if any.
         *
         * <p>
         * Starting 1.282, this method uses "convention over configuration" &mdash; you should
         * just put the "help.html" (and its localized versions, if any) in the same directory
         * you put your Jelly view files, and this method will automatically does the right thing.
         *
         * <p>
         * This value is relative to the context root of Hudson, so normally
         * the values are something like {@code "/plugin/emma/help.html"} to
         * refer to static resource files in a plugin, or {@code "/publisher/EmmaPublisher/abc"}
         * to refer to Jelly script {@code abc.jelly} or a method {@code EmmaPublisher.doAbc()}.
         *
         * @return null to indicate that there's no help.
         */
        @Override
        public String getHelpFile() {
            return "/plugin/remote-result-trigger/help.html";
        }

        @POST
        @Restricted(NoExternalUse.class)
        public FormValidation doCheckRemoteJenkinsUrl(@QueryParameter("remoteJenkinsUrl") final String remoteJenkinsUrl) {
            if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                return FormValidation.ok();
            }
            if (StringUtils.isEmpty(remoteJenkinsUrl) || !isURL(remoteJenkinsUrl)) {
                return FormValidation.error("Invalid URL in 'Remote Jenkins URL'");
            }
            return FormValidation.ok();
        }

        @POST
        @Restricted(NoExternalUse.class)
        public FormValidation doCheckJobName(@QueryParameter("jobName") final String jobName) {
            // check jobName
            if (StringUtils.isEmpty(jobName)) {
                return FormValidation.error("'Remote Job Name' not specified");
            }
            return FormValidation.ok();
        }

        /**
         * Checks if a string is a valid http/https URL.
         *
         * @param string the url to check.
         * @return true if parameter is a valid http/https URL.
         */
        private boolean isURL(String string) {
            if (isEmpty(trimToNull(string))) return false;
            String stringLower = string.toLowerCase();
            if (stringLower.startsWith("http://") || stringLower.startsWith("https://")) {
                if (stringLower.indexOf("://") >= stringLower.length() - 3) {
                    return false; //URL ends after protocol
                }
                if (stringLower.indexOf("$") >= 0) {
                    return false; //We interpret $ in URLs as variables which need to be replaced. TODO: What about URI standard which allows $?
                }
                try {
                    new URL(string);
                    return true;
                } catch (MalformedURLException e) {
                    return false;
                }
            } else {
                return false;
            }
        }

        public static List<Auth2.Auth2Descriptor> getAuth2Descriptors() {
            return Auth2.all();
        }

        public static Auth2.Auth2Descriptor getDefaultAuth2Descriptor() {
            return NoneAuth.DESCRIPTOR;
        }
    }
}
