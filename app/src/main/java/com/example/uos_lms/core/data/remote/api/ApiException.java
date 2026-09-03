package com.example.uos_lms.core.data.remote.api;

/** Wraps a non-2xx REST response as an exception carrying the backend's error message,
 * so ViewModels can keep reading {@code task.getException().getMessage()} the same way
 * they already did for Firebase Task failures. */
public class ApiException extends Exception {
    private final int statusCode;
    private final String accountStatus;

    public ApiException(int statusCode, String message) {
        this(statusCode, message, null);
    }

    public ApiException(int statusCode, String message, String accountStatus) {
        super(message);
        this.statusCode = statusCode;
        this.accountStatus = accountStatus;
    }

    public int getStatusCode() {
        return statusCode;
    }

    /** UserStatus.name() (e.g. "PENDING"/"REJECTED"/"SUSPENDED") when the backend rejected
     * a login specifically because the account isn't approved yet; null for every other
     * kind of API error. */
    public String getAccountStatus() {
        return accountStatus;
    }
}
