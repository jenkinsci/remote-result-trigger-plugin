package io.jenkins.plugins.remote.result.trigger;

import io.jenkins.plugins.remote.result.trigger.utils.RemoteJobResultUtils;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentContributor;
import hudson.model.Job;
import hudson.model.TaskListener;

import java.io.IOException;
import java.util.Map;

/**
 * Remote Env Set
 *
 * @author root
 * @date 2023/02/15 13:11
 */
@Extension
public class RemoteBuildResultEnvironmentContributor extends EnvironmentContributor {
    /**
     * Contributes environment variables used for a job.
     *
     * <p>
     * This method can be called repeatedly for the same {@link Job}, thus
     * the computation of this method needs to be efficient.
     *
     * <p>
     * This method gets invoked concurrently for multiple {@link Job}s,
     * so it must be concurrent-safe.
     *
     * @param j        Job for which some activities are launched.
     * @param envs     Partially built environment variable map. Implementation of this method is expected to
     *                 add additional variables here.
     * @param listener Connected to the build console. Can be used to report errors.
     * @since 1.527
     */
    @Override
    public void buildEnvironmentFor(@NonNull Job j, @NonNull EnvVars envs, @NonNull TaskListener listener) throws IOException, InterruptedException {
        Map<String, String> remoteEnvs = RemoteJobResultUtils.getJobRemoteResultEnvs(j);
        if (!remoteEnvs.isEmpty()) {
            envs.putAll(remoteEnvs);
        }
        super.buildEnvironmentFor(j, envs, listener);
    }
}
