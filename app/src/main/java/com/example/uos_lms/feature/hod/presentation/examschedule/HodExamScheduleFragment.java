package com.example.uos_lms.feature.hod.presentation.examschedule;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uos_lms.R;
import com.example.uos_lms.core.common.DateKeyUtils;
import com.example.uos_lms.core.common.TimeOfDayUtils;
import com.example.uos_lms.core.domain.model.ExamSchedule;
import com.example.uos_lms.core.domain.model.ExamType;
import com.example.uos_lms.core.ui.ExamScheduleStatusChipHelper;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.snackbar.Snackbar;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HodExamScheduleFragment extends Fragment {

    private HodExamScheduleViewModel viewModel;

    public HodExamScheduleFragment() {
        super(R.layout.fragment_hod_examschedule);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_hod_examschedule, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HodExamScheduleViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.exam_schedule_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        ProgressBar progressLoading = view.findViewById(R.id.progressLoading);
        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_exam_schedules_message);
        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<ExamSchedule> adapter = new SimpleListAdapter<>(R.layout.item_exam_schedule_card, (itemView, schedule, position) -> {
            ((TextView) itemView.findViewById(R.id.textExamType)).setText(
                    schedule.getExamType() == ExamType.MID_TERM ? R.string.exam_type_mid_term : R.string.exam_type_final_term);
            ((TextView) itemView.findViewById(R.id.textSubject)).setText(schedule.getSubjectCode() + " • " + schedule.getSubjectTitle());
            ((TextView) itemView.findViewById(R.id.textDateTime)).setText(DateKeyUtils.millisToDisplay(schedule.getExamDateMillis())
                    + " • " + TimeOfDayUtils.formatRange(schedule.getStartTimeMinutes(), schedule.getEndTimeMinutes()));
            ((TextView) itemView.findViewById(R.id.textRoom)).setText(getString(R.string.timetable_room_format, schedule.getRoom()));

            TextView textInvigilator = itemView.findViewById(R.id.textInvigilator);
            if (!schedule.getInvigilatorName().isBlank()) {
                textInvigilator.setText(getString(R.string.exam_invigilator_format, schedule.getInvigilatorName()));
                textInvigilator.setVisibility(View.VISIBLE);
            } else {
                textInvigilator.setVisibility(View.GONE);
            }

            com.example.uos_lms.core.ui.AccentColors.applyBar(itemView.findViewById(R.id.accentBar),
                    ExamScheduleStatusChipHelper.bind(itemView.findViewById(R.id.textStatus), schedule.getStatus()));
            itemView.findViewById(R.id.adminActionsRow).setVisibility(View.GONE);
        });
        recyclerList.setAdapter(adapter);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            progressLoading.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);

            boolean hasSchedules = !state.getSortedSchedules().isEmpty();
            emptyState.setVisibility(!state.isLoading() && !hasSchedules ? View.VISIBLE : View.GONE);
            recyclerList.setVisibility(hasSchedules ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getSortedSchedules());

            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
