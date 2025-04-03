package io.jenkins.plugins.remote.result.trigger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import hudson.Extension;
import hudson.model.Action;
import hudson.model.Node;
import hudson.util.CopyOnWriteList;
import io.jenkins.plugins.remote.result.trigger.exceptions.UnSuccessfulRequestStatusException;
import io.jenkins.plugins.remote.result.trigger.model.ResultCheck;
import io.jenkins.plugins.remote.result.trigger.model.SavedJobInfo;
import io.jenkins.plugins.remote.result.trigger.utils.RemoteJobResultUtils;
import io.jenkins.plugins.remote.result.trigger.utils.SourceMap;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.xtriggerapi.AbstractTrigger;
import org.jenkinsci.plugins.xtriggerapi.XTriggerDescriptor;
import org.jenkinsci.plugins.xtriggerapi.XTriggerException;
import org.jenkinsci.plugins.xtriggerapi.XTriggerLog;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Remote Build Result Trigger
 *
 * @author HW
 */
public class RemoteBuildResultTrigger extends AbstractTrigger implements Serializable {
    private static final long serialVersionUID = -4059001060991775146L;
    private final List<RemoteJobInfo> remoteJobInfos;

    @DataBoundConstructor
    public RemoteBuildResultTrigger(String cronTabSpec, List<RemoteJobInfo> remoteJobInfos) {
        super(cronTabSpec);
        // add id
        if (remoteJobInfos != null) {
            for (RemoteJobInfo jobInfo : remoteJobInfos) {
                jobInfo.setId(UUID.randomUUID().toString());
            }
        }
        this.remoteJobInfos = remoteJobInfos;
    }

