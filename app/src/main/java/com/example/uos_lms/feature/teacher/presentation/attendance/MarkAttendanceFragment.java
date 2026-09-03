package com.example.uos_lms.feature.teacher.presentation.attendance;

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
import com.example.uos_lms.core.domain.model.AttendanceStatus;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MarkAttendanceFragment extends Fragment {

    private MarkAttendanceViewModel viewModel;

    public MarkAttendanceFragment() {
        super(R.layout.fragment_mark_attendance);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mark_attendance, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MarkAttendanceViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_students_enrolled_yet);
        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<RosterRow> adapter = new SimpleListAdapter<>(R.layout.item_roster_row, (itemView, row, position) -> {
            boolean isPresent = row.getStatus() == AttendanceStatus.PRESENT;
            ((TextView) itemView.findViewById(R.id.textName)).setText(row.getStudent().getFullName());

            TextView textStatus = itemView.findViewById(R.id.textStatus);
            int statusColorRes = com.example.uos_lms.core.ui.AttendanceStatusChipHelper.bind(textStatus, row.getStatus());
            com.example.uos_lms.core.ui.AccentColors.applyBar(itemView.findViewById(R.id.accentBar), statusColorRes);

            MaterialSwitch switchPresent = itemView.findViewById(R.id.switchPresent);
            switchPresent.setOnCheckedChangeListener(null);
            switchPresent.setChecked(isPresent);
            switchPresent.setOnCheckedChangeListener((button, checked) -> viewModel.toggleStatus(row.getStudent().getUid()));
        });
        recyclerList.setAdapter(adapter);

        MaterialButton buttonSave = view.findViewById(R.id.buttonSave);
        CircularProgressIndicator progressSaving = view.findViewById(R.id.progressSaving);
        buttonSave.setOnClickListener(v -> viewModel.save());

        viewModel.getSaved().observe(getViewLifecycleOwner(), saved -> {
            if (saved) NavHostFragment.findNavController(this).popBackStack();
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            ((TextView) toolbar.findViewById(R.id.textTitle)).setText(state.getDateLabel());

            boolean hasRows = !state.getRows().isEmpty();
            emptyState.setVisibility(!state.isLoading() && !hasRows ? View.VISIBLE : View.GONE);
            recyclerList.setVisibility(hasRows ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getRows());

            buttonSave.setVisibility(hasRows && !state.isSaving() ? View.VISIBLE : View.GONE);
            progressSaving.setVisibility(state.isSaving() ? View.VISIBLE : View.GONE);
            buttonSave.setEnabled(!state.isSaving());

            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
