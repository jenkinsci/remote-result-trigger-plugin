package io.jenkins.plugins.remote.result.trigger.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.model.Item;
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
    public static Map<String, String> getJobRemoteResultEnvs(Item job) throws IOException {
        return getJobRemoteResult(job).getEnvs();
    }

    /**
     * save remote result
     *
     * @param job
     * @param buildNumber
     * @param envs
     */
    public static void saveJobRemoteResult(Item job, int buildNumber, Map<String, String> envs) throws IOException {
        Result result = getJobRemoteResult(job);
        result.setBuildNumber(buildNumber);
        result.setEnvs(envs);

        saveJobRemoteResult(job, result);
    }

    /**
     * check modified
     *
     * @param job
     * @param buildNumber
     * @return
     * @throws IOException
     */
    public static boolean checkIfModified(Item job, Integer buildNumber) throws IOException {
        Result result = getJobRemoteResult(job);
        return !result.getBuildNumber().equals(buildNumber);
    }

    /**
     * save remote result
     *
     * @param job
     * @param result
     * @throws IOException
     */
    private static void saveJobRemoteResult(Item job, Result result) throws IOException {
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
    private static Result getJobRemoteResult(Item job) throws IOException {
        File file = getJobRemoteResultFile(job);
        if (file.exists()) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return mapper.readValue(file, Result.class);
        } else {
            // default
            Result result = new Result();
            result.setBuildNumber(0);
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
    private static File getJobRemoteResultFile(Item job) {
        return new File(job.getRootDir().getAbsolutePath() + "/remote-build-result.json");
    }

    /**
     * Info
     */
    public static class Result {
        private Integer buildNumber;
        private Map<String, String> envs;

        public Integer getBuildNumber() {
            return buildNumber;
        }

        public void setBuildNumber(Integer buildNumber) {
            this.buildNumber = buildNumber;
        }

        public Map<String, String> getEnvs() {
            return envs;
        }

        public void setEnvs(Map<String, String> envs) {
            this.envs = envs;
        }
    }
}
