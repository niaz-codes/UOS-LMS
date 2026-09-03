package com.example.uos_lms.feature.teacher.presentation.results;

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
import com.example.uos_lms.core.domain.model.ExamResult;
import com.example.uos_lms.core.domain.model.SubjectResultStatus;
import com.example.uos_lms.core.ui.AccentColors;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.example.uos_lms.core.ui.SubjectResultStatusChipHelper;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TeacherSubjectResultFragment extends Fragment {

    private TeacherSubjectResultViewModel viewModel;

    public TeacherSubjectResultFragment() {
        super(R.layout.fragment_teacher_subject_result);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_teacher_subject_result, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TeacherSubjectResultViewModel.class);

        Bundle args = getArguments();
        String subjectTitle = args != null ? args.getString("subjectTitle") : null;

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(subjectTitle != null ? subjectTitle : getString(R.string.results_title));
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_results_published_yet);
        View progressLoading = view.findViewById(R.id.progressLoading);
        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<ExamResult> adapter = new SimpleListAdapter<>(R.layout.item_teacher_result_student_row, (itemView, result, position) -> {
            ((TextView) itemView.findViewById(R.id.textStudentName)).setText(result.getStudentName());
            ((TextView) itemView.findViewById(R.id.textRollNumber)).setText(result.getStudentRollNumber());
            ((TextView) itemView.findViewById(R.id.textMarks)).setText(
                    getString(R.string.marks_slash_format, result.getObtainedMarks(), result.getTotalMarks()));
            ((TextView) itemView.findViewById(R.id.textGpaPoint)).setText(
                    getString(R.string.gpa_result_format, String.format(Locale.getDefault(), "%.2f", result.getGpaPoint())));

            boolean failed = "F".equals(result.getGrade());
            int colorRes = SubjectResultStatusChipHelper.bind(itemView.findViewById(R.id.textStatusChip),
                    failed ? SubjectResultStatus.FAIL : SubjectResultStatus.PASS);
            AccentColors.applyBar(itemView.findViewById(R.id.accentBar), colorRes);
        });
        recyclerList.setAdapter(adapter);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            progressLoading.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
            boolean hasResults = !state.getResults().isEmpty();
            emptyState.setVisibility(!state.isLoading() && !hasResults ? View.VISIBLE : View.GONE);
            recyclerList.setVisibility(hasResults ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getResults());

            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
                viewModel.consumeError();
            }
        });
    }
}
