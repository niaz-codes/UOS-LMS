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
import com.example.uos_lms.core.ui.AccentColors;
import com.example.uos_lms.core.ui.AttendanceStatusChipHelper;
import com.example.uos_lms.core.ui.SimpleListAdapter;

import dagger.hilt.android.AndroidEntryPoint;

/** Teacher's own staff attendance - view-only, marked by the HOD/Admin; a teacher can never
 * mark or edit their own record here. */
@AndroidEntryPoint
public class TeacherOwnAttendanceFragment extends Fragment {

    private TeacherOwnAttendanceViewModel viewModel;

    public TeacherOwnAttendanceFragment() {
        super(R.layout.fragment_teacher_own_attendance);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_teacher_own_attendance, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TeacherOwnAttendanceViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.my_attendance_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        TextView textPercentage = view.findViewById(R.id.textPercentage);
        TextView textRecordCount = view.findViewById(R.id.textRecordCount);
        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_attendance_recorded);
        RecyclerView recyclerRecords = view.findViewById(R.id.recyclerRecords);

        recyclerRecords.setLayoutManager(new LinearLayoutManager(requireContext()));
        SimpleListAdapter<com.example.uos_lms.core.domain.model.TeacherAttendanceRecord> adapter = new SimpleListAdapter<>(
                R.layout.item_attendance_record, (itemView, record, position) -> {
                    TextView textCourse = itemView.findViewById(R.id.textStudentName);
                    textCourse.setVisibility(View.VISIBLE);
                    textCourse.setText(record.getSubjectCode() + " - " + record.getSubjectTitle());
                    ((TextView) itemView.findViewById(R.id.textDate)).setText(DateKeyUtils.dateKeyToDisplay(record.getDateKey()));
                    int accentColorRes = AttendanceStatusChipHelper.bind(itemView.findViewById(R.id.textStatus), record.getStatus());
                    AccentColors.applyBar(itemView.findViewById(R.id.accentBar), accentColorRes);
                });
        recyclerRecords.setAdapter(adapter);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            textPercentage.setText(getString(R.string.percent_present, state.getPercentage()));
            textRecordCount.setText(getString(R.string.records_count, state.getPresentCount(), state.getRecords().size()));

            boolean hasRecords = !state.getRecords().isEmpty();
            emptyState.setVisibility(hasRecords || state.isLoading() ? View.GONE : View.VISIBLE);
            recyclerRecords.setVisibility(hasRecords ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getRecords());
        });
    }
}
