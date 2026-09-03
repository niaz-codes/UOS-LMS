package com.example.uos_lms.feature.messaging.presentation.compose;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiMessagingDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUserDataSource;
import com.example.uos_lms.core.domain.model.User;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/** Contact list is whatever the backend's messaging-contacts endpoint returns - the app-wide
 * role-pair policy (Student<->Teacher, Teacher<->HOD, HOD<->Admin, same department) is now
 * enforced entirely server-side (see userController.contacts / messagingPolicy.js), so there's
 * nothing left to filter client-side. */
@HiltViewModel
public class NewConversationViewModel extends ViewModel {

    private final ApiMessagingDataSource messagingDataSource;

    private final MutableLiveData<NewConversationUiState> uiState = new MutableLiveData<>(NewConversationUiState.initial());
    private final MutableLiveData<String> conversationCreated = new MutableLiveData<>();

    @Inject
    public NewConversationViewModel(ApiMessagingDataSource messagingDataSource, ApiUserDataSource userDataSource) {
        this.messagingDataSource = messagingDataSource;
        userDataSource.messagingContacts()
                .addOnSuccessListener(contacts -> uiState.setValue(uiState.getValue().toBuilder().contacts(contacts).loading(false).build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
    }

    public LiveData<NewConversationUiState> getUiState() {
        return uiState;
    }

    public LiveData<String> getConversationCreated() {
        return conversationCreated;
    }

    public void selectContact(User contact) {
        uiState.setValue(uiState.getValue().toBuilder().creating(true).errorMessage(null).build());
        messagingDataSource.findOrCreateConversation(contact.getUid())
                .addOnSuccessListener(conversation -> {
                    uiState.setValue(uiState.getValue().toBuilder().creating(false).build());
                    conversationCreated.setValue(conversation.getId());
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder()
                        .creating(false).errorMessage(e.getMessage()).build()));
    }
}
