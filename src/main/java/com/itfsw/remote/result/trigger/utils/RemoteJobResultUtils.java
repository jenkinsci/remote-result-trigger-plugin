package com.itfsw.remote.result.trigger.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.model.Job;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Remote Result Result Cache
 *
 * @author HW
 * @date 2023/02/15 13:41
 */
public class RemoteJobResultUtils {
    /**
     * get remote result envs
     *
     * @param job
     * @return
     * @throws IOException
     */
    public static Map<String, String> getJobRemoteResultEnvs(Job job) throws IOException {
        return getJobRemoteResult(job).getEnvs();
    }

    /**
     * save remote result
     *
     * @param job
     * @param buildNumber
     * @param envs
     */
    public static void saveJobRemoteResult(Job job, int buildNumber, Map<String, String> envs) throws IOException {
        Result result = getJobRemoteResult(job);
        result.setRemoteBuildNumber(buildNumber);
        result.setEnvs(envs);

        saveJobRemoteResult(job, result);
    }

    /**
     * save use number
     *
     * @param job
     */
    public static void saveJobRemoteResultUse(Job job) throws IOException {
        Result result = getJobRemoteResult(job);
        result.setUsedBuildNumber(result.getRemoteBuildNumber());
        saveJobRemoteResult(job, result);
    }

    /**
     * check modified
     *
     * @param job
     * @return
     * @throws IOException
     */
    public static boolean checkIfModified(Job job) throws IOException {
        Result result = getJobRemoteResult(job);
        return result.getRemoteBuildNumber().equals(result.getUsedBuildNumber());
    }

    /**
     * save remote result
     *
     * @param job
     * @param result
     * @throws IOException
     */
    private static void saveJobRemoteResult(Job job, Result result) throws IOException {
        File file = getJobRemoteResultFile(job);
        if (!file.getParentFile().exists()) {
            FileUtils.forceMkdirParent(file);
        }

        String string = new ObjectMapper().writeValueAsString(result);
        FileUtils.writeStringToFile(file, string, StandardCharsets.UTF_8);
    }

    /**
     * get cache result
     *
     * @param job
     * @return
     * @throws IOException
     */
    private static Result getJobRemoteResult(Job job) throws IOException {
        File file = getJobRemoteResultFile(job);
        if (file.exists()) {
            return new ObjectMapper().readValue(file, Result.class);
        } else {
            // default
            Result result = new Result();
            result.setRemoteBuildNumber(0);
            result.setUsedBuildNumber(0);
            result.setEnvs(new HashMap<>());
            return result;
        }
    }

    /**
     * get remote result cache file
     *
     * @param job
     * @return
     */
    private static File getJobRemoteResultFile(Job job) {
        return new File(job.getRootDir().getAbsolutePath() + "/remote-build-result.json");
    }

    /**
     * Info
     */
    public static class Result {
        private Integer remoteBuildNumber;
        private Integer usedBuildNumber;
        private Map<String, String> envs;

        public Integer getRemoteBuildNumber() {
            return remoteBuildNumber;
        }

        public void setRemoteBuildNumber(Integer remoteBuildNumber) {
            this.remoteBuildNumber = remoteBuildNumber;
        }

        public Integer getUsedBuildNumber() {
            return usedBuildNumber;
        }

        public void setUsedBuildNumber(Integer usedBuildNumber) {
            this.usedBuildNumber = usedBuildNumber;
        }

        public Map<String, String> getEnvs() {
            return envs;
        }

        public void setEnvs(Map<String, String> envs) {
            this.envs = envs;
        }
    }
}
