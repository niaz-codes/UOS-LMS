package com.example.uos_lms.feature.messaging.presentation.thread;

import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uos_lms.R;
import com.example.uos_lms.core.common.DateKeyUtils;
import com.example.uos_lms.core.domain.model.Message;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MessageThreadFragment extends Fragment {

    private MessageThreadViewModel viewModel;
    private ActivityResultLauncher<String> filePicker;
    private Uri pendingAttachmentUri;

    public MessageThreadFragment() {
        super(R.layout.fragment_message_thread);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        filePicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                pendingAttachmentUri = uri;
                Snackbar.make(requireView(), R.string.attach_file_selected, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_message_thread, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MessageThreadViewModel.class);

        Bundle args = getArguments();
        String otherName = args != null ? args.getString("otherName") : null;

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(otherName != null ? otherName : getString(R.string.messaging_inbox_title));
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_messages_yet);
        RecyclerView recyclerMessages = view.findViewById(R.id.recyclerMessages);
        recyclerMessages.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<Message> adapter = new SimpleListAdapter<>(R.layout.item_message_bubble, (itemView, message, position) -> {
            String myUid = viewModel.getUiState().getValue().getMyUid();
            boolean mine = message.getSenderUid().equals(myUid);

            View bubbleRow = itemView.findViewById(R.id.bubbleRow);
            ((android.widget.LinearLayout) bubbleRow).setGravity(mine ? Gravity.END : Gravity.START);

            MaterialCardView bubbleCard = itemView.findViewById(R.id.bubbleCard);
            int backgroundColorRes = mine ? R.color.indigo_primary_container : R.color.surface_variant_color;
            int contentColorRes = mine ? R.color.on_indigo_primary_container : R.color.on_surface_variant_color;
            bubbleCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), backgroundColorRes));

            TextView textSenderName = itemView.findViewById(R.id.textSenderName);
            textSenderName.setVisibility(mine ? View.GONE : View.VISIBLE);
            textSenderName.setText(message.getSenderName());
            textSenderName.setTextColor(ContextCompat.getColor(requireContext(), contentColorRes));

            TextView textMessageText = itemView.findViewById(R.id.textMessageText);
            textMessageText.setText(message.getText());
            textMessageText.setTextColor(ContextCompat.getColor(requireContext(), contentColorRes));

            TextView textAttachment = itemView.findViewById(R.id.textAttachment);
            if (message.getAttachmentUrl() != null) {
                textAttachment.setText(R.string.attach_file_selected);
                textAttachment.setTextColor(ContextCompat.getColor(requireContext(), contentColorRes));
                textAttachment.setVisibility(View.VISIBLE);
                textAttachment.setOnClickListener(v -> startActivity(new android.content.Intent(
                        android.content.Intent.ACTION_VIEW, Uri.parse(message.getAttachmentUrl()))));
            } else {
                textAttachment.setVisibility(View.GONE);
            }

            TextView textTime = itemView.findViewById(R.id.textTime);
            textTime.setText(DateKeyUtils.millisToDisplay(message.getSentAt()));
            textTime.setTextColor(ContextCompat.getColor(requireContext(), contentColorRes));

            android.widget.ImageView imageDeliveryStatus = itemView.findViewById(R.id.imageDeliveryStatus);
            if (mine) {
                imageDeliveryStatus.setVisibility(View.VISIBLE);
                if (message.getReadAt() != null) {
                    imageDeliveryStatus.setImageResource(R.drawable.ic_done_all);
                    imageDeliveryStatus.setColorFilter(com.google.android.material.color.MaterialColors.getColor(
                            itemView, com.google.android.material.R.attr.colorPrimary));
                } else if (message.getDeliveredAt() != null) {
                    imageDeliveryStatus.setImageResource(R.drawable.ic_done_all);
                    imageDeliveryStatus.setColorFilter(ContextCompat.getColor(requireContext(), contentColorRes));
                } else {
                    imageDeliveryStatus.setImageResource(R.drawable.ic_check_single);
                    imageDeliveryStatus.setColorFilter(ContextCompat.getColor(requireContext(), contentColorRes));
                }
            } else {
                imageDeliveryStatus.setVisibility(View.GONE);
            }
        });
        recyclerMessages.setAdapter(adapter);

        TextInputEditText editMessage = view.findViewById(R.id.editMessage);
        view.findViewById(R.id.buttonAttach).setOnClickListener(v -> filePicker.launch("*/*"));
        view.findViewById(R.id.buttonSend).setOnClickListener(v -> {
            String text = editMessage.getText() == null ? "" : editMessage.getText().toString().trim();
            if (text.isEmpty() && pendingAttachmentUri == null) return;
            viewModel.sendMessage(text, pendingAttachmentUri);
            editMessage.setText("");
            pendingAttachmentUri = null;
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            boolean hasMessages = !state.getSortedMessages().isEmpty();
            emptyState.setVisibility(!state.isLoading() && !hasMessages ? View.VISIBLE : View.GONE);
            recyclerMessages.setVisibility(hasMessages ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getSortedMessages());
            if (hasMessages) recyclerMessages.scrollToPosition(state.getSortedMessages().size() - 1);

            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
                viewModel.consumeError();
            }
        });
    }
}
