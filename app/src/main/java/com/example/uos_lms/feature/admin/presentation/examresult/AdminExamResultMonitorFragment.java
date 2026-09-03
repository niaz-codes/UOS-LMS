package com.example.uos_lms.feature.admin.presentation.examresult;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uos_lms.R;
import com.example.uos_lms.core.domain.model.ExamResult;
import com.example.uos_lms.core.domain.model.ResultStatus;
import com.example.uos_lms.core.ui.ResultStatusChipHelper;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminExamResultMonitorFragment extends Fragment {

    private AdminExamResultMonitorViewModel viewModel;

    public AdminExamResultMonitorFragment() {
        super(R.layout.fragment_admin_exam_result_monitor);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_exam_result_monitor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AdminExamResultMonitorViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.examresult_monitor_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        LinearLayout chipGroup = view.findViewById(R.id.chipGroup);
        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_results_for_filter);
        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<ExamResult> adapter = new SimpleListAdapter<>(R.layout.item_exam_result_monitor, (itemView, result, position) -> {
            ((TextView) itemView.findViewById(R.id.textStudentName)).setText(result.getStudentName());
            ((TextView) itemView.findViewById(R.id.textSubject)).setText(
                    getString(R.string.subject_code_title_format, result.getSubjectCode(), result.getSubjectTitle()));
            com.example.uos_lms.core.ui.AccentColors.applyBar(itemView.findViewById(R.id.accentBar),
                    ResultStatusChipHelper.bind(itemView.findViewById(R.id.textStatusChip), result.getStatus()));
            TextView marks = itemView.findViewById(R.id.textMarks);
            if (result.getGrade() != null) {
                marks.setText(getString(R.string.marks_grade_format, result.getObtainedMarks(), result.getTotalMarks(),
                        result.getGrade(), String.format(Locale.US, "%.2f", result.getGpaPoint())));
            } else {
                marks.setText(getString(R.string.marks_format, result.getObtainedMarks(), result.getTotalMarks()));
            }
            ((TextView) itemView.findViewById(R.id.textTeacher)).setText(getString(R.string.teacher_label_format, result.getTeacherName()));
        });
        recyclerList.setAdapter(adapter);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            rebuildChips(chipGroup, state);

            boolean hasResults = !state.getVisibleResults().isEmpty();
            emptyState.setVisibility(hasResults || state.isLoading() ? View.GONE : View.VISIBLE);
            recyclerList.setVisibility(hasResults ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getVisibleResults());

            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void rebuildChips(LinearLayout chipGroup, AdminExamResultUiState state) {
        chipGroup.removeAllViews();
        chipGroup.addView(buildChip(getString(R.string.filter_all_format, state.getResults().size()),
                state.getStatusFilter() == null, () -> viewModel.setStatusFilter(null)));
        Map<ResultStatus, Integer> counts = state.getCounts();
        for (ResultStatus status : ResultStatus.values()) {
            int count = counts.getOrDefault(status, 0);
            chipGroup.addView(buildChip(getString(R.string.filter_status_format, status.name(), count),
                    state.getStatusFilter() == status, () -> viewModel.setStatusFilter(status)));
        }
    }

    private Chip buildChip(String label, boolean selected, Runnable onClick) {
        Chip chip = new Chip(requireContext());
        chip.setText(label);
        chip.setCheckable(true);
        chip.setChecked(selected);
        chip.setOnClickListener(v -> onClick.run());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMarginEnd(8);
        chip.setLayoutParams(params);
        return chip;
    }
}
