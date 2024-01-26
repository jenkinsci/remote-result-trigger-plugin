package io.jenkins.plugins.remote.result.trigger;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.RootAction;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.WebMethod;
import org.kohsuke.stapler.json.JsonHttpResponse;
import org.kohsuke.stapler.verb.GET;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author heweisc@dingtalk.com
 */
@Extension
public class RemoteResultJsonApi implements RootAction {
    /**
     * Since there is no HTML/Jelly rendering, no icons are needed.
     */
    @Override
    public String getIconFileName() {
        return null;
    }

    /**
     * Since there is no HTML/Jelly rendering, no display name is needed.
     */
    @Override
    public String getDisplayName() {
        return null;
    }

    /**
     * getUrlName() is the root of the JSON API. Each WebMethod in the class is prefixed by this.
     */
    @Override
    public String getUrlName() {
        return "remote-result";
    }

    @GET
    @WebMethod(name = "result.json")
    public JsonHttpResponse getResult(@QueryParameter String buildUrl) {
        // 提取路径
        Pattern pattern = Pattern.compile("(/job/.*?/)+\\d+/$");
        Matcher matcher = pattern.matcher(buildUrl);

        // 构建返回结果
        if (matcher.find()) {
            FilePath root = new FilePath(Jenkins.get().getRootDir());
            // 路径处理
            String[] split = StringUtils.split(matcher.group(), "/");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < split.length; i++) {
                String str = split[i];
                if (i % 2 == 0 && "job".equals(str)) {
                    sb.append("jobs/");
                } else if (i == split.length - 1) {
                    sb.append("builds/").append(str);
                } else {
                    sb.append(str).append("/");
                }
            }
            sb.append("/remote-result-trigger.json");

            FilePath jsonFile = root.child(sb.toString());
            try {
                if (jsonFile.exists()) {
                    String jsonStr = jsonFile.readToString();
                    return new JsonHttpResponse(JSONObject.fromObject(jsonStr), 200);
                }
            } catch (Exception e) {
                // nothing to do
            }
        }
        return new JsonHttpResponse(JSONObject.fromObject("{}"), 200);
    }
}
