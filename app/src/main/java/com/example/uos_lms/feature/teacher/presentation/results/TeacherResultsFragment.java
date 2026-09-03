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
import com.example.uos_lms.core.domain.model.Subject;
import com.example.uos_lms.core.ui.AccentColors;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.snackbar.Snackbar;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TeacherResultsFragment extends Fragment {

    private TeacherResultsViewModel viewModel;

    public TeacherResultsFragment() {
        super(R.layout.fragment_teacher_results);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_teacher_results, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TeacherResultsViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.results_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.results_no_subjects_assigned);
        View progressLoading = view.findViewById(R.id.progressLoading);
        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<Subject> adapter = new SimpleListAdapter<>(R.layout.item_results_subject_row, (itemView, subject, position) -> {
            ((TextView) itemView.findViewById(R.id.textTitle)).setText(subject.getTitle());
            ((TextView) itemView.findViewById(R.id.textCodeCredits)).setText(
                    subject.getCode() + " • " + subject.getCreditHours() + " credit hours");
            AccentColors.applyBar(itemView.findViewById(R.id.accentBar), R.color.role_teacher_start);

            itemView.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString("subjectId", subject.getId());
                args.putString("subjectTitle", subject.getTitle());
                NavHostFragment.findNavController(this).navigate(R.id.teacherSubjectResultFragment, args);
            });
        });
        recyclerList.setAdapter(adapter);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            progressLoading.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
            boolean hasSubjects = !state.getSubjects().isEmpty();
            emptyState.setVisibility(!state.isLoading() && !hasSubjects ? View.VISIBLE : View.GONE);
            recyclerList.setVisibility(hasSubjects ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getSubjects());

            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
                viewModel.consumeError();
            }
        });
    }
}
