package com.example.uos_lms.feature.hod.presentation.semester;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uos_lms.R;
import com.example.uos_lms.core.domain.model.Subject;
import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.core.ui.SelectDialogHelper;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HodSemesterSubjectsFragment extends Fragment {

    private HodSemesterSubjectsViewModel viewModel;

    public HodSemesterSubjectsFragment() {
        super(R.layout.fragment_hod_semester_subjects);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_hod_semester_subjects, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HodSemesterSubjectsViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.semester_fallback_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_subjects_in_semester);

        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<Subject> adapter = new SimpleListAdapter<>(R.layout.item_hod_subject_row, (itemView, subject, position) -> {
            ((TextView) itemView.findViewById(R.id.textTitle)).setText(subject.getTitle());
            ((TextView) itemView.findViewById(R.id.textCodeCredits)).setText(
                    subject.getCode() + " • " + subject.getCreditHours() + " credit hours");
            com.example.uos_lms.core.ui.AccentColors.applyBar(itemView.findViewById(R.id.accentBar), R.color.role_hod_start);

            TextView textTeacher = itemView.findViewById(R.id.textTeacher);
            if (subject.getTeacherName() != null) {
                textTeacher.setText(getString(R.string.teacher_assigned_format, subject.getTeacherName()));
                textTeacher.setTextColor(ContextCompat.getColor(requireContext(), R.color.indigo_primary));
            } else {
                textTeacher.setText(R.string.unassigned);
                textTeacher.setTextColor(ContextCompat.getColor(requireContext(), R.color.error_color));
            }

            MaterialButton buttonAssignTeacher = itemView.findViewById(R.id.buttonAssignTeacher);
            buttonAssignTeacher.setText(subject.getTeacherUid() == null ? R.string.assign_teacher : R.string.change_teacher);
            buttonAssignTeacher.setOnClickListener(v -> {
                HodSemesterSubjectsUiState state = viewModel.getUiState().getValue();
                SelectDialogHelper.show(requireContext(), getString(R.string.assign_teacher_title),
                        state.getTeachers(), User::getFullName, getString(R.string.no_approved_teachers),
                        teacher -> viewModel.assignTeacher(subject.getId(), teacher));
            });

            View buttonUnassignTeacher = itemView.findViewById(R.id.buttonUnassignTeacher);
            buttonUnassignTeacher.setVisibility(subject.getTeacherUid() != null ? View.VISIBLE : View.GONE);
            buttonUnassignTeacher.setOnClickListener(v -> viewModel.unassignTeacher(subject.getId()));
        });
        recyclerList.setAdapter(adapter);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            String title = state.getSemester() != null ? state.getSemester().getDisplayName() : getString(R.string.semester_fallback_title);
            ((TextView) toolbar.findViewById(R.id.textTitle)).setText(title);

            boolean hasSubjects = !state.getSubjects().isEmpty();
            emptyState.setVisibility(hasSubjects || state.isLoading() ? View.GONE : View.VISIBLE);
            recyclerList.setVisibility(hasSubjects ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getSubjects());

            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
                viewModel.clearError();
            }
        });
    }
}
