package com.example.uos_lms.feature.auth.presentation.status;

import com.example.uos_lms.R;

public class RejectedFragment extends StatusMessageFragment {

    @Override
    protected int iconRes() {
        return R.drawable.ic_cancel;
    }

    @Override
    protected int titleRes() {
        return R.string.rejected_title;
    }

    @Override
    protected int messageRes() {
        return R.string.rejected_message;
    }
}
