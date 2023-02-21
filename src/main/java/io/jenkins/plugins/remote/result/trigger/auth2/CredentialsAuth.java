package io.jenkins.plugins.remote.result.trigger.auth2;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Item;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.remote.result.trigger.exceptions.CredentialsNotFoundException;
import jenkins.model.Jenkins;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

public class CredentialsAuth extends Auth2 {

    private static final long serialVersionUID = -2650007108928532552L;

    @Extension
    public static final Auth2Descriptor DESCRIPTOR = new CredentialsAuthDescriptor();

    private String credentialsId;

    @DataBoundConstructor
    public CredentialsAuth() {
        this.credentialsId = null;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    /**
     * Tries to find the Jenkins Credential and returns the user name.
     *
     * @param item the Item (Job, Pipeline,...) we are currently running in.
     *             The item is required to also get Credentials which are defined in the items scope and not Jenkins globally.
     *             Value can be null, but Credentials e.g. configured on a Folder will not be found in this case, only globally configured Credentials.
     * @return The user name configured in this Credential
     * @throws CredentialsNotFoundException if credential could not be found.
     */
    public String getUserName(Item item) throws CredentialsNotFoundException {
        UsernamePasswordCredentials creds = _getCredentials(item);
        return creds.getUsername();
    }

    /**
     * Tries to find the Jenkins Credential and returns the password.
     *
     * @param item the Item (Job, Pipeline,...) we are currently running in.
     *             The item is required to also get Credentials which are defined in the items scope and not Jenkins globally.
     *             Value can be null, but Credentials e.g. configured on a Folder will not be found in this case, only globally configured Credentials.
     * @return The password configured in this Credential
     * @throws CredentialsNotFoundException if credential could not be found.
     */
    public String getPassword(Item item) throws CredentialsNotFoundException {
        UsernamePasswordCredentials creds = _getCredentials(item);
        return creds.getPassword().getPlainText();
    }

    /**
     * Get JenkinsClient Credentials
     *
     * @param item item
     * @return Credentials
     */
    @Override
    public String getCredentials(Item item) throws CredentialsNotFoundException {
        if (StringUtils.isNotEmpty(this.credentialsId)) {
            String username = getUserName(item);
            String password = getPassword(item);
            return "Basic " + Base64.encodeBase64String((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        }
        return null;
    }

    @Override
    public String toString() {
        return toString(null);
    }

    @Override
    public String toString(Item item) {
        try {
            String userName = getUserName(item);
            return String.format("'%s' as user '%s' (Credentials ID '%s')", getDescriptor().getDisplayName(), userName, credentialsId);
        } catch (CredentialsNotFoundException e) {
            return String.format("'%s'. WARNING! No credentials found with ID '%s'!", getDescriptor().getDisplayName(), credentialsId);
        }
    }

    /**
     * Looks up the credentialsID attached to this object in the Global Credentials plugin datastore
     *
     * @param item the Item (Job, Pipeline,...) we are currently running in.
     *             The item is required to also get Credentials which are defined in the items scope and not Jenkins globally.
     *             Value can be null, but Credentials e.g. configured on a Folder will not be found in this case, only globally configured Credentials.
     * @return the matched credentials
     * @throws CredentialsNotFoundException if not found
     */
    private UsernamePasswordCredentials _getCredentials(Item item) throws CredentialsNotFoundException {
        List<StandardUsernameCredentials> listOfCredentials = CredentialsProvider.lookupCredentials(
                StandardUsernameCredentials.class, item, null, Collections.emptyList());

        return (UsernamePasswordCredentials) _findCredential(credentialsId, listOfCredentials);
    }

    private StandardUsernameCredentials _findCredential(String credentialId, List<StandardUsernameCredentials> listOfCredentials) throws CredentialsNotFoundException {
        for (StandardUsernameCredentials cred : listOfCredentials) {
            if (credentialId.equals(cred.getId())) {
                return cred;
            }
        }
        throw new CredentialsNotFoundException(credentialId);
    }


    @Override
    public Auth2Descriptor getDescriptor() {
        return DESCRIPTOR;
    }

    @Symbol("CredentialsAuth")
    public static class CredentialsAuthDescriptor extends Auth2Descriptor {
        @Override
        @NonNull
        public String getDisplayName() {
            return "Credentials Authentication";
        }

        @POST
        public static ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item item,
                                                            @QueryParameter String credentialsId) {
            StandardUsernameListBoxModel result = new StandardUsernameListBoxModel();

            List<StandardUsernameCredentials> credentials = CredentialsProvider.lookupCredentials(
                    StandardUsernameCredentials.class,
                    item,
                    null,
                    Collections.emptyList()
            );
            // since we only care about 'UsernamePasswordCredentials' objects, lets seek those out and ignore the rest.
            for (StandardUsernameCredentials c : credentials) {
                if (c instanceof UsernamePasswordCredentials) {
                    result.with(c);
                }
            }

            if (item == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return result.includeCurrentValue(credentialsId);
                }
            } else {
                if (!item.hasPermission(Item.EXTENDED_READ)
                        && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return result.includeCurrentValue(credentialsId);
                }
            }
            return result.includeEmptyValue();
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((credentialsId == null) ? 0 : credentialsId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!this.getClass().isInstance(obj))
            return false;
        CredentialsAuth other = (CredentialsAuth) obj;
        if (credentialsId == null) {
            if (other.credentialsId != null) {
                return false;
            }
        } else if (!credentialsId.equals(other.credentialsId)) {
            return false;
        }
        return true;
    }

}
