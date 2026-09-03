package com.example.uos_lms.core.data.remote.api;

import android.content.Context;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import androidx.annotation.Nullable;

import com.example.uos_lms.core.data.remote.ProgressListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okio.BufferedSink;

/** Converts a content Uri into a Retrofit multipart part for the backend's file-upload
 * endpoints (Media API). Streams the file straight from the ContentResolver to the network
 * request body rather than buffering the whole thing into a byte[] first - a multi-megapixel
 * camera photo (or any large document picked via the app's wildcard attachment pickers) previously
 * had to fit entirely in the heap as a byte[], with no size guard and only an IOException catch
 * around it, so a large file threw an uncaught OutOfMemoryError straight through and crashed the
 * app. Memory usage here is bounded to one small chunk buffer regardless of file size. */
public final class MultipartFileUtils {

    private static final int CHUNK_SIZE = 8192;
    // Generous client-side guard so a wildly oversized pick (e.g. a video from a "*/*" file
    // picker) fails fast with a clear message instead of spending minutes uploading something
    // the backend would reject anyway (see mediaController.js's own per-category size limits).
    private static final long MAX_UPLOAD_BYTES = 25L * 1024 * 1024;

    private MultipartFileUtils() {
    }

    public static MultipartBody.Part filePart(Context context, Uri uri, String partName, @Nullable ProgressListener onProgress) throws IOException {
        Context appContext = context.getApplicationContext();

        long size = querySize(appContext, uri);
        if (size > MAX_UPLOAD_BYTES) {
            throw new IOException("That file is too large to upload (max " + (MAX_UPLOAD_BYTES / (1024 * 1024)) + " MB).");
        }
        // Fail fast if the Uri can't actually be opened, rather than only discovering that once
        // OkHttp tries to write the request body on its own dispatcher thread.
        try (InputStream probe = appContext.getContentResolver().openInputStream(uri)) {
            if (probe == null) throw new IOException("Could not read the selected file.");
        }

        String mime = mimeType(appContext, uri);
        String fileName = resolveFileName(appContext, uri);
        MediaType mediaType = mime != null ? MediaType.parse(mime) : null;
        RequestBody body = new StreamingRequestBody(appContext, uri, mediaType, size, onProgress);
        return MultipartBody.Part.createFormData(partName, fileName, body);
    }

    /** -1 (unknown length -> chunked transfer encoding) if the provider doesn't expose a size
     * column, rather than failing the whole upload over a missing progress hint. */
    private static long querySize(Context context, Uri uri) {
        try (android.database.Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIndex >= 0 && cursor.moveToFirst() && !cursor.isNull(sizeIndex)) {
                    return cursor.getLong(sizeIndex);
                }
            }
        } catch (Exception ignored) {
            // Fall through to "unknown".
        }
        return -1;
    }

    @Nullable
    private static String mimeType(Context context, Uri uri) {
        String type = context.getContentResolver().getType(uri);
        if (type != null) return type;
        String path = uri.getPath();
        if (path == null) return null;
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex < 0) return null;
        String extension = path.substring(dotIndex + 1).toLowerCase(Locale.US);
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
    }

    private static String resolveFileName(Context context, Uri uri) {
        if ("content".equals(uri.getScheme())) {
            try (android.database.Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0 && cursor.moveToFirst()) {
                        String name = cursor.getString(nameIndex);
                        if (name != null) return name;
                    }
                }
            } catch (Exception ignored) {
                // Fall through to the last-path-segment fallback below.
            }
        }
        String lastSegment = uri.getLastPathSegment();
        return lastSegment != null ? lastSegment : "file";
    }

    /** Reads directly from a fresh ContentResolver InputStream into the network sink, one chunk
     * at a time - never holds more than CHUNK_SIZE bytes of the file in memory. Re-opens the
     * stream on every call rather than caching one, since OkHttp can call writeTo() more than
     * once (redirects/retries) and a content Uri's InputStream isn't re-readable after close. */
    private static class StreamingRequestBody extends RequestBody {
        private final Context context;
        private final Uri uri;
        private final MediaType mediaType;
        private final long size;
        private final ProgressListener onProgress;

        StreamingRequestBody(Context context, Uri uri, @Nullable MediaType mediaType, long size, @Nullable ProgressListener onProgress) {
            this.context = context;
            this.uri = uri;
            this.mediaType = mediaType;
            this.size = size;
            this.onProgress = onProgress;
        }

        @Nullable
        @Override
        public MediaType contentType() {
            return mediaType;
        }

        @Override
        public long contentLength() {
            return size;
        }

        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            try (InputStream input = context.getContentResolver().openInputStream(uri)) {
                if (input == null) throw new IOException("Could not read the selected file.");
                byte[] buffer = new byte[CHUNK_SIZE];
                long written = 0;
                int read;
                while ((read = input.read(buffer)) != -1) {
                    sink.write(buffer, 0, read);
                    written += read;
                    if (onProgress != null && size > 0) {
                        onProgress.onProgress((int) ((written * 100) / size));
                    }
                }
            }
        }
    }
}
