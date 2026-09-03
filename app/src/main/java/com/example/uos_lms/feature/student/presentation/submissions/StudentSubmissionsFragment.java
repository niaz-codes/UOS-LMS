package com.example.uos_lms.feature.student.presentation.submissions;

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
import com.example.uos_lms.core.domain.model.AssignmentSubmission;
import com.example.uos_lms.core.ui.AccentColors;
import com.example.uos_lms.core.ui.RefreshUx;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class StudentSubmissionsFragment extends Fragment {

    private StudentSubmissionsViewModel viewModel;
    private boolean resumedOnce;

    public StudentSubmissionsFragment() {
        super(R.layout.fragment_student_submissions);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student_submissions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(StudentSubmissionsViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.submissions_screen_title);
        toolbar.findViewById(R.id.buttonBack).setVisibility(View.GONE);

        SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.swipeRefresh);
        RefreshUx.Binding refreshBinding = RefreshUx.bindToolbarIcon(
                toolbar.findViewById(R.id.toolbarActionSlot), swipeRefresh, () -> viewModel.refresh());

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_assignments_posted_message);
        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<SubmissionRow> adapter = new SimpleListAdapter<>(R.layout.item_student_submission_row, (itemView, row, position) -> {
            ((TextView) itemView.findViewById(R.id.textTitle)).setText(row.getAssignment().getTitle());
            ((TextView) itemView.findViewById(R.id.textMeta)).setText(getString(R.string.due_marks_format,
                    DateKeyUtils.millisToDisplay(row.getAssignment().getDueDateMillis()), row.getAssignment().getMaxMarks()));

            AssignmentSubmission submission = row.getSubmission();
            TextView textStatus = itemView.findViewById(R.id.textStatus);
            int accentColorRes;
            if (submission != null && submission.isGraded()) {
                AccentColors.applyPill(textStatus, R.color.status_success,
                        getString(R.string.graded_format, submission.getMarksObtained(), row.getAssignment().getMaxMarks()));
                accentColorRes = R.color.status_success;
            } else if (submission != null) {
                AccentColors.applyPill(textStatus, R.color.status_info,
                        getString(R.string.submitted_format, DateKeyUtils.millisToDisplay(submission.getSubmittedAt())));
                accentColorRes = R.color.status_info;
            } else {
                AccentColors.applyPill(textStatus, R.color.error_color, getString(R.string.not_submitted));
                accentColorRes = R.color.error_color;
            }
            AccentColors.applyBar(itemView.findViewById(R.id.accentBar), accentColorRes);

            itemView.setOnClickListener(v -> {
                Bundle extras = new Bundle();
                extras.putString("assignmentId", row.getAssignment().getId());
                extras.putString("subjectId", row.getAssignment().getSubjectId());
                extras.putInt("maxMarks", row.getAssignment().getMaxMarks());
                NavHostFragment.findNavController(this).navigate(R.id.submitAssignmentFragment, extras);
            });
        });
        recyclerList.setAdapter(adapter);

        BottomNavigationView bottomNav = view.findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.navSubmissions);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navSubmissions) return true;
            if (id == R.id.navHome) NavHostFragment.findNavController(this).popBackStack(R.id.studentDashboardFragment, false);
            else if (id == R.id.navAttendance) NavHostFragment.findNavController(this).navigate(R.id.studentAttendanceFragment);
            else if (id == R.id.navResult) NavHostFragment.findNavController(this).navigate(R.id.studentResultsFragment);
            else if (id == R.id.navProfile) NavHostFragment.findNavController(this).navigate(R.id.profileFragment);
            return false;
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            boolean hasRows = !state.getRows().isEmpty();
            emptyState.setVisibility(hasRows || state.isLoading() ? View.GONE : View.VISIBLE);
            recyclerList.setVisibility(hasRows ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getRows());
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
