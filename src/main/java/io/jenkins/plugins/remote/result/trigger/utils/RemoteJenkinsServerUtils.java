package io.jenkins.plugins.remote.result.trigger.utils;

import io.jenkins.plugins.remote.result.trigger.RemoteBuildResultTrigger;
import io.jenkins.plugins.remote.result.trigger.RemoteJenkinsServer;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Global Jenkins Server Tool
 *
 * @author root
 * @date 2023/02/20 17:03
 */
public class RemoteJenkinsServerUtils {

    /**
     * get fill server names
     *
     * @return
     */
    public static List<String> getRemoteServerNames() {
        RemoteBuildResultTrigger.RemoteBuildResultTriggerDescriptor trigger =
                (RemoteBuildResultTrigger.RemoteBuildResultTriggerDescriptor) Jenkins.get()
                        .getDescriptorOrDie(RemoteBuildResultTrigger.class);

        List<String> names = new ArrayList<>();
        RemoteJenkinsServer[] remoteJenkinsServers = trigger.getRemoteJenkinsServers();
        for (RemoteJenkinsServer server : remoteJenkinsServers) {
            if (StringUtils.isNotEmpty(server.getDisplayName())) {
                names.add(server.getDisplayName());
            } else {
                names.add(server.getUrl());
            }
        }
        return names;
    }

    /**
     * get RemoteJenkinsServer
     *
     * @param name
     * @return
     */
    public static RemoteJenkinsServer getRemoteJenkinsServerByName(String name) {
        RemoteBuildResultTrigger.RemoteBuildResultTriggerDescriptor trigger =
                (RemoteBuildResultTrigger.RemoteBuildResultTriggerDescriptor) Jenkins.get()
                        .getDescriptorOrDie(RemoteBuildResultTrigger.class);

        RemoteJenkinsServer[] remoteJenkinsServers = trigger.getRemoteJenkinsServers();
        for (RemoteJenkinsServer server : remoteJenkinsServers) {
            if (StringUtils.isNotEmpty(server.getDisplayName()) && StringUtils.equals(server.getDisplayName(), name)) {
                return server;
            } else if (StringUtils.equals(server.getUrl(), name)) {
                return server;
            }
        }
        return null;
    }

    /**
     * get config display names
     *
     * @return
     */
    public static List<String> getRemoteServerDisplayNames() {
        RemoteBuildResultTrigger.RemoteBuildResultTriggerDescriptor trigger =
                (RemoteBuildResultTrigger.RemoteBuildResultTriggerDescriptor) Jenkins.get()
                        .getDescriptorOrDie(RemoteBuildResultTrigger.class);

        List<String> names = new ArrayList<>();
        RemoteJenkinsServer[] remoteJenkinsServers = trigger.getRemoteJenkinsServers();
        for (RemoteJenkinsServer server : remoteJenkinsServers) {
            if (StringUtils.isNotEmpty(server.getDisplayName())) {
                names.add(server.getDisplayName());
            }
        }
        return names;
    }
}
