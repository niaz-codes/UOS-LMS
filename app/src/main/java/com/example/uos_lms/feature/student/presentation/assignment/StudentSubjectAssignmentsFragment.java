package com.example.uos_lms.feature.student.presentation.assignment;

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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.uos_lms.R;
import com.example.uos_lms.core.common.DateKeyUtils;
import com.example.uos_lms.core.domain.model.Assignment;
import com.example.uos_lms.core.ui.RefreshUx;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.snackbar.Snackbar;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class StudentSubjectAssignmentsFragment extends Fragment {

    private StudentSubjectAssignmentsViewModel viewModel;
    private boolean resumedOnce;

    public StudentSubjectAssignmentsFragment() {
        super(R.layout.fragment_student_subject_assignments);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student_subject_assignments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(StudentSubjectAssignmentsViewModel.class);

        Bundle args = requireArguments();
        String subjectId = args.getString("subjectId");

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.assignments_screen_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.swipeRefresh);
        RefreshUx.Binding refreshBinding = RefreshUx.bindToolbarIcon(
                toolbar.findViewById(R.id.toolbarActionSlot), swipeRefresh, () -> viewModel.refresh());

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_assignments_this_subject_message);
        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<Assignment> adapter = new SimpleListAdapter<>(R.layout.item_assignment_monitor, (itemView, assignment, position) -> {
            ((TextView) itemView.findViewById(R.id.textTitle)).setText(assignment.getTitle());
            itemView.findViewById(R.id.textSubject).setVisibility(View.GONE);
            ((TextView) itemView.findViewById(R.id.textMeta)).setText(getString(R.string.due_marks_format,
                    DateKeyUtils.millisToDisplay(assignment.getDueDateMillis()), assignment.getMaxMarks()));
            com.example.uos_lms.core.ui.AccentColors.applyBar(itemView.findViewById(R.id.accentBar), R.color.status_info);
            itemView.findViewById(R.id.buttonDelete).setVisibility(View.GONE);

            itemView.setOnClickListener(v -> {
                Bundle extras = new Bundle();
                extras.putString("assignmentId", assignment.getId());
                extras.putString("subjectId", subjectId);
                extras.putInt("maxMarks", assignment.getMaxMarks());
                NavHostFragment.findNavController(this).navigate(R.id.submitAssignmentFragment, extras);
            });
        });
        recyclerList.setAdapter(adapter);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            boolean hasAssignments = !state.getAssignments().isEmpty();
            emptyState.setVisibility(hasAssignments || state.isLoading() ? View.GONE : View.VISIBLE);
            recyclerList.setVisibility(hasAssignments ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getAssignments());
            refreshBinding.setRefreshing(state.isRefreshing());
            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (resumedOnce) viewModel.refresh();
        resumedOnce = true;
    }
}
