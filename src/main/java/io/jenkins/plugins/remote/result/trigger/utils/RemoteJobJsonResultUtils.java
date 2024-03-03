package io.jenkins.plugins.remote.result.trigger.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.FilePath;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author HW
 */
public class RemoteJobJsonResultUtils {
    private final static String JSON_FILE = "remote-result-trigger-result.json";

    /**
     * 保存构建信息
     */
    public static void saveJson(String fullJobPath, Integer buildNumber, String result)
            throws IOException, InterruptedException {
        ObjectMapper mapper = new ObjectMapper();
        String jobName = StringUtils.substringBefore(StringUtils.substringAfter(fullJobPath, "/job/"), "/");
        FilePath file = new FilePath(Jenkins.get().getRootDir()).child("jobs/" + jobName + "/" + JSON_FILE);
        Map<String, String> json = new HashMap<>();
        if (file.exists()) {
            json = mapper.readValue(file.readToString(), Map.class);
        }
        json.put(fullJobPath + buildNumber + "/", mapper.writeValueAsString(mapper.readValue(result, Map.class)));
        file.write(mapper.writeValueAsString(json), StandardCharsets.UTF_8.name());
    }

    /**
     * 获取构建信息
     */
    public static String getJson(String jobBuildFullPath) throws IOException, InterruptedException {
        String jobName = StringUtils.substringBefore(StringUtils.substringAfter(jobBuildFullPath, "/job/"), "/");
        FilePath file = new FilePath(Jenkins.get().getRootDir()).child("jobs/" + jobName + "/" + JSON_FILE);
        if (file.exists()) {
            Map<String, String> json = new ObjectMapper().readValue(file.readToString(), Map.class);
            return json.get(jobBuildFullPath);
        }
        return null;
    }
}
