package io.jenkins.plugins.remote.result.trigger.exceptions;

/**
 * @author heweisc@dingtalk.com
 */
public class RemoteJobInBuildingException extends Exception {
    private static final long serialVersionUID = -4416311109922437403L;

    /**
     * Constructs a new exception with the specified detail message.  The
     * cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public RemoteJobInBuildingException(String message) {
        super(message);
    }
}
