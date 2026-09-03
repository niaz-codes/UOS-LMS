package com.example.uos_lms.feature.student.presentation.quiz;

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
import com.example.uos_lms.core.domain.model.QuizAttempt;
import com.example.uos_lms.core.ui.AccentColors;
import com.example.uos_lms.core.ui.RefreshUx;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.snackbar.Snackbar;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class StudentSubjectQuizzesFragment extends Fragment {

    private StudentSubjectQuizzesViewModel viewModel;
    private boolean resumedOnce;

    public StudentSubjectQuizzesFragment() {
        super(R.layout.fragment_student_subject_quizzes);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student_subject_quizzes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(StudentSubjectQuizzesViewModel.class);

        Bundle args = requireArguments();
        String subjectId = args.getString("subjectId");

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.quizzes_exams_screen_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.swipeRefresh);
        RefreshUx.Binding refreshBinding = RefreshUx.bindToolbarIcon(
                toolbar.findViewById(R.id.toolbarActionSlot), swipeRefresh, () -> viewModel.refresh());

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_quizzes_this_subject_message);
        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<QuizListItem> adapter = new SimpleListAdapter<>(R.layout.item_student_quiz_row, (itemView, item, position) -> {
            Quiz quiz = item.getQuiz();
            QuizAttempt attempt = item.getAttempt();
            boolean isClosed = attempt == null && System.currentTimeMillis() > quiz.getDueDateMillis();

            ((TextView) itemView.findViewById(R.id.textTitle)).setText(quiz.getTitle());
            ((TextView) itemView.findViewById(R.id.textMeta)).setText(getString(R.string.due_questions_format,
                    DateKeyUtils.millisToDisplay(quiz.getDueDateMillis()), quiz.getQuestions().size()));

            TextView textStatus = itemView.findViewById(R.id.textStatus);
            TextView textFeedback = itemView.findViewById(R.id.textFeedback);
            int accentColorRes;
            if (attempt != null) {
                accentColorRes = R.color.status_success;
                AccentColors.applyPill(textStatus, accentColorRes,
                        getString(R.string.score_label_format, attempt.getEffectiveScore(), attempt.getTotalMarks()));
                if (attempt.getFeedback() != null && !attempt.getFeedback().isBlank()) {
                    textFeedback.setText(getString(R.string.feedback_prefix_format, attempt.getFeedback()));
                    textFeedback.setVisibility(View.VISIBLE);
                } else {
                    textFeedback.setVisibility(View.GONE);
                }
            } else if (isClosed) {
                accentColorRes = R.color.error_color;
                AccentColors.applyPill(textStatus, accentColorRes, getString(R.string.closed_label));
                textFeedback.setVisibility(View.GONE);
            } else {
                accentColorRes = R.color.status_warning;
                AccentColors.applyPill(textStatus, accentColorRes, getString(R.string.not_attempted_label));
                textFeedback.setVisibility(View.GONE);
            }
            AccentColors.applyBar(itemView.findViewById(R.id.accentBar), accentColorRes);

            itemView.setOnClickListener(v -> {
                Bundle extras = new Bundle();
                extras.putString("quizId", quiz.getId());
                extras.putString("subjectId", subjectId);
                NavHostFragment.findNavController(this).navigate(R.id.takeQuizFragment, extras);
            });
        });
        recyclerList.setAdapter(adapter);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            boolean hasItems = !state.getItems().isEmpty();
            emptyState.setVisibility(hasItems || state.isLoading() ? View.GONE : View.VISIBLE);
            recyclerList.setVisibility(hasItems ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getItems());
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
