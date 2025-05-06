package io.jenkins.plugins.remote.result.trigger.exceptions;

import java.io.IOException;
import java.io.Serial;

/**
 * @author HW
 */
public class CredentialsNotFoundException extends IOException {

    @Serial
    private static final long serialVersionUID = -2489306184948013529L;
    private final String credentialsId;

    public CredentialsNotFoundException(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    @Override
    public String getMessage() {
        return "No Jenkins Credentials found with ID '" + credentialsId + "'";
    }

}
