package io.jenkins.plugins.remote.result.trigger.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import hudson.model.BuildableItem;
import hudson.model.Item;
import io.jenkins.plugins.remote.result.trigger.RemoteJenkinsServer;
import io.jenkins.plugins.remote.result.trigger.RemoteJobInfo;
import io.jenkins.plugins.remote.result.trigger.exceptions.JenkinsRemoteUnSuccessRequestStatusException;
import io.jenkins.plugins.remote.result.trigger.utils.ssl.SSLSocketManager;
import okhttp3.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Remote Result Result Cache
 *
 * @author HW
 * @date 2023/02/15 13:41
 */
public class RemoteJobResultUtils {
    /**
     * do api request
     *
     * @param job
     * @param remoteJenkinsServer
     * @param jobName
     * @return
     * @throws IOException
     */
    public static SourceMap requestLastSuccessfulBuild(Item job, String remoteJenkinsServer, String jobName) throws IOException, JenkinsRemoteUnSuccessRequestStatusException {
        // OkHttp Client
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

        RemoteJenkinsServer remoteServer = RemoteJenkinsServerUtils.getRemoteJenkinsServer(remoteJenkinsServer);

        // remote server configuration deleted
        if (remoteServer == null) {
            return null;
        }

        // trustAllCertificates
        if (remoteServer.isTrustAllCertificates()) {
            clientBuilder = clientBuilder
                    .sslSocketFactory(SSLSocketManager.getSSLSocketFactory(),
                            (X509TrustManager) SSLSocketManager.getTrustManager()[0])
                    .hostnameVerifier(SSLSocketManager.getHostnameVerifier());
        }
        OkHttpClient okHttpClient = clientBuilder.build();

        // OkHttp Request
        Request.Builder requestBuilder = new Request.Builder();
        // auth
        if (remoteServer.getAuth2() != null && remoteServer.getAuth2().getCredentials(job) != null) {
            requestBuilder = requestBuilder.header("Authorization", remoteServer.getAuth2().getCredentials(job));
        }

        // api url
        Request request = requestBuilder.url(getLastSuccessfulBuildApiUrl(remoteJenkinsServer, jobName)).get().build();

        Call call = okHttpClient.newCall(request);
        try (Response response = call.execute()) {
            if (response.isSuccessful()) {
                ResponseBody responseBody = response.body();
                if (null != responseBody) {
                    String body = responseBody.string();
                    // json
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                    return SourceMap.of(mapper.readValue(body, Map.class));
                }
            } else {
                throw new JenkinsRemoteUnSuccessRequestStatusException("Remote Request Error", response.code());
            }
        }
        return null;
    }

    /**
     * get lastSuccessfulBuild api url
     *
     * @param remoteJenkinsServer
     * @param jobName
     * @return
     */
    public static String getLastSuccessfulBuildApiUrl(String remoteJenkinsServer, String jobName) {
        RemoteJenkinsServer remoteServer = RemoteJenkinsServerUtils.getRemoteJenkinsServer(remoteJenkinsServer);
        if (remoteServer != null) {
            return new StringBuilder(remoteServer.getUrl())
                    .append(remoteServer.getUrl().endsWith("/") ? "" : "/")
                    .append("job/").append(jobName)
                    .append("/lastSuccessfulBuild/api/json")
                    .toString();
        }
        return null;
    }

    /**
     * local cache build number
     *
     * @param job
     * @param remoteServer
     * @param remoteJob
     * @return
     * @throws IOException
     */
    public static Integer getLocalCacheBuildNumber(Item job, String remoteServer, String remoteJob) throws IOException {
        List<Result> results = getJobRemoteResults(job);
        for (Result result : results) {
            if (StringUtils.equals(remoteServer, result.getRemoteServer()) && StringUtils.equals(remoteJob, result.getRemoteJob())) {
                return result.getBuildNumber();
            }
        }
        return null;
    }

    /**
     * save build info
     *
     * @param job
     * @param jobInfo
     * @param remoteResult
     */
    public static void saveLastSuccessfulBuild(BuildableItem job, RemoteJobInfo jobInfo, SourceMap remoteResult) throws IOException {
        // build cache info
        // get cached list and remove cached job
        List<Result> cachedList = getJobRemoteResults(job);
        cachedList.removeIf(result -> StringUtils.equals(jobInfo.getRemoteJenkinsServer(), result.getRemoteServer()) && StringUtils.equals(jobInfo.getId(), result.getRemoteJob()));
        // add
        Result result = new Result();
        result.setRemoteServer(jobInfo.getRemoteJenkinsServer());
        result.setRemoteJob(jobInfo.getId());
        result.setRemoteJobName(jobInfo.getRemoteJobName());
        result.setRemoteJobReplacement(jobInfo.getRemoteJobReplacement());
        result.setBuildNumber(remoteResult.integerValue("number"));
        result.setResult(remoteResult.getSource());
        cachedList.add(result);

        // cache
        File file = getJobRemoteResultFile(job);
        if (!file.getParentFile().exists()) {
            FileUtils.forceMkdirParent(file);
        }
        String string = new ObjectMapper().writeValueAsString(cachedList);
        FileUtils.writeStringToFile(file, string, StandardCharsets.UTF_8);
    }

