package io.jenkins.plugins.remote.result.trigger.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import hudson.model.BuildableItem;
import hudson.model.Item;
import io.jenkins.plugins.remote.result.trigger.RemoteJenkinsServer;
import io.jenkins.plugins.remote.result.trigger.RemoteJobInfo;
import io.jenkins.plugins.remote.result.trigger.exceptions.UnSuccessfulRequestStatusException;
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
 */
public class RemoteJobResultUtils {
    /**
     * get remote job next build number
     *
     * @param job     Jenkins job
     * @param jobInfo remote Job info
     * @return 下一个版本
     */
    public static Integer requestNextBuildNumber(Item job, RemoteJobInfo jobInfo)
            throws UnSuccessfulRequestStatusException, IOException {
        SourceMap result = requestRemoteApi(job, jobInfo, "job/" + jobInfo.getRemoteJobName() + "/api/json");
        if (result != null) {
            return result.integerValue("nextBuildNumber");
        }
        return null;
    }

    /**
     * get remote job result
     *
     * @param job     Jenkins job
     * @param jobInfo remote Job info
     * @param number  build number
     * @return api result
     */
    public static SourceMap requestBuildResult(Item job, RemoteJobInfo jobInfo, int number)
            throws UnSuccessfulRequestStatusException, IOException {
        return requestRemoteApi(job, jobInfo, "job/" + jobInfo.getRemoteJobName() + "/" + number + "/api/json");
    }

    /**
     * get remote job result json
     *
     * @param job      Jenkins job
     * @param jobInfo  remote Job info
     * @param buildUrl build url
     * @return api result
     */
    public static SourceMap requestBuildResultJson(Item job, RemoteJobInfo jobInfo, String buildUrl)
            throws UnSuccessfulRequestStatusException, IOException {
        return requestRemoteApi(job, jobInfo, "remote-result/result.json?buildUrl=" + buildUrl);
    }

    /**
     * last trigger build number
     *
     * @param job     Jenkins job
     * @param jobInfo remote Job info
     * @return last trigger number
     */
    public static int getTriggerNumber(Item job, RemoteJobInfo jobInfo) throws IOException {
        SavedJobInfo savedJobInfo = getSavedJobInfo(job, jobInfo);
        return savedJobInfo == null || savedJobInfo.getTriggerNumber() == null ? 0 : savedJobInfo.getTriggerNumber();
    }

    /**
     * save build trigger number
     *
     * @param job     Jenkins job
     * @param jobInfo remote Job info
     * @param number  trigger number
     */
    public static void saveTriggerNumber(BuildableItem job, RemoteJobInfo jobInfo, int number) throws IOException {
        SavedJobInfo savedJobInfo = getSavedJobInfo(job, jobInfo);
        if (savedJobInfo == null) {
            savedJobInfo = new SavedJobInfo();
        }

        savedJobInfo.setTriggerNumber(number);

        saveBuildInfo(job, savedJobInfo);
    }

    /**
     * save build result json
     *
     * @param job        Jenkins job
     * @param jobInfo    remote Job info
     * @param resultJson result json
     */
    public static void saveBuildResultJson(BuildableItem job, RemoteJobInfo jobInfo, SourceMap resultJson) throws IOException {
        SavedJobInfo savedJobInfo = getSavedJobInfo(job, jobInfo);
        if (savedJobInfo == null) {
            savedJobInfo = new SavedJobInfo();
        }

        savedJobInfo.setRemoteServer(jobInfo.getRemoteServer());
        savedJobInfo.setRemoteJob(jobInfo.getId());
        savedJobInfo.setRemoteJobName(jobInfo.getRemoteJobName());
        savedJobInfo.setUid(jobInfo.getUid());
        savedJobInfo.setResultJson(resultJson.getSource());

        saveBuildInfo(job, savedJobInfo);
    }

    /**
     * save build info
     *
     * @param job          Jenkins job
     * @param jobInfo      remote Job info
     * @param remoteResult api result
     */
    public static void saveBuildInfo(BuildableItem job, RemoteJobInfo jobInfo, SourceMap remoteResult) throws IOException {
        SavedJobInfo savedJobInfo = getSavedJobInfo(job, jobInfo);
        if (savedJobInfo == null) {
            savedJobInfo = new SavedJobInfo();
        }

        savedJobInfo.setRemoteServer(jobInfo.getRemoteServer());
        savedJobInfo.setRemoteJob(jobInfo.getId());
        savedJobInfo.setRemoteJobName(jobInfo.getRemoteJobName());
        savedJobInfo.setUid(jobInfo.getUid());
        savedJobInfo.setResult(remoteResult.getSource());

        saveBuildInfo(job, savedJobInfo);
    }

    /**
     * save build info to file
     *
     * @param job          Jenkins job
     * @param savedJobInfo save info
     */
    private static void saveBuildInfo(BuildableItem job, SavedJobInfo savedJobInfo) throws IOException {
        // get saved list
        List<SavedJobInfo> savedJobInfos = getSavedJobInfos(job);
        // remove old
        savedJobInfos.removeIf(
                info -> info.getRemoteServer().equals(savedJobInfo.getRemoteServer())
                        && info.getRemoteJob().equals(savedJobInfo.getRemoteJob())
        );
        savedJobInfos.add(savedJobInfo);
        // save to file
        File file = getRemoteResultConfigFile(job);
        if (!file.getParentFile().exists()) {
            FileUtils.forceMkdirParent(file);
        }
        String string = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(savedJobInfos);
        FileUtils.writeStringToFile(file, string, StandardCharsets.UTF_8);
    }

