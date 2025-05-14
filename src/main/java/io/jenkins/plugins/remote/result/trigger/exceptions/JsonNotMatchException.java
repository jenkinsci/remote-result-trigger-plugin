package io.jenkins.plugins.remote.result.trigger.exceptions;

import java.io.IOException;
import java.io.Serial;

/**
 * @author heweisc@dingtalk.com
 */
public class JsonNotMatchException extends IOException {
    @Serial
    private static final long serialVersionUID = -6097935366992936039L;

    /**
     * Constructs an {@code IOException} with the specified detail message.
     *
     * @param message The detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method)
     */
    public JsonNotMatchException(String message) {
        super(message);
    }

    /**
     * Constructs an {@code IOException} with the specified detail message
     * and cause.
     *
     * <p> Note that the detail message associated with {@code cause} is
     * <i>not</i> automatically incorporated into this exception's detail
     * message.
     *
     * @param message The detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method)
     * @param cause   The cause (which is saved for later retrieval by the
     *                {@link #getCause()} method).  (A null value is permitted,
     *                and indicates that the cause is nonexistent or unknown.)
     * @since 1.6
     */
    public JsonNotMatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
