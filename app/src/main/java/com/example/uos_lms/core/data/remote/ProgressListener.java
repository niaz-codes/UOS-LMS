package com.example.uos_lms.core.data.remote;

/** Upload progress callback (0-100), shared by every file/image upload path. */
public interface ProgressListener {
    void onProgress(int percent);
}
