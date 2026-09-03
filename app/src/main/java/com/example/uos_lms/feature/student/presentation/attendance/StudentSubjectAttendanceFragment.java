package com.example.uos_lms.feature.student.presentation.attendance;

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
import com.example.uos_lms.core.domain.model.AttendanceRecord;
import com.example.uos_lms.core.ui.AccentColors;
import com.example.uos_lms.core.ui.AttendanceStatusChipHelper;
import com.example.uos_lms.core.ui.RefreshUx;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.snackbar.Snackbar;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class StudentSubjectAttendanceFragment extends Fragment {

    private StudentSubjectAttendanceViewModel viewModel;

    public StudentSubjectAttendanceFragment() {
        super(R.layout.fragment_student_subject_attendance);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student_subject_attendance, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(StudentSubjectAttendanceViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.attendance_screen_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.swipeRefresh);
        RefreshUx.Binding refreshBinding = RefreshUx.bindToolbarIcon(
                toolbar.findViewById(R.id.toolbarActionSlot), swipeRefresh, () -> viewModel.refresh());

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_attendance_this_subject_message);
        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<AttendanceRecord> adapter = new SimpleListAdapter<>(R.layout.item_attendance_record, (itemView, record, position) -> {
            itemView.findViewById(R.id.textStudentName).setVisibility(View.GONE);
            ((TextView) itemView.findViewById(R.id.textDate)).setText(DateKeyUtils.dateKeyToDisplay(record.getDateKey()));

            int accentColorRes = AttendanceStatusChipHelper.bind(itemView.findViewById(R.id.textStatus), record.getStatus());
            AccentColors.applyBar(itemView.findViewById(R.id.accentBar), accentColorRes);
        });
        recyclerList.setAdapter(adapter);

        View summaryCard = view.findViewById(R.id.summaryCard);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            summaryCard.setVisibility(state.getTotalCount() > 0 ? View.VISIBLE : View.GONE);
            ((TextView) view.findViewById(R.id.textPercentage)).setText(getString(R.string.percent_present, state.getPercentage()));
            ((TextView) view.findViewById(R.id.textAttendedCount)).setText(
                    getString(R.string.classes_attended_format, state.getPresentCount(), state.getTotalCount()));

            boolean hasRecords = !state.getRecords().isEmpty();
            emptyState.setVisibility(hasRecords || state.isLoading() ? View.GONE : View.VISIBLE);
            recyclerList.setVisibility(hasRecords ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getRecords());
            refreshBinding.setRefreshing(state.isRefreshing());
            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
