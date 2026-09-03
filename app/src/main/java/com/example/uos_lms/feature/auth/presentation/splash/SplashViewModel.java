package com.example.uos_lms.feature.auth.presentation.splash;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.core.domain.model.UserStatus;
import com.example.uos_lms.feature.auth.data.AuthDataSource;
import com.example.uos_lms.feature.auth.presentation.AuthDestination;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class SplashViewModel extends ViewModel {

    /** Sticky (not a SingleLiveEvent) on purpose: the destination must survive being consumed
     * once. Navigation from the splash can be silently dropped when the FragmentManager has
     * already saved its state (screen locked/app backgrounded mid-splash), and the fragment
     * re-observes on resume/config-change - so the resolved value has to be re-delivered. */
    private final MutableLiveData<AuthDestination> destination = new MutableLiveData<>();

    @Inject
    public SplashViewModel(AuthDataSource authDataSource) {
        authDataSource.getCurrentUserProfile().addOnCompleteListener(task -> {
            if (destination.getValue() != null) return;
            if (!task.isSuccessful()) {
                destination.setValue(AuthDestination.login());
                return;
            }
            User user = task.getResult();
            if (user == null) {
                destination.setValue(AuthDestination.login());
            } else if (user.getStatus() == UserStatus.PENDING) {
                destination.setValue(AuthDestination.pending());
            } else if (user.getStatus() == UserStatus.REJECTED) {
                destination.setValue(AuthDestination.rejected());
            } else if (user.getStatus() == UserStatus.SUSPENDED) {
                destination.setValue(AuthDestination.suspended());
            } else {
                destination.setValue(AuthDestination.dashboard(user.getRole()));
            }
        });
    }

    public LiveData<AuthDestination> getDestination() {
        return destination;
    }
}