    /**
     * get remote result envs
     *
     * @param job Jenkins job
     * @return envs
     */
    public static Map<String, String> getJobRemoteResultEnvs(Item job) throws IOException {
        Map<String, String> envs = new HashMap<>();
        List<SavedJobInfo> savedJobInfos = getSavedJobInfos(job);
        for (int i = 0; i < savedJobInfos.size(); i++) {
            SavedJobInfo savedJobInfo = savedJobInfos.get(i);
            // only one
            if (i == 0) {
                envs.putAll(generateEnvs("REMOTE_", savedJobInfo));
            }
            // prefix with job id
            String prefix = new StringBuilder("REMOTE_")
                    .append(StringUtils.isNotEmpty(savedJobInfo.getUid()) ? savedJobInfo.uid : savedJobInfo.remoteJobName)
                    .append("_")
                    .toString();
            envs.putAll(generateEnvs(prefix, savedJobInfo));
        }
        return envs;
    }

    /**
     * do api request
     *
     * @param job     Jenkins job
     * @param jobInfo remote Job info
     * @param apiUrl  api url
     * @return api result
     */
    private static SourceMap requestRemoteApi(Item job, RemoteJobInfo jobInfo, String apiUrl)
            throws IOException, UnSuccessfulRequestStatusException {
        // OkHttp Client
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

        RemoteJenkinsServer remoteServer = RemoteJenkinsServerUtils
                .getRemoteJenkinsServer(jobInfo.getRemoteServer());

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
        String url = new StringBuilder(remoteServer.getUrl())
                .append(remoteServer.getUrl().endsWith("/") ? "" : "/")
                .append(apiUrl)
                .toString();
        Request request = requestBuilder.url(url).get().build();

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
                throw new UnSuccessfulRequestStatusException("Response UnSuccess Code:" + response.code() + ",Url:" + url, response.code(), url);
            }
        }
        return null;
    }

    /**
     * get saved job info
     *
     * @param job     Jenkins job
     * @param jobInfo remote Job info
     * @return saved job info
     */
    private static SavedJobInfo getSavedJobInfo(Item job, RemoteJobInfo jobInfo) throws IOException {
        List<SavedJobInfo> savedJobInfos = getSavedJobInfos(job);
        return savedJobInfos.stream().filter(
                savedJobInfo -> savedJobInfo.getRemoteServer().equals(jobInfo.getRemoteServer())
                        && savedJobInfo.getRemoteJob().equals(jobInfo.getId())
        ).findAny().orElse(null);
    }

    /**
     * get saved job infos
     *
     * @param job Jenkins job
     * @return saved job infos
     */
    private static List<SavedJobInfo> getSavedJobInfos(Item job) throws IOException {
        File file = getRemoteResultConfigFile(job);
        if (file.exists()) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            CollectionType collectionType = TypeFactory.defaultInstance().constructCollectionType(List.class, SavedJobInfo.class);
            return mapper.readValue(file, collectionType);
        }
        return new ArrayList<>();
    }

    /**
     * get remote result config file
     *
     * @param job Jenkins job
     * @return config file
     */
    private static File getRemoteResultConfigFile(Item job) {
        return new File(job.getRootDir().getAbsolutePath() + "/remote-build-result.json");
    }

    /**
     * Generate envs
     *
     * @param prefix       prefix
     * @param savedJobInfo saved info
     * @return envs
     */
    private static Map<String, String> generateEnvs(String prefix, SavedJobInfo savedJobInfo) {
        Map<String, String> envs = new HashMap<>();
        SourceMap sourceMap = SourceMap.of(savedJobInfo.result);
        // BUILD_NUMBER
        envs.put(prefix + "BUILD_NUMBER", sourceMap.stringValue("number"));
        // TIMESTAMP
        envs.put(prefix + "BUILD_TIMESTAMP", sourceMap.stringValue("timestamp"));
        // BUILD_URL
        envs.put(prefix + "BUILD_URL", sourceMap.stringValue("url"));
        // BUILD_RESULT
        envs.put(prefix + "BUILD_RESULT", sourceMap.stringValue("result"));

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

        // result json
        Map<String, Object> resultJson = savedJobInfo.getResultJson();
        if (resultJson != null) {
            SourceMap map = SourceMap.of(resultJson);
            for (String key : resultJson.keySet()) {
                envs.put(prefix + "RESULT_" + key, map.stringValue(key));
            }
        }
        return envs;
    }


    /**
     * Info
     */
    public static class SavedJobInfo {
        private String remoteServer;
        private String remoteJob;
        private String remoteJobName;
        private String uid;
        private Integer triggerNumber;
        private Map<String, Object> result;
        private Map<String, Object> resultJson;

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

        public String getUid() {
            return uid;
        }

        public void setUid(String uid) {
            this.uid = uid;
        }

        public Integer getTriggerNumber() {
            return triggerNumber;
        }

        public void setTriggerNumber(Integer triggerNumber) {
            this.triggerNumber = triggerNumber;
        }

        public Map<String, Object> getResult() {
            return result;
        }

        public void setResult(Map<String, Object> result) {
            this.result = result;
        }

        public Map<String, Object> getResultJson() {
            return resultJson;
        }

        public void setResultJson(Map<String, Object> resultJson) {
            this.resultJson = resultJson;
        }
    }
}
