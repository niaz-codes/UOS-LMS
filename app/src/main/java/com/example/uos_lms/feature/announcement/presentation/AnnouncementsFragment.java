package com.example.uos_lms.feature.announcement.presentation;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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
import com.example.uos_lms.core.domain.model.Announcement;
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.Subject;
import com.example.uos_lms.core.domain.model.UserRole;
import com.example.uos_lms.core.ui.SelectDialogHelper;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AnnouncementsFragment extends Fragment {

    private AnnouncementsViewModel viewModel;
    private AnnouncementsUiState latestState;

    public AnnouncementsFragment() {
        super(R.layout.fragment_announcements);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_announcements, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AnnouncementsViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.announcements_screen_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_announcements_yet);
        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<Announcement> adapter = new SimpleListAdapter<>(R.layout.item_announcement_card, (itemView, announcement, position) -> {
            String scopeLabel;
            int accentColorRes;
            switch (announcement.getScope()) {
                case ALL:
                    scopeLabel = getString(R.string.scope_all_label);
                    accentColorRes = R.color.role_admin_start;
                    break;
                case DEPARTMENT:
                    scopeLabel = announcement.getDepartmentName() != null ? announcement.getDepartmentName() : getString(R.string.scope_department_fallback);
                    accentColorRes = R.color.role_hod_start;
                    break;
                default:
                    scopeLabel = announcement.getSubjectName() != null ? announcement.getSubjectName() : getString(R.string.scope_subject_fallback);
                    accentColorRes = R.color.role_teacher_start;
                    break;
            }
            com.example.uos_lms.core.ui.AccentColors.applyBar(itemView.findViewById(R.id.accentBar), accentColorRes);
            ((TextView) itemView.findViewById(R.id.textScope)).setText(scopeLabel);
            ((TextView) itemView.findViewById(R.id.textTitle)).setText(announcement.getTitle());
            ((TextView) itemView.findViewById(R.id.textBody)).setText(announcement.getBody());
            ((TextView) itemView.findViewById(R.id.textAuthorDate)).setText(getString(R.string.author_date_format,
                    announcement.getAuthorName(), DateKeyUtils.millisToDisplay(announcement.getCreatedAt())));

            ImageButton buttonDelete = itemView.findViewById(R.id.buttonDelete);
            boolean canDelete = latestState != null && latestState.getRole() == UserRole.ADMIN;
            buttonDelete.setVisibility(canDelete ? View.VISIBLE : View.GONE);
            buttonDelete.setOnClickListener(v -> viewModel.deleteAnnouncement(announcement.getId()));
        });
        recyclerList.setAdapter(adapter);

        view.findViewById(R.id.fabAdd).setOnClickListener(v -> {
            if (latestState != null) showPostDialog(latestState);
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            latestState = state;
            render(view, adapter, emptyState, recyclerList, state);
        });
    }

    private void render(View view, SimpleListAdapter<Announcement> adapter, View emptyState, RecyclerView recyclerList, AnnouncementsUiState state) {
        UserRole role = state.getRole();
        if (role != null) {
            view.findViewById(R.id.toolbar).setBackgroundResource(bgDrawableForRole(role));
        }

        boolean canPost = role == UserRole.ADMIN || role == UserRole.HOD || role == UserRole.TEACHER;
        view.findViewById(R.id.fabAdd).setVisibility(canPost ? View.VISIBLE : View.GONE);

        boolean hasAnnouncements = !state.getAnnouncements().isEmpty();
        emptyState.setVisibility(hasAnnouncements || state.isLoading() ? View.GONE : View.VISIBLE);
        recyclerList.setVisibility(hasAnnouncements ? View.VISIBLE : View.GONE);
        adapter.submitList(state.getAnnouncements());

        if (state.getErrorMessage() != null) {
            Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
            viewModel.clearError();
        }
    }

    private int bgDrawableForRole(UserRole role) {
        switch (role) {
            case HOD:
                return R.drawable.bg_hod_gradient;
            case TEACHER:
                return R.drawable.bg_teacher_gradient;
            case STUDENT:
                return R.drawable.bg_student_gradient;
            default:
                return R.drawable.bg_auth_gradient;
        }
    }

    private void showPostDialog(AnnouncementsUiState state) {
        UserRole role = state.getRole();
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_post_announcement, null);
        TextInputEditText editTitle = dialogView.findViewById(R.id.editTitle);
        TextInputEditText editBody = dialogView.findViewById(R.id.editBody);
        TextView textError = dialogView.findViewById(R.id.textError);

        View adminSection = dialogView.findViewById(R.id.adminAudienceSection);
        Chip chipAllUsers = dialogView.findViewById(R.id.chipAllUsers);
        Chip chipChooseDepartment = dialogView.findViewById(R.id.chipChooseDepartment);
        TextView textHodAudience = dialogView.findViewById(R.id.textHodAudience);
        View teacherSection = dialogView.findViewById(R.id.teacherAudienceSection);
        Chip chipChooseSubject = dialogView.findViewById(R.id.chipChooseSubject);

        Department[] selectedDepartment = {null};
        Subject[] selectedSubject = {null};

        if (role == UserRole.ADMIN) {
            adminSection.setVisibility(View.VISIBLE);
            chipAllUsers.setChecked(true);
            chipAllUsers.setOnClickListener(v -> {
                selectedDepartment[0] = null;
                chipAllUsers.setChecked(true);
                chipChooseDepartment.setChecked(false);
                chipChooseDepartment.setText(R.string.choose_department_option);
            });
            chipChooseDepartment.setOnClickListener(v -> SelectDialogHelper.show(requireContext(),
                    getString(R.string.choose_department_title), state.getDepartments(), Department::getName,
                    getString(R.string.no_departments_yet), department -> {
                        selectedDepartment[0] = department;
                        chipAllUsers.setChecked(false);
                        chipChooseDepartment.setChecked(true);
                        chipChooseDepartment.setText(department.getName());
                    }));
        } else if (role == UserRole.HOD) {
            textHodAudience.setVisibility(View.VISIBLE);
            textHodAudience.setText(getString(R.string.posting_to_format,
                    state.getMyDepartmentName() != null ? state.getMyDepartmentName() : getString(R.string.your_department_fallback)));
        } else if (role == UserRole.TEACHER) {
            teacherSection.setVisibility(View.VISIBLE);
            chipChooseSubject.setOnClickListener(v -> SelectDialogHelper.show(requireContext(),
                    getString(R.string.choose_subject_option), state.getMySubjects(),
                    subject -> subject.getCode() + " • " + subject.getTitle(),
                    getString(R.string.no_assigned_subjects_yet), subject -> {
                        selectedSubject[0] = subject;
                        chipChooseSubject.setChecked(true);
                        chipChooseSubject.setText(subject.getTitle());
                    }));
        }

        Dialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.post_announcement_title)
                .setView(dialogView)
                .setPositiveButton(R.string.post_button, null)
                .setNegativeButton(R.string.cancel_button, null)
                .show();

        dialog.findViewById(android.R.id.button1).setOnClickListener(v -> {
            String title = editTitle.getText() == null ? "" : editTitle.getText().toString().trim();
            String body = editBody.getText() == null ? "" : editBody.getText().toString().trim();
            if (title.isEmpty() || body.isEmpty()) {
                textError.setText(R.string.title_message_required);
                textError.setVisibility(View.VISIBLE);
                return;
            }
            if (role == UserRole.ADMIN) {
                if (selectedDepartment[0] == null) viewModel.postAll(title, body);
                else viewModel.postToDepartment(title, body, selectedDepartment[0]);
            } else if (role == UserRole.HOD) {
                viewModel.postToMyDepartment(title, body);
            } else if (role == UserRole.TEACHER) {
                if (selectedSubject[0] == null) {
                    textError.setText(R.string.choose_a_subject_error);
                    textError.setVisibility(View.VISIBLE);
                    return;
                }
                viewModel.postToSubject(title, body, selectedSubject[0]);
            }
            dialog.dismiss();
        });
    }
}
