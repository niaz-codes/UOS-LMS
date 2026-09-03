package com.example.uos_lms.core.ui;

import android.content.Context;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Java equivalent of the Compose SelectDialog - a single-choice picker backed by a plain
 * list, reused across every "choose a department / semester / role" prompt. */
public final class SelectDialogHelper {

    private SelectDialogHelper() {
    }

    public static <T> void show(
            Context context,
            String title,
            List<T> options,
            Function<T, String> labelMapper,
            String emptyMessage,
            Consumer<T> onSelect) {
        if (options.isEmpty()) {
            new MaterialAlertDialogBuilder(context)
                    .setTitle(title)
                    .setMessage(emptyMessage)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }
        CharSequence[] labels = new CharSequence[options.size()];
        for (int i = 0; i < options.size(); i++) {
            labels[i] = labelMapper.apply(options.get(i));
        }
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setItems(labels, (dialog, which) -> onSelect.accept(options.get(which)))
                .show();
    }
}
