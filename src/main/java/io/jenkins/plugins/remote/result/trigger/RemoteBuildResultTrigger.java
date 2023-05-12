package io.jenkins.plugins.remote.result.trigger;

import antlr.ANTLRException;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Action;
import hudson.model.Node;
import hudson.util.CopyOnWriteList;
import io.jenkins.plugins.remote.result.trigger.exceptions.UnSuccessfulRequestStatusException;
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Remote Build Result Trigger
 *
 * @author HW
 */
public class RemoteBuildResultTrigger extends AbstractTrigger implements Serializable {
    private static final long serialVersionUID = -4059001060991775146L;
    private List<RemoteJobInfo> remoteJobInfos;

    @DataBoundConstructor
    public RemoteBuildResultTrigger(String cronTabSpec, List<RemoteJobInfo> remoteJobInfos) throws ANTLRException {
        super(cronTabSpec);
        // add id
        if (remoteJobInfos != null) {
            for (RemoteJobInfo jobInfo : remoteJobInfos) {
                if (StringUtils.isEmpty(jobInfo.getId())) {
                    jobInfo.setId(UUID.randomUUID().toString());
                }
            }
        }
        this.remoteJobInfos = remoteJobInfos;
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
        boolean changed = false;
        if (CollectionUtils.isNotEmpty(remoteJobInfos)) {
            try {
                for (RemoteJobInfo jobInfo : remoteJobInfos) {
                    // get next build number
                    Integer nextBuildNumber = RemoteJobResultUtils.requestNextBuildNumber(job, jobInfo);
                    if (nextBuildNumber != null) {
                        int triggerNumber = RemoteJobResultUtils.getTriggerNumber(job, jobInfo);
                        // checked remote build
                        for (int number = nextBuildNumber - 1; number > triggerNumber; number--) {
                            SourceMap result = RemoteJobResultUtils.requestBuildResult(job, jobInfo, number);
                            if (result != null) {
                                Integer buildNumber = result.integerValue("number");

                                log.info("Last build url: " + result.stringValue("url"));
                                log.info("Last build number: " + buildNumber);
                                // check need trigger
                                if (jobInfo.getTriggerResults().contains(result.stringValue("result"))) {
                                    // changed
                                    log.info("Need trigger, remote build result: " + result.stringValue("result"));
                                    // cache
                                    RemoteJobResultUtils.saveBuildInfo(job, jobInfo, result);
                                    changed = true;
                                    // saved trigger number
                                    RemoteJobResultUtils.saveTriggerNumber(job, jobInfo, buildNumber);
                                    break;
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
        return changed;
    }

    /**
     * {@link Action}s to be displayed in the job page.
     *
     * @return can be empty but never null
     * @since 1.341
     */
    @Override
    public Collection<? extends Action> getProjectActions() {
        RemoteBuildResultLogAction action = new RemoteBuildResultLogAction(job, getLogFile());
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

    @Extension
    public static class RemoteBuildResultTriggerDescriptor extends XTriggerDescriptor {

        /**
         * To persist global configuration information, simply store it in a field and
         * call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        private CopyOnWriteList<RemoteJenkinsServer> remoteJenkinsServers = new CopyOnWriteList<>();

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
