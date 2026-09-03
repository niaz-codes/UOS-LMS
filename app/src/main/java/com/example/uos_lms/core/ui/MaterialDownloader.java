package com.example.uos_lms.core.ui;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;

/** Enqueues a study-material file with the system DownloadManager so it lands in the device's
 * shared Downloads folder with its own progress notification - a real, persisted local copy,
 * distinct from just opening the file in a viewer (see StudentSubjectMaterialsFragment, which
 * offers both as separate actions). Callers on API &lt; 29 must hold WRITE_EXTERNAL_STORAGE
 * first (exempt on 29+ under scoped storage) - see AndroidManifest.xml. */
public final class MaterialDownloader {

    private MaterialDownloader() {
    }

    /** Returns false (instead of crashing) if the enqueue itself fails - a free-form title can
     * contain path-hostile characters once it's used as a fallback filename, and some devices
     * refuse public-Downloads writes outright. */
    public static boolean download(Context context, String fileUrl, String fileName, String title) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(fileUrl));
            request.setTitle(title);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            String destinationName = fileName != null && !fileName.isBlank() ? fileName : title;
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, sanitizeFileName(destinationName));
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);

            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager == null) return false;
            downloadManager.enqueue(request);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static String sanitizeFileName(String name) {
        return name.replaceAll("[/\\\\:*?\"<>|]", "_");
    }
}
