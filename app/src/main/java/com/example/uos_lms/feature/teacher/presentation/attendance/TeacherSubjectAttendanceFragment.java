package com.example.uos_lms.feature.teacher.presentation.attendance;

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
import com.example.uos_lms.core.common.DateKeyUtils;
import com.example.uos_lms.core.ui.SimpleListAdapter;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TeacherSubjectAttendanceFragment extends Fragment {

    private TeacherSubjectAttendanceViewModel viewModel;

    public TeacherSubjectAttendanceFragment() {
        super(R.layout.fragment_teacher_subject_attendance);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_teacher_subject_attendance, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TeacherSubjectAttendanceViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.attendance_screen_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        view.findViewById(R.id.buttonTakeToday).setOnClickListener(v -> openDate(DateKeyUtils.todayDateKey()));

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_attendance_taken_yet);
        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<String> adapter = new SimpleListAdapter<>(R.layout.item_attendance_date_row, (itemView, dateKey, position) -> {
            ((TextView) itemView.findViewById(R.id.textDate)).setText(DateKeyUtils.dateKeyToDisplay(dateKey));
            itemView.setOnClickListener(v -> openDate(dateKey));
        });
        recyclerList.setAdapter(adapter);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            boolean hasDates = !state.getHistoryDates().isEmpty();
            emptyState.setVisibility(hasDates || state.isLoading() ? View.GONE : View.VISIBLE);
            recyclerList.setVisibility(hasDates ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getHistoryDates());
        });
    }

    private void openDate(String dateKey) {
        Bundle args = requireArguments();
        Bundle navArgs = new Bundle();
        navArgs.putString("subjectId", args.getString("subjectId"));
        navArgs.putString("departmentId", args.getString("departmentId"));
        navArgs.putString("semesterId", args.getString("semesterId"));
        navArgs.putString("dateKey", dateKey);
        NavHostFragment.findNavController(this).navigate(R.id.markAttendanceFragment, navArgs);
    }
}
