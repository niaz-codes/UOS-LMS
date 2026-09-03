package com.example.uos_lms.core.ui;

import android.content.Context;

import com.example.uos_lms.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/** Java equivalent of the Compose MultiSelectDialog - a checkbox-list picker, used for the
 * Teacher role's multi-department assignment. */
public final class MultiSelectDialogHelper {

    private MultiSelectDialogHelper() {
    }

    public static <T> void show(
            Context context,
            String title,
            List<T> options,
            Function<T, String> labelMapper,
            Function<T, String> keyMapper,
            Set<String> initiallySelected,
            String emptyMessage,
            Consumer<List<String>> onConfirm) {
        if (options.isEmpty()) {
            new MaterialAlertDialogBuilder(context)
                    .setTitle(title)
                    .setMessage(emptyMessage)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }
        CharSequence[] labels = new CharSequence[options.size()];
        boolean[] checked = new boolean[options.size()];
        List<String> selectedIds = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            labels[i] = labelMapper.apply(options.get(i));
            String key = keyMapper.apply(options.get(i));
            checked[i] = initiallySelected.contains(key);
            if (checked[i]) selectedIds.add(key);
        }
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> {
                    String key = keyMapper.apply(options.get(which));
                    if (isChecked) {
                        if (!selectedIds.contains(key)) selectedIds.add(key);
                    } else {
                        selectedIds.remove(key);
                    }
                })
                .setPositiveButton(R.string.save, (dialog, which) -> onConfirm.accept(selectedIds))
                .setNegativeButton(R.string.cancel_button, null)
                .show();
    }
}
