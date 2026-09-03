package com.example.uos_lms.feature.messaging.presentation.compose;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uos_lms.R;
import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.snackbar.Snackbar;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class NewConversationFragment extends Fragment {

    private NewConversationViewModel viewModel;

    public NewConversationFragment() {
        super(R.layout.fragment_new_conversation);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_conversation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(NewConversationViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.new_conversation_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_contacts_available_message);
        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<User> adapter = new SimpleListAdapter<>(R.layout.item_contact_card, (itemView, contact, position) -> {
            ((TextView) itemView.findViewById(R.id.textName)).setText(contact.getFullName());
            ((TextView) itemView.findViewById(R.id.textRole)).setText(contact.getRole().name());
            com.example.uos_lms.core.ui.AccentColors.applyBar(itemView.findViewById(R.id.accentBar),
                    com.example.uos_lms.core.ui.AccentColors.colorForRole(contact.getRole()));
            itemView.setOnClickListener(v -> viewModel.selectContact(contact));
        });
        recyclerList.setAdapter(adapter);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            view.findViewById(R.id.progressLoading).setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);

            boolean hasContacts = !state.getContacts().isEmpty();
            emptyState.setVisibility(!state.isLoading() && !hasContacts ? View.VISIBLE : View.GONE);
            recyclerList.setVisibility(hasContacts ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getContacts());

            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
            }
        });

        viewModel.getConversationCreated().observe(getViewLifecycleOwner(), conversationId -> {
            if (conversationId == null) return;
            Bundle args = new Bundle();
            args.putString("conversationId", conversationId);
            NavOptions navOptions = new NavOptions.Builder()
                    .setPopUpTo(R.id.newConversationFragment, true)
                    .build();
            NavHostFragment.findNavController(this).navigate(R.id.messageThreadFragment, args, navOptions);
        });
    }
}