    @Override
    protected File getLogFile() {
        if (job == null) {
            return null;
        }
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
    @SuppressFBWarnings(value = "NP_NULL_PARAM_DEREF")
    protected boolean checkIfModified(Node pollingNode, XTriggerLog log) throws XTriggerException {
        boolean modified = false;
        ObjectWriter jsonPretty = new ObjectMapper().writerWithDefaultPrettyPrinter();
        // check job is null
        if (job == null) {
            return false;
        } else if (CollectionUtils.isNotEmpty(remoteJobInfos)) {
            try {
                log.info("Job count: " + remoteJobInfos.size());
                // clean unused build result
                List<SavedJobInfo> removedJobs = RemoteJobResultUtils.cleanUnusedBuildInfo(job, remoteJobInfos);
                if (!removedJobs.isEmpty()) {
                    for (SavedJobInfo removedJob : removedJobs) {
                        log.info("Removing unused job: " + removedJob.getRemoteJobUrl());
                    }
                }
                for (RemoteJobInfo jobInfo : remoteJobInfos) {
                    log.info("================== " + jobInfo.getRemoteJobUrl() + " ==================");
                    // get next build number
                    Integer nextBuildNumber = RemoteJobResultUtils.requestNextBuildNumber(job, jobInfo);
                    if (nextBuildNumber != null) {
                        log.info("Build number: " + (nextBuildNumber - 1));
                        int checkedNumber = RemoteJobResultUtils.getCheckedNumber(job, jobInfo);
                        log.info("Checked number: " + checkedNumber);
                        // checked remote build
                        for (int number = nextBuildNumber - 1; number > checkedNumber; number--) {
                            SourceMap result = RemoteJobResultUtils.requestBuildResult(job, jobInfo, number);
                            if (result != null) {
                                // 清理result,并提取resultJson
                                SourceMap resultJson = cleanAndFixResultJson(result);

                                Integer buildNumber = result.integerValue("number");
                                String buildUrl = result.stringValue("url");

                                log.info("Last build url: " + buildUrl);
                                log.info("Last build number: " + buildNumber);
                                log.info("Remote build result: " + jsonPretty.writeValueAsString(result.getSource()));
                                if (resultJson != null) {
                                    log.info("Remote build result json: " + jsonPretty.writeValueAsString(resultJson.getSource()));
                                }

                                // build completed
                                if (!(result.booleanValue("building") || result.booleanValue("inProgress"))) {

                                    // check need trigger
                                    if (jobInfo.getTriggerResults().contains(result.stringValue("result"))) {
                                        log.info("Result confirmed: " + result.stringValue("result"));
                                        // check result
                                        List<ResultCheck> resultChecks = jobInfo.getResultChecks();
                                        if (CollectionUtils.isNotEmpty(resultChecks)) {
                                            if (resultJson == null) {
                                                log.error("Cannot find remote result json!");
                                            } else {
                                                modified = true;
                                                for (ResultCheck check : resultChecks) {
                                                    if (StringUtils.isNotEmpty(check.getKey())
                                                            && StringUtils.isNotEmpty(check.getExpectedValue())) {
                                                        if (resultJson.containsKey(check.getKey())) {
                                                            String value = resultJson.stringValue(check.getKey());
                                                            Pattern pattern = Pattern.compile(check.getExpectedValue());
                                                            if (!pattern.matcher(value).matches()) {
                                                                // 发现错误，跳出检查
                                                                modified = false;
                                                                break;
                                                            }
                                                        } else {
                                                            modified = false;
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            modified = true;
                                        }

                                        // saved checked number
                                        RemoteJobResultUtils.saveCheckedNumber(job, jobInfo, buildNumber);

                                        if (modified) {
                                            // changed
                                            log.info("Need trigger, remote build result: " + result.stringValue("result"));
                                            // save info
                                            RemoteJobResultUtils.saveBuildInfo(job, jobInfo, result);
                                            if (resultJson != null) {
                                                RemoteJobResultUtils.saveBuildResultJson(job, jobInfo, resultJson);
                                            }
                                            // 这个任务检查完成了，继续下一个任务检查
                                            break;
                                        }
                                    }
                                }
                            } else {
                                // remote server has been deleted
                                throw new XTriggerException("Can't get remote build result, Server maybe deleted");
                            }
                        }
                    }
                }
            } catch (IOException e) {
                throw new XTriggerException("Request last remote have a io exception", e);
            } catch (UnSuccessfulRequestStatusException e) {
                // if status is 404, maybe didn't have a successful build
                if (e.getStatus() != 404) {
                    throw new XTriggerException("Request last remote successful job fail", e);
                }
            }
        } else {
            log.error("No remote job configured!");
        }
        return modified;
    }

    @Override
    protected Action[] getScheduledActions(Node pollingNode, XTriggerLog log) {
        if (job != null) {
            try {
                List<SavedJobInfo> savedJobInfos = RemoteJobResultUtils.getSavedJobInfos(job);
                return new Action[]{
                        new RemoteBuildResultTriggerScheduledAction(job, savedJobInfos)
                };
            } catch (IOException e) {
                // do nothing
            }
        }
        return new Action[0];
    }

    /**
     * {@link Action}s to be displayed in the job page.
     *
     * @return can be empty but never null
     * @since 1.341
     */
    @Override
    public Collection<? extends Action> getProjectActions() {
        RemoteBuildResultTriggerProjectAction action = new RemoteBuildResultTriggerProjectAction(job, getLogFile());
        return Collections.singleton(action);
    }

    @Override
    protected String getCause() {
        return "A successful build result within the remote job";
    }

    @Override
    public RemoteBuildResultTriggerDescriptor getDescriptor() {
        return (RemoteBuildResultTriggerDescriptor) Jenkins.get().getDescriptorOrDie(getClass());
    }

    public List<RemoteJobInfo> getRemoteJobInfos() {
        return remoteJobInfos;
    }

    /**
     * 清理并格式化返回
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private SourceMap cleanAndFixResultJson(SourceMap result) throws JsonProcessingException {
        Map<String, Object> source = result.getSource();
        // 清理changeSets、culprits、artifacts等
        source.remove("changeSets");
        source.remove("culprits");
        source.remove("artifacts");
        source.remove("_class");
        // 清理actions
        List<Map> actions = result.listValue("actions", Map.class);
        SourceMap resultJson = null;
        for (Map action : actions) {
            SourceMap sourceMap = SourceMap.of(action);
            if (RemoteResultAction.class.getName().equals(sourceMap.stringValue("_class"))) {
                Map resultJsonMap = sourceMap.sourceMap("result").getSource();
                resultJsonMap.remove("_class");
                resultJson = SourceMap.of(resultJsonMap);
                break;
            }
        }
        source.remove("actions");
        return resultJson;
    }

    @Extension
    public static class RemoteBuildResultTriggerDescriptor extends XTriggerDescriptor {

        /**
         * To persist global configuration information, simply store it in a field and
         * call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        private final CopyOnWriteList<RemoteJenkinsServer> remoteJenkinsServers = new CopyOnWriteList<>();

        /**
         * In order to load the persisted global configuration, you have to
         * call load() in the constructor.
         */
        public RemoteBuildResultTriggerDescriptor() {
            load();
        }

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

        /**
         * Invoked when the global configuration page is submitted.
         * <p>
         * Can be overridden to store descriptor-specific information.
         *
         * @param req  StaplerRequest
         * @param json The JSON object that captures the configuration data for this {@link hudson.model.Descriptor}.
         *             See <a href="https://www.jenkins.io/doc/developer/forms/structured-form-submission/">the developer documentation</a>.
         * @return false
         * to keep the client in the same config page.
         */
        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            List<RemoteJenkinsServer> servers = req.bindJSONToList(RemoteJenkinsServer.class, json.get("remoteJenkinsServers"));

            // add id
            for (RemoteJenkinsServer server : servers) {
                if (StringUtils.isEmpty(server.getId())) {
                    server.setId(UUID.randomUUID().toString());
                }
            }

            remoteJenkinsServers.replaceBy(servers);

            save();

            return super.configure(req, json);
        }

        public RemoteJenkinsServer[] getRemoteJenkinsServers() {
            return remoteJenkinsServers.toArray(new RemoteJenkinsServer[this.remoteJenkinsServers.size()]);
        }
    }
}
