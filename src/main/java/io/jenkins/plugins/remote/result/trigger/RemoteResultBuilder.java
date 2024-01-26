package io.jenkins.plugins.remote.result.trigger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.jetbrains.annotations.NotNull;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author heweisc@dingtalk.com
 */
public class RemoteResultBuilder extends Builder implements SimpleBuildStep, Serializable {
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

    @Override
    public void perform(@NotNull Run<?, ?> run, @NotNull FilePath workspace, @NotNull Launcher launcher, @NotNull TaskListener listener) throws InterruptedException, IOException {
        if (result != null && result.startsWith("{") && result.endsWith("}")) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.readValue(result, new TypeReference<Map<String, String>>() {
                });
            } catch (Exception e) {
                throw new JsonNotMatchException("Not Json Map Str:" + result);
            }
            // envs
            String expand = run.getEnvironment(listener).expand(result);
            new FilePath(run.getRootDir()).child("remote-result-trigger.json").write(expand, StandardCharsets.UTF_8.name());
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
        @NotNull
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
