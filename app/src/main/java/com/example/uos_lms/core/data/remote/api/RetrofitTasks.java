package com.example.uos_lms.core.data.remote.api;

import com.example.uos_lms.core.data.remote.api.dto.ApiErrorDto;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Bridges Retrofit's callback-based {@link Call} to the {@link Task}-based idiom the rest
 * of this Java codebase already uses everywhere (a carryover from Firebase's Task-returning
 * SDKs) — so REST-backed data sources plug into existing ViewModels without changing their
 * addOnCompleteListener/addOnSuccessListener call sites. */
public final class RetrofitTasks {

    private static final Gson GSON = new Gson();

    private RetrofitTasks() {
    }

    public static <T> Task<T> call(Call<T> call) {
        TaskCompletionSource<T> completionSource = new TaskCompletionSource<>();
        call.enqueue(new Callback<T>() {
            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                if (response.isSuccessful()) {
                    completionSource.setResult(response.body());
                } else {
                    completionSource.setException(toApiException(response));
                }
            }

            @Override
            public void onFailure(Call<T> call, Throwable t) {
                completionSource.setException(t instanceof IOException
                        ? new ApiException(0, "You're offline. Please check your internet connection and try again.")
                        : new Exception(t));
            }
        });
        return completionSource.getTask();
    }

    private static ApiException toApiException(Response<?> response) {
        ResponseBody errorBody = response.errorBody();
        if (errorBody != null) {
            try {
                ApiErrorDto parsed = GSON.fromJson(errorBody.charStream(), ApiErrorDto.class);
                if (parsed != null && parsed.getError() != null && parsed.getError().getMessage() != null) {
                    return new ApiException(response.code(), parsed.getError().getMessage(), parsed.getError().getAccountStatus());
                }
            } catch (Exception ignored) {
                // fall through to the generic message below
            }
        }
        return new ApiException(response.code(), defaultMessageFor(response.code()));
    }

    private static String defaultMessageFor(int code) {
        switch (code) {
            case 401:
                return "Invalid email or password.";
            case 403:
                return "You are not authorized to do that.";
            case 404:
                return "Not found.";
            default:
                return code >= 500
                        ? "Server error. Please try again later."
                        : "Something went wrong. Please try again.";
        }
    }
}
