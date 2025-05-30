package io.jenkins.plugins.remote.result.trigger.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import hudson.model.BuildableItem;
import hudson.model.Item;
import io.jenkins.plugins.remote.result.trigger.RemoteJenkinsServer;
import io.jenkins.plugins.remote.result.trigger.RemoteJobInfo;
import io.jenkins.plugins.remote.result.trigger.exceptions.UnSuccessfulRequestStatusException;
import io.jenkins.plugins.remote.result.trigger.model.JobResultInfo;
import io.jenkins.plugins.remote.result.trigger.utils.ssl.SSLSocketManager;
import okhttp3.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.lang.NonNull;

import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Remote Result Result Cache
 *
 * @author HW
 */
public class RemoteJobResultUtils {

    /**
     * get remote job last build number
     *
     * @param job     Jenkins job
     * @param jobInfo remote Job info
     * @return 版本
     */
    public static Integer requestLastBuildBuildNumber(Item job, RemoteJobInfo jobInfo)
            throws UnSuccessfulRequestStatusException, IOException {
        String api = jobInfo.getRemoteJobUrl() + "/lastBuild/buildNumber";
        String result = requestRemoteApi(job, jobInfo, api);
        if (result != null) {
            return Integer.valueOf(result);
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
        String api = jobInfo.getRemoteJobUrl() + "/" + number + "/api/json";
        return requestRemoteJsonApi(job, jobInfo, api);
    }

    /**
     * 请求任务信息
     *
     * @param job     Jenkins job
     * @param jobInfo remote Job info
     * @return job info
     */
    public static SourceMap requestJobInfo(Item job, RemoteJobInfo jobInfo)
            throws UnSuccessfulRequestStatusException, IOException {
        String api = jobInfo.getRemoteJobUrl() + "/api/json";
        return requestRemoteJsonApi(job, jobInfo, api);
    }

    /**
     * last checked build number
     *
     * @param job     Jenkins job
     * @param jobInfo remote Job info
     * @return last trigger number
     */
    public static int getCheckedNumber(Item job, RemoteJobInfo jobInfo) throws IOException {
        JobResultInfo jobResultInfo = getSavedJobInfo(job, jobInfo);
        // 兼容老版本
        if (jobResultInfo != null && jobResultInfo.getCheckedNumber() != null) {
            return jobResultInfo.getCheckedNumber();
        }
        return 0;
    }

    /**
     * save build checked number
     *
     * @param job     Jenkins job
     * @param jobInfo remote Job info
     * @param number  checked number
     */
    public static void saveCheckedNumber(BuildableItem job, RemoteJobInfo jobInfo, int number) throws IOException {
        JobResultInfo jobResultInfo = getSavedJobInfo(job, jobInfo);
        if (jobResultInfo == null) {
            jobResultInfo = new JobResultInfo();
        }

        jobResultInfo.setCheckedNumber(number);

        saveBuildResultInfo(job, jobInfo, jobResultInfo);
    }

    /**
     * save build trigger number
     *
     * @param job     Jenkins job
     * @param jobInfo remote Job info
     * @param number  trigger number
     */
    public static void saveTriggeredNumber(BuildableItem job, RemoteJobInfo jobInfo, int number) throws IOException {
        JobResultInfo jobResultInfo = getSavedJobInfo(job, jobInfo);
        if (jobResultInfo == null) {
            jobResultInfo = new JobResultInfo();
        }

        jobResultInfo.setTriggeredNumber(number);

        saveBuildResultInfo(job, jobInfo, jobResultInfo);
    }

    /**
     * save build result json
     *
     * @param job          Jenkins job
     * @param jobInfo      remote Job info
     * @param remoteResult result json
     */
    public static void saveRemoteResultInfo(BuildableItem job, RemoteJobInfo jobInfo, SourceMap remoteResult) throws IOException {
        JobResultInfo jobResultInfo = getSavedJobInfo(job, jobInfo);
        if (jobResultInfo == null) {
            jobResultInfo = new JobResultInfo();
        }

        jobResultInfo.setRemoteResult(remoteResult.getSource());

        saveBuildResultInfo(job, jobInfo, jobResultInfo);
    }

    /**
     * save build info
     *
     * @param job         Jenkins job
     * @param jobInfo     remote Job info
     * @param buildResult api result
     */
    public static void saveBuildResultInfo(BuildableItem job, RemoteJobInfo jobInfo, SourceMap buildResult) throws IOException {
        JobResultInfo jobResultInfo = getSavedJobInfo(job, jobInfo);
        if (jobResultInfo == null) {
            jobResultInfo = new JobResultInfo();
        }

        jobResultInfo.setBuildResult(buildResult.getSource());

        saveBuildResultInfo(job, jobInfo, jobResultInfo);
    }

    /**
     * clean
     *
     * @param job            Jenkins job
     * @param remoteJobInfos remote Job infos
     */
    @NonNull
    public static void cleanUnusedBuildInfo(BuildableItem job, List<RemoteJobInfo> remoteJobInfos) {
        try {
            if (remoteJobInfos != null) {
                List<JobResultInfo> jobResultInfos = getSavedJobInfos(job);
                jobResultInfos.removeIf(savedJobInfo -> remoteJobInfos.stream().noneMatch(
                        remoteJobInfo -> remoteJobInfo.getId().equals(savedJobInfo.getRemoteJob())
                ));
                // save to file
                File file = getRemoteResultConfigFile(job);
                if (!file.getParentFile().exists()) {
                    FileUtils.forceMkdirParent(file);
                }
                String string = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(jobResultInfos);
                FileUtils.writeStringToFile(file, string, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            // do nothing
        }
    }

    /**
     * save build info to file
     *
     * @param job           Jenkins job
     * @param jobInfo       remote Job info
     * @param jobResultInfo save info
     */
    private static void saveBuildResultInfo(BuildableItem job, RemoteJobInfo jobInfo, JobResultInfo jobResultInfo) throws IOException {
        // remote job info
        jobResultInfo.setRemoteServer(jobInfo.getRemoteServer());
        jobResultInfo.setRemoteJob(jobInfo.getId());
        jobResultInfo.setRemoteJobUrl(jobInfo.getRemoteJobUrl());
        jobResultInfo.setUid(jobInfo.getUid());

        // get saved list
        List<JobResultInfo> jobResultInfos = getSavedJobInfos(job);
        // remove old
        jobResultInfos.removeIf(
                info -> info.getRemoteServer().equals(jobResultInfo.getRemoteServer())
                        && info.getRemoteJob().equals(jobResultInfo.getRemoteJob())
        );
        jobResultInfos.add(jobResultInfo);
        // save to file
        File file = getRemoteResultConfigFile(job);
        if (!file.getParentFile().exists()) {
            FileUtils.forceMkdirParent(file);
        }
        String string = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(jobResultInfos);
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
        List<JobResultInfo> jobResultInfos = getSavedJobInfos(job);
        for (int i = 0; i < jobResultInfos.size(); i++) {
            JobResultInfo jobResultInfo = jobResultInfos.get(i);
            // only one
            if (i == 0) {
                envs.putAll(generateEnvs("REMOTE_", jobResultInfo));
            }
            // prefix with job id
            String prefix = "REMOTE_" + jobResultInfo.getUid() + "_";
            envs.putAll(generateEnvs(prefix, jobResultInfo));
        }
        // jobs list
        List<String> jobs = jobResultInfos.stream()
                .map(info -> StringUtils.isNotBlank(info.getUid()) ? info.getUid() : info.getRemoteJobUrl())
                .collect(Collectors.toUnmodifiableList());
        envs.put("REMOTE_JOBS", new ObjectMapper().writeValueAsString(jobs));
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
    private static String requestRemoteApi(Item job, RemoteJobInfo jobInfo, String apiUrl)
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
            clientBuilder
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
        Request request = requestBuilder.url(apiUrl).get().build();

        Call call = okHttpClient.newCall(request);
        try (Response response = call.execute()) {
            if (response.isSuccessful()) {
                ResponseBody responseBody = response.body();
                if (null != responseBody) {
                    return responseBody.string();
                }
            } else {
                throw new UnSuccessfulRequestStatusException("Response UnSuccess Code:" + response.code() + ",Url:" + apiUrl, response.code(), apiUrl);
            }
        }
        return null;
    }

    /**
     * do api request
     *
     * @param job     Jenkins job
     * @param jobInfo remote Job info
     * @param apiUrl  api url
     * @return api result
     */
    private static SourceMap requestRemoteJsonApi(Item job, RemoteJobInfo jobInfo, String apiUrl)
            throws IOException, UnSuccessfulRequestStatusException {
        String body = requestRemoteApi(job, jobInfo, apiUrl);
        if (body != null) {
            // json
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            //noinspection unchecked
            return SourceMap.of(mapper.readValue(body, Map.class));
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
    private static JobResultInfo getSavedJobInfo(Item job, RemoteJobInfo jobInfo) throws IOException {
        List<JobResultInfo> jobResultInfos = getSavedJobInfos(job);
        return jobResultInfos.stream().filter(
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
    public static List<JobResultInfo> getSavedJobInfos(Item job) throws IOException {
        File file = getRemoteResultConfigFile(job);
        if (file.exists()) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            CollectionType collectionType = TypeFactory.defaultInstance().constructCollectionType(List.class, JobResultInfo.class);
            return mapper.readValue(file, collectionType);
        }
        return new ArrayList<>();
    }

    /**
     * 清理缓存
     */
    public static void cleanCache(BuildableItem job) throws IOException {
        File file = getRemoteResultConfigFile(job);
        if (file.exists()) {
            FileUtils.delete(file);
        }
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
     * @param prefix        prefix
     * @param jobResultInfo saved info
     * @return envs
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Map<String, String> generateEnvs(String prefix, JobResultInfo jobResultInfo) throws JsonProcessingException {
        Map<String, String> envs = new HashMap<>();
        if (jobResultInfo.getBuildResult() != null) {
            SourceMap sourceMap = SourceMap.of(jobResultInfo.getBuildResult());
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
                                    String key = prefix + "PARAMETER_" + parameterMap.stringValue("name");
                                    envs.put(key, parameterMap.stringValue("value"));
                                }
                            }
                        }
                    }
                }
            }

            // result json
            Map<String, Object> resultJson = jobResultInfo.getRemoteResult();
            if (resultJson != null) {
                ObjectMapper mapper = new ObjectMapper();
                SourceMap map = SourceMap.of(resultJson);
                for (String key : resultJson.keySet()) {
                    Object object = map.getSource().get(key);
                    if (object instanceof String) {
                        envs.put(prefix + "RESULT_" + key, map.stringValue(key));
                    } else if (object instanceof Collection<?> || object instanceof Map<?, ?>) {
                        envs.put(prefix + "RESULT_" + key, mapper.writeValueAsString(object));
                    } else {
                        envs.put(prefix + "RESULT_" + key, object.toString());
                    }
                }
            }
        }
        return envs;
    }
}
