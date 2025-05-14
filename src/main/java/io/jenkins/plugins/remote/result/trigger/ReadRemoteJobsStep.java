package io.jenkins.plugins.remote.result.trigger;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.Extension;
import hudson.model.Run;
import io.jenkins.plugins.remote.result.trigger.model.JobResultInfo;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.io.Serial;
import java.util.*;

/**
 * @author heweisc@dingtalk.com
 */
public class ReadRemoteJobsStep extends Step {

    @DataBoundConstructor
    public ReadRemoteJobsStep() {
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new ReadRemoteJobsStepExecution(context);
    }

    @Extension
    public static class ReadRemoteJobsStepDescriptor extends StepDescriptor {

        /**
         * Enumerates any kinds of context the {@link StepExecution} will treat as mandatory.
         * When {@link StepContext#get} is called, the return value may be null in general;
         * if your step cannot trivially handle a null value of a given kind, list that type here.
         * The Pipeline execution engine will then signal a user error before even starting your step if called in an inappropriate context.
         * For example, a step requesting a Launcher may only be run inside a {@code node {…}} block.
         */
        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.singleton(Run.class);
        }

        /**
         * Return a short string that is a valid identifier for programming languages.
         * Follow the pattern {@code [a-z][A-Za-z0-9_]*}.
         * Step will be referenced by this name when used in a programming language.
         */
        @Override
        public String getFunctionName() {
            return "readRemoteJobs";
        }
    }


    public static class ReadRemoteJobsStepExecution extends SynchronousNonBlockingStepExecution<List<Map<?, ?>>> {
        @Serial
        private static final long serialVersionUID = 4436899316471397907L;

        public ReadRemoteJobsStepExecution(@NonNull StepContext context) {
            super(context);
        }

        /**
         * Meat of the execution.
         * <p>
         * When this method returns, a step execution is over.
         */
        @Override
        protected List<Map<?, ?>> run() throws Exception {
            RemoteBuildResultTriggerScheduledAction action = getTriggerAction();
            ArrayList<Map<?, ?>> results = new ArrayList<>();
            if (action != null && !action.getJobResultInfos().isEmpty()) {
                for (JobResultInfo info : action.getJobResultInfos()) {
                    results.add(Map.of(
                            "uid", info.getUid(),
                            "jobUrl", info.getRemoteJobUrl()
                    ));
                }
            }
            return results;
        }

        @Nullable
        @SuppressWarnings("rawtypes")
        private RemoteBuildResultTriggerScheduledAction getTriggerAction() throws IOException, InterruptedException {
            Run run = getContext().get(Run.class);
            if (run != null) {
                return run.getAction(RemoteBuildResultTriggerScheduledAction.class);
            }
            return null;
        }
    }
}
