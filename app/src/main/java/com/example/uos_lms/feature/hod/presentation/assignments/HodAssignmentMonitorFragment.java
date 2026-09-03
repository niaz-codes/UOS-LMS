package com.example.uos_lms.feature.hod.presentation.assignments;

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
import com.example.uos_lms.core.domain.model.Assignment;
import com.example.uos_lms.core.domain.model.Subject;
import com.example.uos_lms.core.ui.SimpleListAdapter;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HodAssignmentMonitorFragment extends Fragment {

    private HodAssignmentMonitorViewModel viewModel;

    public HodAssignmentMonitorFragment() {
        super(R.layout.fragment_toolbar_recycler_hod);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_toolbar_recycler_hod, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HodAssignmentMonitorViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.assignment_monitor_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_assignments_in_department);

        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<Assignment> adapter = new SimpleListAdapter<>(R.layout.item_assignment_monitor, (itemView, assignment, position) -> {
            HodAssignmentMonitorUiState state = viewModel.getUiState().getValue();
            Subject subject = state != null ? state.getSubjectsById().get(assignment.getSubjectId()) : null;

            ((TextView) itemView.findViewById(R.id.textTitle)).setText(assignment.getTitle());
            ((TextView) itemView.findViewById(R.id.textSubject)).setText(subject != null
                    ? getString(R.string.subject_code_title_format, subject.getCode(), subject.getTitle())
                    : getString(R.string.subject_removed));
            ((TextView) itemView.findViewById(R.id.textMeta)).setText(getString(R.string.due_marks_format,
                    DateKeyUtils.millisToDisplay(assignment.getDueDateMillis()), assignment.getMaxMarks()));
            com.example.uos_lms.core.ui.AccentColors.applyBar(itemView.findViewById(R.id.accentBar), R.color.status_info);
            itemView.findViewById(R.id.buttonDelete).setVisibility(View.GONE);

            itemView.setOnClickListener(v -> {
                Bundle extras = new Bundle();
                extras.putString("assignmentId", assignment.getId());
                extras.putString("title", assignment.getTitle());
                extras.putInt("maxMarks", assignment.getMaxMarks());
                NavHostFragment.findNavController(this).navigate(R.id.assignmentSubmissionsFragment, extras);
            });
        });
        recyclerList.setAdapter(adapter);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            boolean hasAssignments = !state.getAssignments().isEmpty();
            emptyState.setVisibility(hasAssignments || state.isLoading() ? View.GONE : View.VISIBLE);
            recyclerList.setVisibility(hasAssignments ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getAssignments());
        });
    }
}
