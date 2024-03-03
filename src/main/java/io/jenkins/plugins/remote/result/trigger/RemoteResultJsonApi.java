package io.jenkins.plugins.remote.result.trigger;

import hudson.Extension;
import hudson.model.RootAction;
import io.jenkins.plugins.remote.result.trigger.utils.RemoteJobJsonResultUtils;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.WebMethod;
import org.kohsuke.stapler.json.JsonHttpResponse;
import org.kohsuke.stapler.verb.GET;

import java.io.IOException;
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
        // 提取路径和构建编号
        Matcher matcher = Pattern.compile("((/job/(\\S+)/)+\\d+/)$").matcher(buildUrl);
        // 构建返回结果
        if (matcher.find()) {
            try {
                String json = RemoteJobJsonResultUtils.getJson(matcher.group());
                if (json != null) {
                    return new JsonHttpResponse(JSONObject.fromObject(json), 200);
                }
            } catch (IOException | InterruptedException e) {
                // nothing to do here
            }
        }
        return new JsonHttpResponse(JSONObject.fromObject("{}"), 200);
    }
}
