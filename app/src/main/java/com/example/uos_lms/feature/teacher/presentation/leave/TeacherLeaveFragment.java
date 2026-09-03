package com.example.uos_lms.feature.teacher.presentation.leave;

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
import com.example.uos_lms.core.domain.model.LeaveApplication;
import com.example.uos_lms.core.domain.model.LeaveStatus;
import com.example.uos_lms.core.ui.LeaveStatusChipHelper;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.snackbar.Snackbar;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TeacherLeaveFragment extends Fragment {

    private TeacherLeaveViewModel viewModel;

    public TeacherLeaveFragment() {
        super(R.layout.fragment_teacher_leave);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_teacher_leave, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TeacherLeaveViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.student_leave_applications_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        ProgressBar progressLoading = view.findViewById(R.id.progressLoading);
        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_pending_leave_message);
        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<LeaveApplication> adapter = new SimpleListAdapter<>(R.layout.item_leave_card, (itemView, leave, position) -> {
            ((TextView) itemView.findViewById(R.id.textStudentName)).setText(leave.getStudentName());
            ((TextView) itemView.findViewById(R.id.textDateRange)).setText(getString(R.string.leave_date_range_format,
                    DateKeyUtils.millisToDisplay(leave.getFromDateMillis()), DateKeyUtils.millisToDisplay(leave.getToDateMillis())));
            ((TextView) itemView.findViewById(R.id.textReason)).setText(leave.getReason());
            int accentColorRes = LeaveStatusChipHelper.bind(itemView.findViewById(R.id.textStatus), LeaveStatus.PENDING);
            com.example.uos_lms.core.ui.AccentColors.applyBar(itemView.findViewById(R.id.accentBar), accentColorRes);
            itemView.findViewById(R.id.textAttachment).setVisibility(leave.getAttachmentUrl() != null ? View.VISIBLE : View.GONE);
            itemView.findViewById(R.id.textReviewer).setVisibility(View.GONE);

            View actions = itemView.findViewById(R.id.decisionActions);
            actions.setVisibility(View.VISIBLE);
            boolean processing = leave.getId().equals(viewModel.getUiState().getValue().getProcessingLeaveId());
            itemView.findViewById(R.id.buttonApprove).setEnabled(!processing);
            itemView.findViewById(R.id.buttonReject).setEnabled(!processing);
            itemView.findViewById(R.id.buttonApprove).setOnClickListener(v -> viewModel.approve(leave));
            itemView.findViewById(R.id.buttonReject).setOnClickListener(v -> viewModel.reject(leave));
        });
        recyclerList.setAdapter(adapter);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            progressLoading.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);

            boolean hasLeaves = !state.getPending().isEmpty();
            emptyState.setVisibility(!state.isLoading() && !hasLeaves ? View.VISIBLE : View.GONE);
            recyclerList.setVisibility(hasLeaves ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getPending());

            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
                viewModel.consumeMessages();
            } else if (state.getActionMessage() != null) {
                Snackbar.make(view, state.getActionMessage(), Snackbar.LENGTH_SHORT).show();
                viewModel.consumeMessages();
            }
        });
    }

}
