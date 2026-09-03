package com.example.uos_lms.feature.student.presentation.leave;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.example.uos_lms.core.ui.LeaveStatusChipHelper;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class StudentLeaveFragment extends Fragment {

    private StudentLeaveViewModel viewModel;
    private ActivityResultLauncher<String> filePicker;
    private Uri pickedAttachmentUri;
    private MaterialButton attachButtonRef;

    public StudentLeaveFragment() {
        super(R.layout.fragment_student_leave);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        filePicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null) return;
            pickedAttachmentUri = uri;
            if (attachButtonRef != null) attachButtonRef.setText(R.string.attach_file_selected);
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student_leave, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(StudentLeaveViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.student_leave_applications_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_leave_applications_message);
        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<LeaveApplication> adapter = new SimpleListAdapter<>(R.layout.item_leave_card, (itemView, leave, position) -> {
            itemView.findViewById(R.id.textStudentName).setVisibility(View.GONE);
            ((TextView) itemView.findViewById(R.id.textDateRange)).setText(getString(R.string.leave_date_range_format,
                    DateKeyUtils.millisToDisplay(leave.getFromDateMillis()), DateKeyUtils.millisToDisplay(leave.getToDateMillis())));
            ((TextView) itemView.findViewById(R.id.textReason)).setText(leave.getReason());
            int accentColorRes = LeaveStatusChipHelper.bind(itemView.findViewById(R.id.textStatus), leave.getStatus());
            com.example.uos_lms.core.ui.AccentColors.applyBar(itemView.findViewById(R.id.accentBar), accentColorRes);

            TextView textAttachment = itemView.findViewById(R.id.textAttachment);
            if (leave.getAttachmentUrl() != null) {
                textAttachment.setText(R.string.attach_file_selected);
                textAttachment.setVisibility(View.VISIBLE);
            } else {
                textAttachment.setVisibility(View.GONE);
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
        pickedAttachmentUri = null;
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_apply_leave, null);
        MaterialButton buttonFromDate = dialogView.findViewById(R.id.buttonPickFromDate);
        MaterialButton buttonToDate = dialogView.findViewById(R.id.buttonPickToDate);
        TextInputEditText editReason = dialogView.findViewById(R.id.editReason);
        MaterialButton buttonAttachFile = dialogView.findViewById(R.id.buttonAttachFile);
        TextView textError = dialogView.findViewById(R.id.textError);
        attachButtonRef = buttonAttachFile;

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
        buttonAttachFile.setOnClickListener(v -> filePicker.launch("*/*"));

        Dialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.apply_for_leave)
                .setView(dialogView)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel_button, null)
                .setOnDismissListener(d -> attachButtonRef = null)
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
            viewModel.applyForLeave(fromDate[0], toDate[0], reason, pickedAttachmentUri);
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
