package com.example.uos_lms.core.common;

/**
 * Wraps a value together with the state of the operation that produced it, so a single
 * LiveData can carry loading/success/error without a separate boolean+message pair.
 * Standard Android "Resource" pattern (Google's Guide to App Architecture Java sample).
 */
public class Resource<T> {

    public enum Status { SUCCESS, ERROR, LOADING }

    private final Status status;
    private final T data;
    private final String message;
    private final Throwable cause;

    private Resource(Status status, T data, String message, Throwable cause) {
        this.status = status;
        this.data = data;
        this.message = message;
        this.cause = cause;
    }

    public static <T> Resource<T> success(T data) {
        return new Resource<>(Status.SUCCESS, data, null, null);
    }

    public static <T> Resource<T> error(String message) {
        return new Resource<>(Status.ERROR, null, message, null);
    }

    public static <T> Resource<T> error(String message, Throwable cause) {
        return new Resource<>(Status.ERROR, null, message, cause);
    }

    public static <T> Resource<T> loading() {
        return new Resource<>(Status.LOADING, null, null, null);
    }

    public Status getStatus() {
        return status;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getCause() {
        return cause;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isError() {
        return status == Status.ERROR;
    }

    public boolean isLoading() {
        return status == Status.LOADING;
    }
}
