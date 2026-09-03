package com.example.uos_lms.feature.hod.presentation.results;

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
import com.example.uos_lms.core.domain.model.StudentSemesterResultSummary;
import com.example.uos_lms.core.ui.AccentColors;
import com.example.uos_lms.core.ui.PromotionStatusChipHelper;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.snackbar.Snackbar;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HodSemesterResultFragment extends Fragment {

    private HodSemesterResultViewModel viewModel;

    public HodSemesterResultFragment() {
        super(R.layout.fragment_hod_semester_result_list);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_hod_semester_result_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HodSemesterResultViewModel.class);

        Bundle args = getArguments();
        String semesterLabel = args != null ? args.getString("semesterLabel") : null;

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(semesterLabel != null ? semesterLabel : getString(R.string.results_title));
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_semester_results_yet);
        View progressLoading = view.findViewById(R.id.progressLoading);
        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<StudentSemesterResultSummary> adapter = new SimpleListAdapter<>(
                R.layout.item_teacher_result_student_row, (itemView, result, position) -> {
                    ((TextView) itemView.findViewById(R.id.textStudentName)).setText(result.getStudentName());
                    ((TextView) itemView.findViewById(R.id.textRollNumber)).setText(result.getStudentRollNumber());
                    ((TextView) itemView.findViewById(R.id.textMarks)).setText(
                            getString(R.string.semester_gpa_label_format, result.getSemesterGpa()));
                    ((TextView) itemView.findViewById(R.id.textGpaPoint)).setText(
                            getString(R.string.cumulative_cgpa_label_format, result.getCumulativeCgpa()));
                    int colorRes = PromotionStatusChipHelper.bind(itemView.findViewById(R.id.textStatusChip), result.getPromotionStatus());
                    AccentColors.applyBar(itemView.findViewById(R.id.accentBar), colorRes);

                    itemView.setOnClickListener(v -> {
                        Bundle navArgs = new Bundle();
                        navArgs.putString("studentId", result.getStudentUid());
                        navArgs.putString("studentName", result.getStudentName());
                        navArgs.putString("semesterId", result.getSemesterId());
                        NavHostFragment.findNavController(this).navigate(R.id.hodStudentSemesterResultFragment, navArgs);
                    });
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
            }
        });
    }
}
