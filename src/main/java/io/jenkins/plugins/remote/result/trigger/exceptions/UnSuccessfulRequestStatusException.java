package io.jenkins.plugins.remote.result.trigger.exceptions;

/**
 * jenkins request status is not 200
 *
 * @author HW
 */
public class UnSuccessfulRequestStatusException extends Exception {
    private static final long serialVersionUID = 8420230702251100687L;
    private final int status;
    private final String url;

    /**
     * Constructs a new exception with {@code null} as its detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     */
    public UnSuccessfulRequestStatusException(String message, int status, String url) {
        super(message);
        this.status = status;
        this.url = url;
    }

    public int getStatus() {
        return status;
    }

    public String getUrl() {
        return url;
    }
}
