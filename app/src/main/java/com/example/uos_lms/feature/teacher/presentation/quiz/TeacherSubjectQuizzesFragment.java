package com.example.uos_lms.feature.teacher.presentation.quiz;

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
import com.example.uos_lms.core.domain.model.Quiz;
import com.example.uos_lms.core.ui.RefreshUx;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.snackbar.Snackbar;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TeacherSubjectQuizzesFragment extends Fragment {

    private TeacherSubjectQuizzesViewModel viewModel;
    private boolean resumedOnce;

    public TeacherSubjectQuizzesFragment() {
        super(R.layout.fragment_teacher_subject_quizzes);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_teacher_subject_quizzes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TeacherSubjectQuizzesViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.quizzes_exams_screen_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.swipeRefresh);
        RefreshUx.Binding refreshBinding = RefreshUx.bindToolbarIcon(
                toolbar.findViewById(R.id.toolbarActionSlot), swipeRefresh, () -> viewModel.refresh());

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_quizzes_tap_add);
        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<Quiz> adapter = new SimpleListAdapter<>(R.layout.item_quiz_monitor, (itemView, quiz, position) -> {
            ((TextView) itemView.findViewById(R.id.textTitle)).setText(quiz.getTitle());
            itemView.findViewById(R.id.textSubject).setVisibility(View.GONE);
            ((TextView) itemView.findViewById(R.id.textMeta)).setText(getString(R.string.due_questions_marks_format,
                    DateKeyUtils.millisToDisplay(quiz.getDueDateMillis()), quiz.getQuestions().size(), quiz.getTotalMarks()));
            com.example.uos_lms.core.ui.AccentColors.applyBar(itemView.findViewById(R.id.accentBar), R.color.status_info);

            itemView.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString("quizId", quiz.getId());
                args.putString("title", quiz.getTitle());
                args.putInt("totalMarks", quiz.getTotalMarks());
                NavHostFragment.findNavController(this).navigate(R.id.quizAttemptsFragment, args);
            });
        });
        recyclerList.setAdapter(adapter);

        view.findViewById(R.id.fabAdd).setOnClickListener(v -> {
            Bundle args = requireArguments();
            Bundle navArgs = new Bundle();
            navArgs.putString("subjectId", args.getString("subjectId"));
            navArgs.putString("departmentId", args.getString("departmentId"));
            navArgs.putString("semesterId", args.getString("semesterId"));
            NavHostFragment.findNavController(this).navigate(R.id.createQuizFragment, navArgs);
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            boolean hasQuizzes = !state.getQuizzes().isEmpty();
            emptyState.setVisibility(hasQuizzes || state.isLoading() ? View.GONE : View.VISIBLE);
            recyclerList.setVisibility(hasQuizzes ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getQuizzes());
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
