package com.example.uos_lms.core.export;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Writes exported report bytes to the app's cache dir and shares them as a real file via
 * FileProvider - replaces the previous fake export (CSV text pasted into a share-sheet body
 * with no actual file). */
public final class ShareFileHelper {

    private ShareFileHelper() {
    }

    public static void shareCsv(Context context, String fileName, String csvContent, String chooserTitle) {
        shareBytes(context, fileName, csvContent.getBytes(), "text/csv", chooserTitle);
    }

    public static void sharePdf(Context context, String fileName, byte[] pdfBytes, String chooserTitle) {
        shareBytes(context, fileName, pdfBytes, "application/pdf", chooserTitle);
    }

    private static void shareBytes(Context context, String fileName, byte[] bytes, String mimeType, String chooserTitle) {
        try {
            File exportsDir = new File(context.getCacheDir(), "exports");
            if (!exportsDir.exists() && !exportsDir.mkdirs()) {
                throw new IOException("Could not create the exports directory.");
            }
            File file = new File(exportsDir, fileName);
            try (FileOutputStream out = new FileOutputStream(file)) {
                out.write(bytes);
            }

            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType(mimeType);
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(shareIntent, chooserTitle));
        } catch (IOException e) {
            throw new IllegalStateException("Could not create the export file.", e);
        }
    }
}
