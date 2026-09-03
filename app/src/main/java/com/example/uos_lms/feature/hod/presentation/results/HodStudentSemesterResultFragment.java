package com.example.uos_lms.feature.hod.presentation.results;

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

import com.example.uos_lms.R;
import com.example.uos_lms.core.domain.model.SubjectResultStatus;
import com.example.uos_lms.core.ui.PromotionStatusChipHelper;
import com.example.uos_lms.core.ui.SubjectResultRowBinder;
import com.example.uos_lms.core.ui.SubjectResultStatusChipHelper;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HodStudentSemesterResultFragment extends Fragment {

    private HodStudentSemesterResultViewModel viewModel;

    public HodStudentSemesterResultFragment() {
        super(R.layout.fragment_hod_student_semester_result);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_hod_student_semester_result, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HodStudentSemesterResultViewModel.class);

        Bundle args = getArguments();
        String studentName = args != null ? args.getString("studentName") : null;

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(studentName != null ? studentName : getString(R.string.results_title));
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_semester_results_yet);
        View progressLoading = view.findViewById(R.id.progressLoading);
        View contentContainer = view.findViewById(R.id.contentContainer);
        LinearLayout subjectsContainer = view.findViewById(R.id.subjectsContainer);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            progressLoading.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
            emptyState.setVisibility(!state.isLoading() && !state.isHasResult() ? View.VISIBLE : View.GONE);
            contentContainer.setVisibility(!state.isLoading() && state.isHasResult() ? View.VISIBLE : View.GONE);

            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
            }

            if (state.isLoading() || !state.isHasResult()) return;

            ((TextView) view.findViewById(R.id.textSemesterGpa)).setText(String.format(Locale.getDefault(), "%.2f", state.getSemesterGpa()));
            ((TextView) view.findViewById(R.id.textCumulativeCgpa)).setText(String.format(Locale.getDefault(), "%.2f", state.getCumulativeCgpa()));

            SubjectResultStatusChipHelper.bind(view.findViewById(R.id.textOverallStatus),
                    state.isAllPassed() ? SubjectResultStatus.PASS : SubjectResultStatus.FAIL);
            PromotionStatusChipHelper.bind(view.findViewById(R.id.textPromotionStatus), state.getPromotionStatus());

            SubjectResultRowBinder.bindRows(subjectsContainer, state.getSubjectResults(), state.getRepeatStatusBySubjectId());
        });
    }
}
