package com.example.uos_lms.core.data.remote.api;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.uos_lms.core.data.remote.ProgressListener;
import com.example.uos_lms.core.data.remote.api.dto.MediaResponseDto;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/** REST-backed replacement for CloudinaryDataSource's upload flows - proxies through the
 * backend's Media API so the Cloudinary API secret never touches the app (see mediaController
 * on the backend). Used as each remaining Firestore feature (Assignments, Materials, Leave,
 * Messaging) migrates off the old direct-to-Cloudinary client. */
@Singleton
public class ApiMediaDataSource {

    private static final String TAG = "ApiMediaDataSource";

    private final Context context;
    private final MediaApi mediaApi;

    @Inject
    public ApiMediaDataSource(@ApplicationContext Context context, MediaApi mediaApi) {
        this.context = context;
        this.mediaApi = mediaApi;
    }

    public Task<MediaResponseDto> upload(String category, @Nullable String relatedType, @Nullable String relatedId,
                                          Uri uri, @Nullable ProgressListener onProgress) {
        MultipartBody.Part filePart;
        try {
            filePart = MultipartFileUtils.filePart(context, uri, "file", onProgress);
        } catch (IOException e) {
            Log.w(TAG, "Failed to prepare " + category + " upload for " + uri, e);
            TaskCompletionSource<MediaResponseDto> source = new TaskCompletionSource<>();
            source.setException(e);
            return source.getTask();
        }
        RequestBody categoryBody = text(category);
        RequestBody relatedTypeBody = relatedType != null ? text(relatedType) : null;
        RequestBody relatedIdBody = relatedId != null ? text(relatedId) : null;
        return RetrofitTasks.call(mediaApi.upload(categoryBody, relatedTypeBody, relatedIdBody, filePart))
                .onSuccessTask(envelope -> Tasks.forResult(envelope.getMedia()));
    }

    /** Best-effort - a missing/already-deleted asset is not treated as an error. */
    public Task<Void> delete(String mediaId) {
        return RetrofitTasks.call(mediaApi.delete(mediaId)).continueWith(task -> null);
    }

    private static RequestBody text(String value) {
        return RequestBody.create(MediaType.parse("text/plain"), value);
    }
}
