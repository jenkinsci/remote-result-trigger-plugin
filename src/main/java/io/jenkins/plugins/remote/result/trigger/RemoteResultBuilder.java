package io.jenkins.plugins.remote.result.trigger;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import io.jenkins.plugins.remote.result.trigger.exceptions.JsonNotMatchException;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * @author heweisc@dingtalk.com
 */
public class RemoteResultBuilder extends Builder implements SimpleBuildStep, Serializable {
    @Serial
    private static final long serialVersionUID = -1800772775254484836L;
    private String result;

    @DataBoundConstructor
    public RemoteResultBuilder(String result) {
        this.result = result;
    }

    @DataBoundSetter
    public void setResult(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }

    /**
     * Run this step.
     * <p>
     * This method <strong>must</strong> be overridden when this step requires a workspace context. If such a context is
     * <em>not</em> required, it does not need to be overridden; it will then forward to
     * {@link #perform(Run, EnvVars, TaskListener)}.
     *
     * @param run       a build this is running as a part of
     * @param workspace a workspace to use for any file operations
     * @param env       environment variables applicable to this step
     * @param launcher  a way to start processes
     * @param listener  a place to send output
     * @throws AbstractMethodError  if this step requires a workspace context and neither this method nor {@link #perform(Run, FilePath, Launcher, TaskListener)} is overridden
     * @throws InterruptedException if the step is interrupted
     * @throws IOException          if something goes wrong; use {@link hudson.AbortException} for a polite error
     * @since 2.241
     */
    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace, @NonNull EnvVars env, @NonNull Launcher launcher, @NonNull TaskListener listener) throws InterruptedException, IOException {
        if (result != null && result.startsWith("{") && result.endsWith("}")) {
            // envs
            String expand = run.getEnvironment(listener).expand(result);
            // 确认是map json
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> result = mapper.readValue(expand, new TypeReference<>() {
                });
                run.addAction(new RemoteResultAction(run, result));
            } catch (JsonProcessingException e) {
                throw new JsonNotMatchException("Not Json Map Str:" + result, e);
            }
        } else {
            throw new JsonNotMatchException("Not Json Map Str:" + result);
        }
    }

    @Symbol("pubResult")
    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {
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
            return "Publish Build Result";
        }

        /**
         * Returns true if this task is applicable to the given project.
         *
         * @param jobType job type
         * @return true to allow user to configure this post-promotion task for the given project.
         * @see hudson.model.AbstractProject.AbstractProjectDescriptor
         */
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }
}
