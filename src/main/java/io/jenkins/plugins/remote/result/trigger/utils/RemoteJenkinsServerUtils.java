package io.jenkins.plugins.remote.result.trigger.utils;

import io.jenkins.plugins.remote.result.trigger.RemoteBuildResultTrigger;
import io.jenkins.plugins.remote.result.trigger.RemoteJenkinsServer;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;

/**
 * Global Jenkins Server Tool
 *
 * @author HW
 */
public class RemoteJenkinsServerUtils {

    /**
     * get fill server names
     *
     * @return server names
     */
    public static RemoteJenkinsServer[] getRemoteServers() {
        RemoteBuildResultTrigger.RemoteBuildResultTriggerDescriptor trigger =
                (RemoteBuildResultTrigger.RemoteBuildResultTriggerDescriptor) Jenkins.get()
                        .getDescriptorOrDie(RemoteBuildResultTrigger.class);

        return trigger.getRemoteJenkinsServers();
    }

    /**
     * get RemoteJenkinsServer
     *
     * @param id remote server uid
     * @return remote server
     */
    public static RemoteJenkinsServer getRemoteJenkinsServer(String id) {
        RemoteBuildResultTrigger.RemoteBuildResultTriggerDescriptor trigger =
                (RemoteBuildResultTrigger.RemoteBuildResultTriggerDescriptor) Jenkins.get()
                        .getDescriptorOrDie(RemoteBuildResultTrigger.class);

        RemoteJenkinsServer[] remoteJenkinsServers = trigger.getRemoteJenkinsServers();
        for (RemoteJenkinsServer server : remoteJenkinsServers) {
            if (StringUtils.equals(server.getId(), id)) {
                return server;
            }
        }
        return null;
    }
}
