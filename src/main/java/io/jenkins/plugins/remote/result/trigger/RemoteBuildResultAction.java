package io.jenkins.plugins.remote.result.trigger;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Functions;
import hudson.Util;
import hudson.console.AnnotatedLargeText;
import hudson.model.Action;
import hudson.model.BuildableItem;
import org.apache.commons.jelly.XMLOutput;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * log action
 *
 * @author HW
 */
public class RemoteBuildResultAction implements Action {
    private final BuildableItem job;
    private final File logFile;

    public RemoteBuildResultAction(BuildableItem job, File logFile) {
        this.job = job;
        this.logFile = logFile;
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
        return "Remote Result Trigger Log";
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
        return "remote-build-result-trigger-log";
    }

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
    public void writeLogTo(XMLOutput out) throws IOException {
        new AnnotatedLargeText<RemoteBuildResultAction>(logFile, Charset.defaultCharset(), true, this).writeHtmlTo(0, out.asWriter());
    }

    public String getLog() throws IOException {
        return Util.loadFile(logFile);
    }

    @SuppressWarnings("unused")
    public BuildableItem getOwner() {
        return job;
    }
}