    /**
     * get remote result envs
     *
     * @param job
     * @return
     * @throws IOException
     */
    public static Map<String, String> getJobRemoteResultEnvs(Item job) throws IOException {
        Map<String, String> envs = new HashMap<>();
        List<Result> results = getJobRemoteResults(job);
        for (int i = 0; i < results.size(); i++) {
            Result result = results.get(i);
            // only one
            if (i == 0) {
                envs.putAll(generateEnvs("REMOTE_", result));
            }
            // prefix with job id
            String prefix = new StringBuilder("REMOTE_")
                    .append(StringUtils.isNotEmpty(result.getRemoteJobReplacement()) ? result.remoteJobReplacement : result.remoteJobName)
                    .append("_")
                    .toString();
            envs.putAll(generateEnvs(prefix, result));
        }
        return envs;
    }

    /**
     * get cache result
     *
     * @param job
     * @return
     * @throws IOException
     */
    private static List<Result> getJobRemoteResults(Item job) throws IOException {
        File file = getJobRemoteResultFile(job);
        if (file.exists()) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            CollectionType collectionType = TypeFactory.defaultInstance().constructCollectionType(List.class, Result.class);
            return mapper.readValue(file, collectionType);
        }
        return new ArrayList<>();
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
     * Generate envs
     *
     * @param prefix
     * @param result
     * @return
     */
    private static Map<String, String> generateEnvs(String prefix, Result result) {
        Map<String, String> envs = new HashMap<>();
        SourceMap sourceMap = SourceMap.of(result.result);
        // BUILD_NUMBER
        envs.put(prefix + "BUILD_NUMBER", sourceMap.stringValue("number"));
        // TIMESTAMP
        envs.put(prefix + "BUILD_TIMESTAMP", sourceMap.stringValue("timestamp"));
        // BUILD_URL
        envs.put(prefix + "BUILD_URL", sourceMap.stringValue("url"));
        // JOB_NAME
        envs.put(prefix + "JOB_NAME", result.getRemoteJobName());
        // JOB_ID
        if (StringUtils.isNotEmpty(result.getRemoteJobReplacement())) {
            envs.put(prefix + "JOB_ID", result.getRemoteJobReplacement());
        }

        // Parameters
        List<Map> actions = sourceMap.listValue("actions", Map.class);
        if (actions != null) {
            for (Map action : actions) {
                SourceMap actionMap = SourceMap.of(action);
                if (actionMap.stringValue("_class") != null
                        && "hudson.model.ParametersAction".equals(actionMap.stringValue("_class"))) {
                    List<Map> parameters = actionMap.listValue("parameters", Map.class);
                    if (parameters != null) {
                        for (Map parameter : parameters) {
                            SourceMap parameterMap = SourceMap.of(parameter);
                            if (parameterMap.stringValue("name") != null) {
                                String key = new StringBuilder(prefix)
                                        .append("PARAMETER_")
                                        .append(parameterMap.stringValue("name"))
                                        .toString();
                                envs.put(key, parameterMap.stringValue("value"));
                            }
                        }
                    }
                }
            }
        }

        return envs;
    }


    /**
     * Info
     */
    public static class Result {
        private String remoteServer;
        private String remoteJob;
        private String remoteJobName;
        private String remoteJobReplacement;
        private Integer buildNumber;
        private Map<String, Object> result;

        public String getRemoteServer() {
            return remoteServer;
        }

        public void setRemoteServer(String remoteServer) {
            this.remoteServer = remoteServer;
        }

        public String getRemoteJob() {
            return remoteJob;
        }

        public void setRemoteJob(String remoteJob) {
            this.remoteJob = remoteJob;
        }

        public String getRemoteJobName() {
            return remoteJobName;
        }

        public void setRemoteJobName(String remoteJobName) {
            this.remoteJobName = remoteJobName;
        }

        public String getRemoteJobReplacement() {
            return remoteJobReplacement;
        }

        public void setRemoteJobReplacement(String remoteJobReplacement) {
            this.remoteJobReplacement = remoteJobReplacement;
        }

        public Integer getBuildNumber() {
            return buildNumber;
        }

        public void setBuildNumber(Integer buildNumber) {
            this.buildNumber = buildNumber;
        }

        public Map<String, Object> getResult() {
            return result;
        }

        public void setResult(Map<String, Object> result) {
            this.result = result;
        }
    }
}
