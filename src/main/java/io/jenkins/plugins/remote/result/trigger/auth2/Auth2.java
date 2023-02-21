package io.jenkins.plugins.remote.result.trigger.auth2;

import hudson.DescriptorExtensionList;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import io.jenkins.plugins.remote.result.trigger.exceptions.CredentialsNotFoundException;
import jenkins.model.Jenkins;

import java.io.Serializable;

public abstract class Auth2 extends AbstractDescribableImpl<Auth2> implements Serializable, Cloneable {

    private static final long serialVersionUID = -3217381962636283564L;

    private static final DescriptorExtensionList<Auth2, Auth2Descriptor> ALL = DescriptorExtensionList
            .createDescriptorList(Jenkins.getInstanceOrNull(), Auth2.class);

    public static DescriptorExtensionList<Auth2, Auth2Descriptor> all() {
        return ALL;
    }

    public static abstract class Auth2Descriptor extends Descriptor<Auth2> {
    }

    /**
     * Get JenkinsClient Credentials Or ApiToken
     *
     * @param item
     * @return
     */
    public abstract String getCredentials(Item item) throws CredentialsNotFoundException;

    public abstract String toString();

    /**
     * Returns a string representing the authorization.
     *
     * @param item the Item (Job, Pipeline,...) we are currently running in.
     *             The item is required to also get Credentials which are defined in the items scope and not Jenkins globally.
     *             Value can be null, but Credentials e.g. configured on a Folder will not be found in this case,
     *             only globally configured Credentials.
     * @return a string representing the authorization.
     */
    public abstract String toString(Item item);


    @Override
    public Auth2 clone() throws CloneNotSupportedException {
        return (Auth2) super.clone();
    }

}
