package io.jenkins.plugins.remote.result.trigger.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import hudson.model.BuildableItem;
import hudson.model.Item;
import io.jenkins.plugins.remote.result.trigger.RemoteJenkinsServer;
import io.jenkins.plugins.remote.result.trigger.exceptions.JenkinsRemoteUnSuccessRequestStatusException;
import io.jenkins.plugins.remote.result.trigger.utils.ssl.SSLSocketManager;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
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
        if (remoteServer == null){
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
        if (remoteServer.getAuth2().getCredentials(job) != null) {
            requestBuilder = requestBuilder.header("Authorization", remoteServer.getAuth2().getCredentials(job));
        }

        // api url
        Request request = requestBuilder.url(getLastSuccessfulBuildApiUrl(job, remoteJenkinsServer, jobName)).get().build();

        Call call = okHttpClient.newCall(request);
        try (Response response = call.execute()) {
            if (response.isSuccessful()) {
                String body = response.body().string();
                // json
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                return SourceMap.of(mapper.readValue(body, Map.class));
            } else {
                throw new JenkinsRemoteUnSuccessRequestStatusException("Remote Request Error", response.code());
            }
        }
    }

    /**
     * get lastSuccessfulBuild api url
     *
     * @param job
     * @param remoteJenkinsServer
     * @param jobName
     * @return
     */
    public static String getLastSuccessfulBuildApiUrl(Item job, String remoteJenkinsServer, String jobName) {
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
     * @param remoteJenkinsServer
     * @param jobName
     * @return
     * @throws IOException
     */
    public static Integer getLocalCacheBuildNumber(Item job, String remoteJenkinsServer, String jobName) throws IOException {
        List<Result> results = getJobRemoteResults(job);
        for (Result result : results) {
            if (result.getServerJobKey().equals(serverJobKey(remoteJenkinsServer, jobName))) {
                return result.getBuildNumber();
            }
        }
        return null;
    }

    /**
     * save build info
     *
     * @param job
     * @param remoteJenkinsServer
     * @param remoteJobName
     * @param remoteJobId
     * @param remoteResult
     */
    public static void saveLastSuccessfulBuild(BuildableItem job, String remoteJenkinsServer,
                                               String remoteJobName, String remoteJobId, SourceMap remoteResult) throws IOException {
        // build cache info
        // server job key
        String serverJobKey = serverJobKey(remoteJenkinsServer, remoteJobName);
        // get cached list and remove cached job
        List<Result> cachedList = getJobRemoteResults(job);
        cachedList.removeIf(result1 -> result1.serverJobKey.equals(serverJobKey));
        // add
        Result result = new Result();
        result.setServerJobKey(serverJobKey);
        result.setRemoteJobName(remoteJobName);
        result.setRemoteJobId(remoteJobId);
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
                envs.putAll(generateEnvs("REMOTE_", result.result));
            }
            // prefix with job id
            String prefix = new StringBuilder("REMOTE_")
                    .append(StringUtils.isNotEmpty(result.getRemoteJobId()) ? result.remoteJobId : result.remoteJobName)
                    .append("_")
                    .toString();
            envs.putAll(generateEnvs(prefix, result.result));
        }
        return envs;
    }


    /**
     * job id
     *
     * @param remoteJenkinsServer
     * @param jobName
     * @return
     */
    private static String serverJobKey(String remoteJenkinsServer, String jobName) {
        return new StringBuilder(remoteJenkinsServer).append("###").append(jobName).toString();
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
    private static Map<String, String> generateEnvs(String prefix, Map<String, Object> result) {
        Map<String, String> envs = new HashMap<>();
        SourceMap sourceMap = SourceMap.of(result);
        // BUILD_NUMBER
        envs.put(prefix + "BUILD_NUMBER", sourceMap.stringValue("number"));
        // TIMESTAMP
        envs.put(prefix + "BUILD_TIMESTAMP", sourceMap.stringValue("timestamp"));
        // BUILD_URL
        envs.put(prefix + "BUILD_URL", sourceMap.stringValue("url"));

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
                                envs.put(prefix + parameterMap.stringValue("name"),
                                        parameterMap.stringValue("value"));
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
        private String serverJobKey;
        private String remoteJobName;
        private String remoteJobId;
        private Integer buildNumber;
        private Map<String, Object> result;

        public String getServerJobKey() {
            return serverJobKey;
        }

        public void setServerJobKey(String serverJobKey) {
            this.serverJobKey = serverJobKey;
        }

        public String getRemoteJobName() {
            return remoteJobName;
        }

        public void setRemoteJobName(String remoteJobName) {
            this.remoteJobName = remoteJobName;
        }

        public String getRemoteJobId() {
            return remoteJobId;
        }

        public void setRemoteJobId(String remoteJobId) {
            this.remoteJobId = remoteJobId;
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
