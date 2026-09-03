package com.example.uos_lms.feature.messaging.presentation.inbox;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiMessagingDataSource;
import com.example.uos_lms.core.session.CachedSession;
import com.example.uos_lms.core.session.SessionManager;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class MessagingInboxViewModel extends ViewModel {

    private final MutableLiveData<MessagingInboxUiState> uiState = new MutableLiveData<>(MessagingInboxUiState.initial());

    @Inject
    public MessagingInboxViewModel(ApiMessagingDataSource messagingDataSource, SessionManager sessionManager) {
        CachedSession session = sessionManager.getCachedSession().getValue();
        if (session == null) {
            uiState.setValue(uiState.getValue().toBuilder().loading(false).build());
            return;
        }
        uiState.setValue(uiState.getValue().toBuilder().myUid(session.getUid()).build());
        messagingDataSource.listConversations()
                .addOnSuccessListener(conversations -> uiState.setValue(uiState.getValue().toBuilder().conversations(conversations).loading(false).build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
    }

    public LiveData<MessagingInboxUiState> getUiState() {
        return uiState;
    }
}
