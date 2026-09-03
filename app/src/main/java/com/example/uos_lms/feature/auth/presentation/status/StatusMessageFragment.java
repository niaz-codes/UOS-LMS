package com.example.uos_lms.feature.auth.presentation.status;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.uos_lms.R;
import com.example.uos_lms.feature.auth.presentation.AuthNavigator;

/** Shared shell for the 3 post-login "your account isn't usable yet" screens - only the
 * icon/title/message differ per status, so the layout and back-to-login wiring live once here. */
public abstract class StatusMessageFragment extends Fragment {

    protected StatusMessageFragment() {
        super(R.layout.fragment_status_message);
    }

    @DrawableRes
    protected abstract int iconRes();

    @StringRes
    protected abstract int titleRes();

    @StringRes
    protected abstract int messageRes();

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((ImageView) view.findViewById(R.id.imageIcon)).setImageResource(iconRes());
        ((TextView) view.findViewById(R.id.textTitle)).setText(titleRes());
        ((TextView) view.findViewById(R.id.textMessage)).setText(messageRes());

        view.findViewById(R.id.buttonBackToLogin).setOnClickListener(v ->
                AuthNavigator.navigateToLoginClearingStack(NavHostFragment.findNavController(this)));
    }
}
