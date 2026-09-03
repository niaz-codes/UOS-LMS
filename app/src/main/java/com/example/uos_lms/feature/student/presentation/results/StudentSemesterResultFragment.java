package com.example.uos_lms.feature.student.presentation.results;

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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.uos_lms.R;
import com.example.uos_lms.core.domain.model.SubjectResultSnapshot;
import com.example.uos_lms.core.domain.model.SubjectResultStatus;
import com.example.uos_lms.core.ui.AnimUtils;
import com.example.uos_lms.core.ui.PromotionStatusChipHelper;
import com.example.uos_lms.core.ui.RefreshUx;
import com.example.uos_lms.core.ui.SubjectResultRowBinder;
import com.example.uos_lms.core.ui.SubjectResultStatusChipHelper;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class StudentSemesterResultFragment extends Fragment {

    private StudentSemesterResultViewModel viewModel;
    private boolean heroAnimated;

    public StudentSemesterResultFragment() {
        super(R.layout.fragment_student_semester_result);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student_semester_result, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(StudentSemesterResultViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_semester_results_yet);
        View progressLoading = view.findViewById(R.id.progressLoading);
        SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.contentContainer);
        RefreshUx.Binding refreshBinding = RefreshUx.bindToolbarIcon(
                toolbar.findViewById(R.id.toolbarActionSlot), swipeRefresh, () -> viewModel.refresh());

        LinearLayout subjectsContainer = view.findViewById(R.id.subjectsContainer);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            progressLoading.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
            emptyState.setVisibility(!state.isLoading() && !state.isHasResult() ? View.VISIBLE : View.GONE);
            swipeRefresh.setVisibility(!state.isLoading() && state.isHasResult() ? View.VISIBLE : View.GONE);
            refreshBinding.setRefreshing(state.isRefreshing());

            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
                viewModel.consumeError();
            }

            if (state.isLoading() || !state.isHasResult()) return;

            ((TextView) toolbar.findViewById(R.id.textTitle)).setText(state.getSemesterLabel());
            ((TextView) view.findViewById(R.id.textSemesterGpa)).setText(String.format(Locale.getDefault(), "%.2f", state.getSemesterGpa()));
            ((TextView) view.findViewById(R.id.textCumulativeCgpa)).setText(String.format(Locale.getDefault(), "%.2f", state.getCumulativeCgpa()));
            ((TextView) view.findViewById(R.id.textSubjectCount)).setText(getString(
                    R.string.subjects_credit_hours_format, state.getSubjectResults().size(), totalCreditHours(state)));

            SubjectResultStatusChipHelper.bind(view.findViewById(R.id.textOverallStatus),
                    state.isAllPassed() ? SubjectResultStatus.PASS : SubjectResultStatus.FAIL);
            PromotionStatusChipHelper.bind(view.findViewById(R.id.textPromotionStatus), state.getPromotionStatus());

            SubjectResultRowBinder.bindRows(subjectsContainer, state.getSubjectResults(), state.getRepeatStatusBySubjectId());

            if (!heroAnimated) {
                heroAnimated = true;
                AnimUtils.fadeSlideIn(view.findViewById(R.id.heroCard), 0L);
            }
        });
    }

    private int totalCreditHours(StudentSemesterResultUiState state) {
        int total = 0;
        for (SubjectResultSnapshot subject : state.getSubjectResults()) {
            total += subject.getCreditHours();
        }
        return total;
    }
}
