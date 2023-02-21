package io.jenkins.plugins.remote.result.trigger.exceptions;

/**
 * jenkins request status is not 200
 *
 * @author HW
 * @date 2023/02/20 19:58
 */
public class JenkinsRemoteUnSuccessRequestStatusException extends Exception{
    private int status;
    /**
     * Constructs a new exception with the specified detail message.  The
     * cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public JenkinsRemoteUnSuccessRequestStatusException(String message, int status) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
