package com.example.uos_lms.core.ui;

import android.content.Context;

import com.example.uos_lms.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Java equivalent of the Compose ConfirmDialog - a destructive-action confirmation prompt
 * reused across every delete/remove action in the app. */
public final class ConfirmDialogHelper {

    private ConfirmDialogHelper() {
    }

    public static void show(Context context, String title, String message, String confirmLabel, Runnable onConfirm) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(confirmLabel, (dialog, which) -> onConfirm.run())
                .setNegativeButton(R.string.cancel_button, null)
                .show();
    }
}
