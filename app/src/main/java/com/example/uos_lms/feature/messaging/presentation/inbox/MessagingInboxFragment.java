package com.example.uos_lms.feature.messaging.presentation.inbox;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uos_lms.R;
import com.example.uos_lms.core.common.DateKeyUtils;
import com.example.uos_lms.core.domain.model.Conversation;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.snackbar.Snackbar;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MessagingInboxFragment extends Fragment {

    private MessagingInboxViewModel viewModel;

    public MessagingInboxFragment() {
        super(R.layout.fragment_messaging_inbox);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_messaging_inbox, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MessagingInboxViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.messaging_inbox_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_conversations_message);
        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<Conversation> adapter = new SimpleListAdapter<>(R.layout.item_conversation_card, (itemView, conversation, position) -> {
            String myUid = viewModel.getUiState().getValue().getMyUid();
            ((TextView) itemView.findViewById(R.id.textName)).setText(conversation.otherParticipantName(myUid));
            String otherRole = conversation.otherParticipantRole(myUid);
            ((TextView) itemView.findViewById(R.id.textRole)).setText(otherRole);
            com.example.uos_lms.core.ui.AccentColors.applyBar(itemView.findViewById(R.id.accentBar),
                    com.example.uos_lms.core.ui.AccentColors.colorForRole(com.example.uos_lms.core.domain.model.UserRole.fromStringOrNull(otherRole)));
            TextView textLastMessage = itemView.findViewById(R.id.textLastMessage);
            textLastMessage.setText(conversation.getLastMessageText() != null
                    ? conversation.getLastMessageText() : getString(R.string.no_messages_yet));

            int unread = conversation.unreadCountFor(myUid);
            TextView textBadge = itemView.findViewById(R.id.textUnreadBadge);
            if (unread > 0) {
                textBadge.setText(String.valueOf(unread));
                textBadge.setVisibility(View.VISIBLE);
            } else {
                textBadge.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString("conversationId", conversation.getId());
                args.putString("otherName", conversation.otherParticipantName(myUid));
                NavHostFragment.findNavController(this).navigate(R.id.messageThreadFragment, args);
            });
        });
        recyclerList.setAdapter(adapter);

        view.findViewById(R.id.fabNewConversation).setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.newConversationFragment));

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            view.findViewById(R.id.progressLoading).setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);

            boolean hasConversations = !state.getSortedConversations().isEmpty();
            emptyState.setVisibility(!state.isLoading() && !hasConversations ? View.VISIBLE : View.GONE);
            recyclerList.setVisibility(hasConversations ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getSortedConversations());

            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
