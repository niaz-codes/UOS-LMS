package com.example.uos_lms.feature.auth.presentation.status;

import com.example.uos_lms.R;

public class SuspendedFragment extends StatusMessageFragment {

    @Override
    protected int iconRes() {
        return R.drawable.ic_block;
    }

    @Override
    protected int titleRes() {
        return R.string.suspended_title;
    }

    @Override
    protected int messageRes() {
        return R.string.suspended_message;
    }
}
