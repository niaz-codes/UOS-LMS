package com.example.uos_lms.feature.messaging.presentation.thread;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiMediaDataSource;
import com.example.uos_lms.core.data.remote.api.ApiMessagingDataSource;
import com.example.uos_lms.core.data.remote.api.dto.MediaResponseDto;
import com.example.uos_lms.core.domain.model.Conversation;
import com.example.uos_lms.core.domain.model.Message;
import com.example.uos_lms.core.session.CachedSession;
import com.example.uos_lms.core.session.SessionManager;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/** No real-time layer (locked-in migration decision) - polls listMessages() with a `since`
 * cursor every POLL_INTERVAL_MS while this ViewModel is alive, instead of a Firestore listener. */
@HiltViewModel
public class MessageThreadViewModel extends ViewModel {

    private static final long POLL_INTERVAL_MS = 4000L;
    private static final String MEDIA_CATEGORY = "message_attachment";
    // Every Nth poll re-fetches the whole conversation (no `since` cursor) instead of just new
    // messages - the incremental since-filtered poll only ever discovers messages that didn't
    // exist before, so it can't pick up a delivered/read status change on a message that was
    // already fetched (e.g. the other participant opening the thread and marking it read some
    // time after we first loaded it). This bridges that gap without abandoning the incremental
    // poll's lower payload cost the rest of the time.
    private static final int FULL_RESYNC_EVERY_N_POLLS = 5;

    private final ApiMessagingDataSource messagingDataSource;
    private final ApiMediaDataSource mediaDataSource;
    private final String conversationId;
    private final String myUid;

    private final MutableLiveData<MessageThreadUiState> uiState = new MutableLiveData<>(MessageThreadUiState.initial());
    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private final Set<String> seenMessageIds = new HashSet<>();
    private long lastMessageMillis;
    private int pollCount;
    private final Runnable pollRunnable = this::poll;

    @Inject
    public MessageThreadViewModel(
            SavedStateHandle savedStateHandle,
            ApiMessagingDataSource messagingDataSource,
            ApiMediaDataSource mediaDataSource,
            SessionManager sessionManager) {
        this.messagingDataSource = messagingDataSource;
        this.mediaDataSource = mediaDataSource;
        this.conversationId = savedStateHandle.get("conversationId");

        CachedSession session = sessionManager.getCachedSession().getValue();
        this.myUid = session != null ? session.getUid() : null;

        if (myUid == null || conversationId == null) {
            uiState.setValue(uiState.getValue().toBuilder().loading(false).build());
            return;
        }
        uiState.setValue(uiState.getValue().toBuilder().myUid(myUid).build());
        load();
    }

    private void load() {
        messagingDataSource.listConversations().addOnSuccessListener(conversations -> {
            Conversation conversation = null;
            for (Conversation candidate : conversations) {
                if (candidate.getId().equals(conversationId)) {
                    conversation = candidate;
                    break;
                }
            }
            if (conversation == null) {
                uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage("Conversation not found.").build());
                return;
            }
            uiState.setValue(uiState.getValue().toBuilder().conversation(conversation).build());
            if (conversation.unreadCountFor(myUid) > 0) {
                messagingDataSource.markRead(conversationId);
            }
            loadInitialMessages();
        }).addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
    }

    private void loadInitialMessages() {
        messagingDataSource.listMessages(conversationId, null)
                .addOnSuccessListener(messages -> {
                    List<Message> enriched = enrich(messages);
                    for (Message message : enriched) {
                        seenMessageIds.add(message.getId());
                        lastMessageMillis = Math.max(lastMessageMillis, message.getSentAt());
                    }
                    uiState.setValue(uiState.getValue().toBuilder().messages(enriched).loading(false).build());
                    pollHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
    }

    private void poll() {
        pollCount++;
        boolean fullResync = pollCount % FULL_RESYNC_EVERY_N_POLLS == 0;
        Long since = fullResync ? null : (lastMessageMillis > 0 ? lastMessageMillis : null);

        messagingDataSource.listMessages(conversationId, since)
                .addOnSuccessListener(fetchedMessages -> {
                    List<Message> enriched = enrich(fetchedMessages);
                    boolean changed = fullResync;
                    List<Message> merged = fullResync ? new ArrayList<>() : new ArrayList<>(uiState.getValue().getMessages());
                    if (fullResync) seenMessageIds.clear();

                    for (Message message : enriched) {
                        if (fullResync || seenMessageIds.add(message.getId())) {
                            seenMessageIds.add(message.getId());
                            merged.add(message);
                            lastMessageMillis = Math.max(lastMessageMillis, message.getSentAt());
                            changed = true;
                        }
                    }
                    if (changed) uiState.setValue(uiState.getValue().toBuilder().messages(merged).build());
                })
                .addOnCompleteListener(task -> pollHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS));
    }

    /** senderName/senderRole aren't populated server-side on Message (see MessageResponseDto) -
     * fill them in from the two participants of the already-loaded Conversation instead. */
    private List<Message> enrich(List<Message> messages) {
        Conversation conversation = uiState.getValue().getConversation();
        if (conversation == null) return messages;
        List<Message> result = new ArrayList<>();
        for (Message message : messages) {
            boolean fromA = conversation.getParticipantAUid().equals(message.getSenderUid());
            result.add(message.toBuilder()
                    .senderName(fromA ? conversation.getParticipantAName() : conversation.getParticipantBName())
                    .senderRole(fromA ? conversation.getParticipantARole() : conversation.getParticipantBRole())
                    .build());
        }
        return result;
    }

    public LiveData<MessageThreadUiState> getUiState() {
        return uiState;
    }

    public void sendMessage(String text, @Nullable Uri attachmentUri) {
        MessageThreadUiState state = uiState.getValue();
        if (state.getConversation() == null) return;

        uiState.setValue(uiState.getValue().toBuilder().sending(true).errorMessage(null).build());

        Task<MediaResponseDto> uploadTask = attachmentUri != null
                ? mediaDataSource.upload(MEDIA_CATEGORY, "conversation", conversationId, attachmentUri, null)
                : Tasks.forResult(null);

        uploadTask.continueWithTask(uploadResultTask -> {
            MediaResponseDto media = uploadResultTask.getResult();
            String mediaId = media != null ? media.getId() : null;
            return messagingDataSource.sendMessage(conversationId, text, mediaId);
        }).addOnSuccessListener(message -> {
                    List<Message> enriched = enrich(java.util.Collections.singletonList(message));
                    Message sent = enriched.get(0);
                    if (seenMessageIds.add(sent.getId())) {
                        List<Message> merged = new ArrayList<>(uiState.getValue().getMessages());
                        merged.add(sent);
                        lastMessageMillis = Math.max(lastMessageMillis, sent.getSentAt());
                        uiState.setValue(uiState.getValue().toBuilder().sending(false).messages(merged).build());
                    } else {
                        uiState.setValue(uiState.getValue().toBuilder().sending(false).build());
                    }
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder()
                        .sending(false).errorMessage(e.getMessage()).build()));
    }

    public void consumeError() {
        uiState.setValue(uiState.getValue().toBuilder().errorMessage(null).build());
    }

    @Override
    protected void onCleared() {
        pollHandler.removeCallbacks(pollRunnable);
    }
}
