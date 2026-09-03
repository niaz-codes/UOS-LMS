package com.example.uos_lms.feature.auth.presentation.status;

import com.example.uos_lms.R;

public class PendingApprovalFragment extends StatusMessageFragment {

    @Override
    protected int iconRes() {
        return R.drawable.ic_hourglass_empty;
    }

    @Override
    protected int titleRes() {
        return R.string.pending_title;
    }

    @Override
    protected int messageRes() {
        return R.string.pending_message;
    }
}
