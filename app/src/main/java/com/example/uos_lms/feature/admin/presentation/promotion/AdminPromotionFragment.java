package com.example.uos_lms.feature.admin.presentation.promotion;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.uos_lms.R;
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.ExamResult;
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.Session;
import com.example.uos_lms.core.ui.ConfirmDialogHelper;
import com.example.uos_lms.core.ui.RefreshUx;
import com.example.uos_lms.core.ui.SelectDialogHelper;
import com.example.uos_lms.core.ui.UserAvatarHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminPromotionFragment extends Fragment {

    private AdminPromotionViewModel viewModel;
    private RefreshUx.Binding refreshBinding;

    public AdminPromotionFragment() {
        super(R.layout.fragment_admin_promotion);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_promotion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AdminPromotionViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.promotion_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.swipeRefresh);
        refreshBinding = RefreshUx.bindToolbarIcon(
                toolbar.findViewById(R.id.toolbarActionSlot), swipeRefresh, () -> viewModel.refresh());

        setLabel(view, R.id.rowDepartment, R.string.label_department);
        setLabel(view, R.id.rowSession, R.string.label_session);
        setLabel(view, R.id.rowSemester, R.string.label_semester);

        view.findViewById(R.id.buttonSelectDepartment).setOnClickListener(v ->
                SelectDialogHelper.show(requireContext(), getString(R.string.select_department),
                        viewModel.getUiState().getValue().getDepartments(), Department::getName,
                        getString(R.string.no_departments_exist), viewModel::selectDepartment));

        view.findViewById(R.id.buttonSelectSession).setOnClickListener(v ->
                SelectDialogHelper.show(requireContext(), getString(R.string.select_session),
                        viewModel.getUiState().getValue().getSessions(), Session::getLabel,
                        getString(R.string.no_sessions_exist), viewModel::selectSession));

        view.findViewById(R.id.buttonSelectSemester).setOnClickListener(v ->
                SelectDialogHelper.show(requireContext(), getString(R.string.select_current_semester),
                        viewModel.getUiState().getValue().getSemesters(), Semester::getDisplayName,
                        getString(R.string.no_semesters_exist), viewModel::selectSemester));

        view.findViewById(R.id.buttonPromote).setOnClickListener(v -> {
            AdminPromotionUiState state = viewModel.getUiState().getValue();
            if (state.getTargetSemester() == null) return;
            ConfirmDialogHelper.show(requireContext(), getString(R.string.promote_confirm_title),
                    getString(R.string.promote_confirm_message_format, state.getCurrentSemesterNumber(), state.getTargetSemester().getNumber()),
                    getString(R.string.promote_students), viewModel::promote);
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> render(view, state));
    }

    private void render(View view, AdminPromotionUiState state) {
        setValue(view, R.id.rowDepartment, state.getSelectedDepartmentName() != null ? state.getSelectedDepartmentName() : getString(R.string.not_assigned));
        setValue(view, R.id.rowSession, state.getSelectedSessionLabel() != null ? state.getSelectedSessionLabel() : getString(R.string.not_assigned));
        Semester currentSemester = findSemesterById(state.getSemesters(), state.getSelectedSemesterId());
        setValue(view, R.id.rowSemester, currentSemester != null ? currentSemester.getDisplayName() : getString(R.string.not_assigned));

        view.findViewById(R.id.buttonSelectSession).setEnabled(state.getSelectedDepartmentId() != null);
        view.findViewById(R.id.buttonSelectSemester).setEnabled(state.getSelectedSessionId() != null);

        TextView textTarget = view.findViewById(R.id.textTargetSemester);
        boolean semesterChosen = state.isSemesterChosen();
        textTarget.setVisibility(semesterChosen ? View.VISIBLE : View.GONE);
        if (semesterChosen) {
            if (state.getTargetSemester() != null) {
                textTarget.setText(getString(R.string.target_semester_ready_format, state.getCurrentSemesterNumber(), state.getTargetSemester().getNumber()));
            } else {
                textTarget.setText(getString(R.string.target_semester_missing_format, state.getCurrentSemesterNumber() + 1));
            }
        }

        view.findViewById(R.id.textStudentsHeader).setVisibility(semesterChosen ? View.VISIBLE : View.GONE);

        ProgressBar progress = view.findViewById(R.id.progressLoadingRoster);
        progress.setVisibility(state.isLoadingRoster() ? View.VISIBLE : View.GONE);

        TextView emptyState = view.findViewById(R.id.textEmptyRoster);
        boolean showEmpty = semesterChosen && !state.isLoadingRoster() && state.getRows().isEmpty();
        emptyState.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
        emptyState.setText(R.string.no_students_in_semester);

        LinearLayout container = view.findViewById(R.id.studentListContainer);
        container.removeAllViews();
        if (!state.isLoadingRoster()) {
            for (PromotionStudentRow row : state.getRows()) {
                container.addView(buildStudentRow(container, row));
            }
        }

        MaterialButton buttonPromote = view.findViewById(R.id.buttonPromote);
        buttonPromote.setEnabled(state.canPromote());
        buttonPromote.setText(state.getSelectedCount() > 0
                ? getString(R.string.promote_students_count_format, state.getSelectedCount())
                : getString(R.string.promote_students));

        refreshBinding.setRefreshing(state.isRefreshing());

        if (state.getErrorMessage() != null) {
            Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
            viewModel.consumeMessages();
        } else if (state.getActionMessage() != null) {
            Snackbar.make(view, state.getActionMessage(), Snackbar.LENGTH_SHORT).show();
            viewModel.consumeMessages();
        }
    }

    private View buildStudentRow(LinearLayout parent, PromotionStudentRow row) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.item_promotion_student_card, parent, false);
        UserAvatarHelper.bind(view.findViewById(R.id.imageAvatar), row.getStudent().getProfilePhotoUrl());
        ((TextView) view.findViewById(R.id.textName)).setText(row.getStudent().getFullName());
        ((TextView) view.findViewById(R.id.textRollNumber)).setText(
                getString(R.string.roll_number) + ": " + (row.getStudent().getRollNumber() != null ? row.getStudent().getRollNumber() : getString(R.string.not_assigned)));

        com.example.uos_lms.core.ui.AccentColors.applyBar(view.findViewById(R.id.accentBar), bindResultChip(view.findViewById(R.id.textResultStatus), row));

        CheckBox checkbox = view.findViewById(R.id.checkboxSelect);
        checkbox.setChecked(row.isSelected());
        checkbox.setEnabled(!row.isAlreadyPromoted());
        String uid = row.getStudent().getUid();
        checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) viewModel.toggleSelected(uid);
        });

        return view;
    }

    @androidx.annotation.ColorRes
    private int bindResultChip(TextView chip, PromotionStudentRow row) {
        int colorRes;
        String text;
        if (row.isAlreadyPromoted()) {
            colorRes = R.color.on_surface_variant_color;
            text = getString(R.string.already_promoted);
        } else if (!row.isHasResults()) {
            colorRes = R.color.on_surface_variant_color;
            text = getString(R.string.no_result_yet);
        } else if (row.hasFailedSubjects()) {
            colorRes = R.color.status_warning;
            StringBuilder codes = new StringBuilder();
            for (ExamResult result : row.getFailedResults()) {
                if (codes.length() > 0) codes.append(", ");
                codes.append(result.getSubjectCode());
            }
            text = getString(R.string.retake_subjects_format, codes.toString());
        } else {
            colorRes = R.color.status_success;
            text = getString(R.string.all_subjects_passed);
        }
        com.example.uos_lms.core.ui.AccentColors.applyPill(chip, colorRes, text);
        return colorRes;
    }

    private static void setLabel(View root, int rowId, int labelRes) {
        ((TextView) root.findViewById(rowId).findViewById(R.id.textLabel)).setText(labelRes);
    }

    private static void setValue(View root, int rowId, String value) {
        ((TextView) root.findViewById(rowId).findViewById(R.id.textValue)).setText(value);
    }

    @Nullable
    private static Semester findSemesterById(List<Semester> semesters, @Nullable String id) {
        if (id == null) return null;
        for (Semester semester : semesters) {
            if (semester.getId().equals(id)) return semester;
        }
        return null;
    }
}
