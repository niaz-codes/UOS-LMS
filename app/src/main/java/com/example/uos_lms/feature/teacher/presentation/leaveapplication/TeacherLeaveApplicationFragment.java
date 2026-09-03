package com.example.uos_lms.feature.teacher.presentation.leaveapplication;

import android.app.DatePickerDialog;
import android.app.Dialog;
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
import com.example.uos_lms.core.domain.model.LeaveType;
import com.example.uos_lms.core.domain.model.TeacherLeaveApplication;
import com.example.uos_lms.core.ui.LeaveStatusChipHelper;
import com.example.uos_lms.core.ui.SelectDialogHelper;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Arrays;
import java.util.Calendar;

import dagger.hilt.android.AndroidEntryPoint;

/** Teacher applies for their own leave and tracks its status here - distinct from the
 * existing "Leave Applications" screen (TeacherLeaveFragment), which is where a Teacher
 * reviews STUDENT leave requests. Backed by a separate collection/endpoint
 * (ApiTeacherLeaveDataSource / /api/teacher-leaves) - see TeacherLeaveApplication.js for why. */
@AndroidEntryPoint
public class TeacherLeaveApplicationFragment extends Fragment {

    private TeacherLeaveApplicationViewModel viewModel;
    private LeaveType selectedType = LeaveType.CASUAL;
    private MaterialButton dialogButtonPickType;

    public TeacherLeaveApplicationFragment() {
        super(R.layout.fragment_teacher_leave_application);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_teacher_leave_application, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TeacherLeaveApplicationViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.teacher_leave_application_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_leave_applications_message);
        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<TeacherLeaveApplication> adapter = new SimpleListAdapter<>(R.layout.item_teacher_leave_card, (itemView, leave, position) -> {
            itemView.findViewById(R.id.textTeacherName).setVisibility(View.GONE);
            ((TextView) itemView.findViewById(R.id.textLeaveType)).setText(leave.getLeaveType().name());
            ((TextView) itemView.findViewById(R.id.textDateRange)).setText(getString(R.string.leave_date_range_format,
                    DateKeyUtils.millisToDisplay(leave.getFromDateMillis()), DateKeyUtils.millisToDisplay(leave.getToDateMillis())));
            ((TextView) itemView.findViewById(R.id.textReason)).setText(leave.getReason());
            int accentColorRes = LeaveStatusChipHelper.bind(itemView.findViewById(R.id.textStatus), leave.getStatus());
            com.example.uos_lms.core.ui.AccentColors.applyBar(itemView.findViewById(R.id.accentBar), accentColorRes);

            TextView textRejectionReason = itemView.findViewById(R.id.textRejectionReason);
            if (leave.getRejectionReason() != null && !leave.getRejectionReason().isBlank()) {
                textRejectionReason.setText(getString(R.string.leave_rejection_reason_format, leave.getRejectionReason()));
                textRejectionReason.setVisibility(View.VISIBLE);
            } else {
                textRejectionReason.setVisibility(View.GONE);
            }

            TextView textReviewer = itemView.findViewById(R.id.textReviewer);
            if (leave.getReviewerName() != null) {
                textReviewer.setText(getString(R.string.leave_reviewed_by_format, leave.getReviewerName(), leave.getReviewerRole()));
                textReviewer.setVisibility(View.VISIBLE);
            } else {
                textReviewer.setVisibility(View.GONE);
            }

            itemView.findViewById(R.id.decisionActions).setVisibility(View.GONE);
        });
        recyclerList.setAdapter(adapter);

        view.findViewById(R.id.fabApply).setOnClickListener(v -> showApplyLeaveDialog());

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            view.findViewById(R.id.progressLoading).setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);

            boolean hasLeaves = !state.getLeaves().isEmpty();
            emptyState.setVisibility(hasLeaves || state.isLoading() ? View.GONE : View.VISIBLE);
            recyclerList.setVisibility(hasLeaves ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getLeaves());

            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
                viewModel.consumeMessages();
            } else if (state.getActionMessage() != null) {
                Snackbar.make(view, state.getActionMessage(), Snackbar.LENGTH_SHORT).show();
                viewModel.consumeMessages();
            }
        });
    }

    private void showApplyLeaveDialog() {
        selectedType = LeaveType.CASUAL;
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_apply_teacher_leave, null);
        dialogButtonPickType = dialogView.findViewById(R.id.buttonPickLeaveType);
        MaterialButton buttonFromDate = dialogView.findViewById(R.id.buttonPickFromDate);
        MaterialButton buttonToDate = dialogView.findViewById(R.id.buttonPickToDate);
        TextInputEditText editReason = dialogView.findViewById(R.id.editReason);
        TextView textError = dialogView.findViewById(R.id.textError);

        dialogButtonPickType.setText(selectedType.name());
        dialogButtonPickType.setOnClickListener(v ->
                SelectDialogHelper.show(requireContext(), getString(R.string.leave_type_hint),
                        Arrays.asList(LeaveType.values()), Enum::name, "", type -> {
                            selectedType = type;
                            dialogButtonPickType.setText(type.name());
                        }));

        Long[] fromDate = {null};
        Long[] toDate = {null};

        buttonFromDate.setOnClickListener(v -> pickDate(fromDate[0], picked -> {
            fromDate[0] = picked;
            buttonFromDate.setText(getString(R.string.leave_from_date_format, DateKeyUtils.millisToDisplay(picked)));
        }));
        buttonToDate.setOnClickListener(v -> pickDate(toDate[0], picked -> {
            toDate[0] = picked;
            buttonToDate.setText(getString(R.string.leave_to_date_format, DateKeyUtils.millisToDisplay(picked)));
        }));

        Dialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.apply_for_leave)
                .setView(dialogView)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel_button, null)
                .setOnDismissListener(d -> dialogButtonPickType = null)
                .show();

        dialog.findViewById(android.R.id.button1).setOnClickListener(v -> {
            String reason = editReason.getText() == null ? "" : editReason.getText().toString().trim();
            if (fromDate[0] == null || toDate[0] == null) {
                textError.setText(R.string.leave_pick_dates_error);
                textError.setVisibility(View.VISIBLE);
                return;
            }
            if (toDate[0] < fromDate[0]) {
                textError.setText(R.string.leave_invalid_range_error);
                textError.setVisibility(View.VISIBLE);
                return;
            }
            if (reason.isEmpty()) {
                textError.setText(R.string.leave_reason_required_error);
                textError.setVisibility(View.VISIBLE);
                return;
            }
            viewModel.applyForLeave(selectedType, fromDate[0], toDate[0], reason);
            dialog.dismiss();
        });
    }

    private interface DatePicked {
        void onPicked(long millis);
    }

    private void pickDate(@Nullable Long existing, DatePicked callback) {
        Calendar calendar = Calendar.getInstance();
        if (existing != null) calendar.setTimeInMillis(existing);
        new DatePickerDialog(requireContext(), (dp, year, month, dayOfMonth) -> {
            Calendar picked = Calendar.getInstance();
            picked.set(year, month, dayOfMonth, 0, 0, 0);
            picked.set(Calendar.MILLISECOND, 0);
            callback.onPicked(picked.getTimeInMillis());
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }
}
