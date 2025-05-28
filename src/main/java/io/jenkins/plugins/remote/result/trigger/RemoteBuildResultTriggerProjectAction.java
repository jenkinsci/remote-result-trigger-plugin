package io.jenkins.plugins.remote.result.trigger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Functions;
import hudson.Util;
import hudson.console.AnnotatedLargeText;
import hudson.model.Action;
import hudson.model.BuildableItem;
import io.jenkins.plugins.remote.result.trigger.model.JobResultDisplayInfo;
import io.jenkins.plugins.remote.result.trigger.model.JobResultInfo;
import io.jenkins.plugins.remote.result.trigger.utils.RemoteJobResultUtils;
import org.apache.commons.jelly.XMLOutput;
import org.kohsuke.stapler.interceptor.RequirePOST;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * log action
 *
 * @author HW
 */
public class RemoteBuildResultTriggerProjectAction implements Action {
    private final BuildableItem job;
    private final File logFile;

    public RemoteBuildResultTriggerProjectAction(BuildableItem job, File logFile) {
        this.job = job;
        this.logFile = logFile;
    }

    public List<JobResultDisplayInfo> getJobResultDisplayInfos() throws IOException {
        ObjectWriter jsonPretty = new ObjectMapper().writerWithDefaultPrettyPrinter();
        List<JobResultInfo> jobResultInfos = RemoteJobResultUtils.getSavedJobInfos(job);
        List<JobResultDisplayInfo> results = new ArrayList<>();
        for (JobResultInfo jobResultInfo : jobResultInfos) {
            JobResultDisplayInfo info = new JobResultDisplayInfo();
            info.setRemoteJobUrl(jobResultInfo.getRemoteJobUrl());
            info.setBuildUrl(jobResultInfo.getBuildUrl());
            info.setResult(jsonPretty.writeValueAsString(jobResultInfo.getBuildResult()));
            if (jobResultInfo.getRemoteResult() != null) {
                info.setResultJson(jsonPretty.writeValueAsString(jobResultInfo.getRemoteResult()));
            }
            results.add(info);
        }
        return results;
    }

    /**
     * 清理
     */
    @RequirePOST
    @SuppressWarnings("unused")
    public void doClean() throws IOException {
        RemoteJobResultUtils.cleanCache(job);
    }

    /**
     * Gets the name of the icon.
     *
     * @return If the icon name is prefixed with "symbol-", a Jenkins Symbol
     * will be used.
     * <p>
     * If just a file name (like "abc.gif") is returned, it will be
     * interpreted as a file name inside {@code /images/24x24}.
     * This is useful for using one of the stock images.
     * <p>
     * If an absolute file name that starts from '/' is returned (like
     * "/plugin/foo/abc.gif"), then it will be interpreted as a path
     * from the context root of Jenkins. This is useful to pick up
     * image files from a plugin.
     * <p>
     * Finally, return null to hide it from the task list. This is normally not very useful,
     * but this can be used for actions that only contribute {@code floatBox.jelly}
     * and no task list item. The other case where this is useful is
     * to avoid showing links that require a privilege when the user is anonymous.
     * @see <a href="https://www.jenkins.io/doc/developer/views/symbols/">Jenkins Symbols</a>
     * @see Functions#isAnonymous()
     * @see Functions#getIconFilePath(Action)
     */
    @Override
    public String getIconFileName() {
        return "symbol-details";
    }

    /**
     * Gets the string to be displayed.
     * <p>
     * The convention is to capitalize the first letter of each word,
     * such as "Test Result".
     *
     * @return Can be null in case the action is hidden.
     */
    @Override
    public String getDisplayName() {
        return "Remote Result Trigger";
    }

    /**
     * Gets the URL path name.
     *
     * <p>
     * For example, if this method returns "xyz", and if the parent object
     * (that this action is associated with) is bound to /foo/bar/zot,
     * then this action object will be exposed to /foo/bar/zot/xyz.
     *
     * <p>
     * This method should return a string that's unique among other {@link Action}s.
     *
     * <p>
     * The returned string can be an absolute URL, like "http://www.sun.com/",
     * which is useful for directly connecting to external systems.
     *
     * <p>
     * If the returned string starts with '/', like '/foo', then it's assumed to be
     * relative to the context path of the Jenkins webapp.
     *
     * @return null if this action object doesn't need to be bound to web
     * (when you do that, be sure to also return null from {@link #getIconFileName()}.
     * @see Functions#getActionUrl(String, Action)
     */
    @Override
    public String getUrlName() {
        return "remote-result-trigger";
    }

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
    public void writeLogTo(XMLOutput out) throws IOException {
        new AnnotatedLargeText<>(logFile, StandardCharsets.UTF_8, true, this).writeHtmlTo(0, out.asWriter());
    }

    public String getLog() throws IOException {
        return Util.loadFile(logFile, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unused")
    public BuildableItem getOwner() {
        return job;
    }
}
