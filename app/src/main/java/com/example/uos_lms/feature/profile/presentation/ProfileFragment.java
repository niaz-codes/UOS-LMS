package com.example.uos_lms.feature.profile.presentation;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import com.example.uos_lms.R;
import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.core.domain.model.UserRole;
import com.example.uos_lms.core.ui.AnimUtils;
import com.example.uos_lms.core.ui.RefreshUx;
import com.example.uos_lms.core.ui.UserAvatarHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.snackbar.Snackbar;

import java.util.Date;

import dagger.hilt.android.AndroidEntryPoint;

/** Display-only Profile: photo management (tap avatar -> pick -> crop -> preview -> upload) plus
 * role-based detail rows built dynamically per user - see bindFieldsForRole below. Editing/
 * Password/Theme/Logout live on the new Settings screen, reached via the gear icon. */
@AndroidEntryPoint
public class ProfileFragment extends Fragment {

    private ProfileViewModel viewModel;
    private ActivityResultLauncher<String> photoPicker;
    private ActivityResultLauncher<CropImageContractOptions> cropImage;
    private ActivityResultLauncher<String> photoPermissionLauncher;
    private boolean bottomNavBound;
    private boolean cardsAnimated;
    private boolean resumedOnce;
    private RefreshUx.Binding refreshBinding;

    public ProfileFragment() {
        super(R.layout.fragment_profile);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        photoPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) launchCrop(uri);
        });
        cropImage = registerForActivityResult(new CropImageContract(), result -> {
            if (result.isSuccessful()) {
                showPhotoPreview(result.getUriContent());
            } else if (result.getError() != null) {
                Snackbar.make(requireView(), R.string.crop_error_message, Snackbar.LENGTH_LONG).show();
            }
            // Neither successful nor an error means the user cancelled the crop screen - nothing to do.
        });
        photoPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) {
                photoPicker.launch("image/*");
            } else {
                Snackbar.make(requireView(), R.string.photo_permission_denied_message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    /** GetContent() itself never needs a storage/media permission on stock Android (the system
     * picker runs out-of-process and hands back a temporary read grant) - but some OEM skins
     * route photo picking through a legacy file browser that does expect the calling app to
     * already hold read access, silently failing (or hanging) otherwise. Requesting it explicitly
     * here is defensive and harmless on devices where it was never actually required. */
    private void launchPhotoPicker() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            photoPicker.launch("image/*");
        } else {
            photoPermissionLauncher.launch(permission);
        }
    }

    /** Square, aspect-locked crop (zoom/pan handled by the library itself). The library's own
     * crop screen provides Crop/Cancel as a toolbar checkmark (confirm) and back arrow (cancel) -
     * both render using this Fragment's own theme colors here, since the crop screen is a
     * separate Activity with no knowledge of Theme.UOS_LMS otherwise and would default to
     * colors that can end up low-contrast (effectively invisible) against its own background. */
    // A raw camera photo can be tens of megabytes; capping the crop's own output resolution
    // means every downstream step (preview, upload) only ever deals with a small file,
    // regardless of how large the original picked image was.
    private static final int PROFILE_PHOTO_MAX_DIMENSION_PX = 800;
    private static final int PROFILE_PHOTO_JPEG_QUALITY = 85;

    private void launchCrop(Uri sourceUri) {
        int primaryColor = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimary, android.graphics.Color.BLACK);
        int onPrimaryColor = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnPrimary, android.graphics.Color.WHITE);

        CropImageOptions options = new CropImageOptions();
        options.fixAspectRatio = true;
        options.aspectRatioX = 1;
        options.aspectRatioY = 1;
        options.cropShape = CropImageView.CropShape.RECTANGLE;
        options.outputRequestWidth = PROFILE_PHOTO_MAX_DIMENSION_PX;
        options.outputRequestHeight = PROFILE_PHOTO_MAX_DIMENSION_PX;
        options.outputRequestSizeOptions = CropImageView.RequestSizeOptions.RESIZE_INSIDE;
        options.outputCompressFormat = android.graphics.Bitmap.CompressFormat.JPEG;
        options.outputCompressQuality = PROFILE_PHOTO_JPEG_QUALITY;
        options.activityTitle = getString(R.string.crop_photo_title);
        options.toolbarColor = primaryColor;
        options.toolbarTitleColor = onPrimaryColor;
        options.toolbarBackButtonColor = onPrimaryColor;
        options.toolbarTintColor = onPrimaryColor;
        options.activityMenuIconColor = onPrimaryColor;
        options.activityMenuTextColor = onPrimaryColor;
        cropImage.launch(new CropImageContractOptions(sourceUri, options));
    }

    private void showPhotoPreview(Uri croppedUri) {
        if (croppedUri == null) return;
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_photo_preview, null);
        Glide.with(this)
                .load(croppedUri)
                .circleCrop()
                .into((ShapeableImageView) dialogView.findViewById(R.id.imagePreview));

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.photo_preview_title)
                .setView(dialogView)
                .setPositiveButton(R.string.upload_button, (d, w) -> viewModel.updatePhoto(croppedUri))
                .setNegativeButton(R.string.cancel_button, null)
                .show();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.contentContainer);
        refreshBinding = RefreshUx.bindMenuItem(toolbar.getMenu().findItem(R.id.actionRefresh), swipeRefresh, () -> viewModel.refresh());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.actionRefresh) {
                refreshBinding.trigger();
                return true;
            }
            if (item.getItemId() == R.id.actionSettings) {
                NavHostFragment.findNavController(this).navigate(R.id.settingsFragment);
                return true;
            }
            return false;
        });

        view.findViewById(R.id.imageAvatar).setOnClickListener(v -> launchPhotoPicker());
        view.findViewById(R.id.cameraBadge).setOnClickListener(v -> launchPhotoPicker());
        view.findViewById(R.id.buttonEditPhoto).setOnClickListener(v -> launchPhotoPicker());
        view.findViewById(R.id.buttonRemovePhoto).setOnClickListener(v -> viewModel.removePhoto());

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> render(view, state));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (resumedOnce) viewModel.refresh();
        resumedOnce = true;
    }

    private void render(View view, ProfileUiState state) {
        ProgressBar progressLoading = view.findViewById(R.id.progressLoading);
        View contentContainer = view.findViewById(R.id.contentContainer);
        User user = state.getUser();

        progressLoading.setVisibility(state.isLoading() || user == null ? View.VISIBLE : View.GONE);
        contentContainer.setVisibility(!state.isLoading() && user != null ? View.VISIBLE : View.GONE);
        if (state.isLoading() || user == null) return;

        UserAvatarHelper.bind(view.findViewById(R.id.imageAvatar), user.getProfilePhotoUrl());
        ((TextView) view.findViewById(R.id.textFullName)).setText(user.getFullName());
        ((TextView) view.findViewById(R.id.textRole)).setText(user.getRole().name());
        view.findViewById(R.id.profileHeader).setBackgroundResource(roleGradientRes(user.getRole()));
        view.findViewById(R.id.buttonRemovePhoto).setVisibility(user.getProfilePhotoUrl() != null ? View.VISIBLE : View.GONE);

        View uploadOverlay = view.findViewById(R.id.uploadOverlay);
        uploadOverlay.setVisibility(state.isUploadingPhoto() ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.cameraBadge).setVisibility(state.isUploadingPhoto() ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.imageAvatar).setEnabled(!state.isUploadingPhoto());
        view.findViewById(R.id.buttonEditPhoto).setEnabled(!state.isUploadingPhoto());
        view.findViewById(R.id.buttonRemovePhoto).setEnabled(!state.isUploadingPhoto());

        bindRoleAccent(view, user.getRole());
        bindPersonalInformation((LinearLayout) view.findViewById(R.id.personalRowsContainer), user, state);
        bindAcademicInformation(view, user, state);
        bindQuickStat(view, user, state);

        if (!cardsAnimated) {
            cardsAnimated = true;
            AnimUtils.fadeSlideIn(view.findViewById(R.id.floatingStatsRow), 80L);
            AnimUtils.fadeSlideIn(view.findViewById(R.id.cardsContainer), 160L);
        }

        refreshBinding.setRefreshing(state.isRefreshing());
        if (state.getErrorMessage() != null) {
            Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
            viewModel.clearError();
        }

        if (!bottomNavBound) {
            bottomNavBound = true;
            bindBottomNav(view.findViewById(R.id.bottomNav), user.getRole());
        }
    }

    /** This screen is intentionally role-agnostic (see colors.xml's "Indigo/Emerald/Amber stays
     * the neutral/default chrome for role-agnostic screens" note) - every role sees the exact
     * same Profile identity, not a red-for-Admin/green-for-Student palette. Role-based coloring
     * lives on the screens that actually need to distinguish role at a glance (bottom nav,
     * gradient toolbars, the Admin User Profile management screen); this self-service screen
     * doesn't need it. */
    private int roleGradientRes(UserRole role) {
        return R.drawable.bg_profile_gradient;
    }

    @androidx.annotation.ColorRes
    private static int roleAccentColorRes(UserRole role) {
        return R.color.indigo_primary;
    }

    @androidx.annotation.ColorRes
    private static int roleContainerColorRes(UserRole role) {
        return R.color.indigo_primary_container;
    }

    @androidx.annotation.ColorRes
    private static int roleOnContainerColorRes(UserRole role) {
        return R.color.on_indigo_primary_container;
    }

    /** The two section-header icons get the app's own indigo/emerald pairing - a consistent
     * identity for every role, not a per-role palette. */
    private void bindRoleAccent(View view, UserRole role) {
        view.findViewById(R.id.iconBackgroundPersonal).setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.indigo_primary)));
        view.findViewById(R.id.iconBackgroundAcademic).setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.emerald_secondary)));
    }

    /** The floating stat row: Account Status (always available, colored by the actual status)
     * plus a role-relevant second fact - assigned/enrolled subjects for Teacher/Student, or
     * membership date as a graceful fallback for HOD/Admin, who have no subject count. Every
     * role ends up with two fully populated cards rather than an empty gap. */
    private void bindQuickStat(View view, User user, ProfileUiState state) {
        bindStatCard(view.findViewById(R.id.statAccountStatus), R.drawable.ic_verified_user,
                getString(R.string.stat_account_status), user.getStatus().name(),
                statusAccentColorRes(user.getStatus()), statusContainerColorRes(user.getStatus()), statusOnContainerColorRes(user.getStatus()));

        boolean hasSubjects = state.getSubjectsCount() != null;
        String secondaryLabel = hasSubjects
                ? getString(user.getRole() == UserRole.TEACHER ? R.string.stat_assigned_subjects : R.string.stat_enrolled_subjects)
                : getString(R.string.member_since);
        String secondaryValue = hasSubjects
                ? String.valueOf(state.getSubjectsCount())
                : (user.getCreatedAt() > 0 ? DateFormat.getMediumDateFormat(requireContext()).format(new Date(user.getCreatedAt())) : "—");
        bindStatCard(view.findViewById(R.id.statSecondary), hasSubjects ? R.drawable.ic_folder_open : R.drawable.ic_calendar_month,
                secondaryLabel, secondaryValue,
                roleAccentColorRes(user.getRole()), roleContainerColorRes(user.getRole()), roleOnContainerColorRes(user.getRole()));
    }

    @androidx.annotation.ColorRes
    private static int statusAccentColorRes(com.example.uos_lms.core.domain.model.UserStatus status) {
        switch (status) {
            case PENDING: return R.color.status_warning;
            case REJECTED:
            case SUSPENDED: return R.color.error_color;
            default: return R.color.status_success;
        }
    }

    @androidx.annotation.ColorRes
    private static int statusContainerColorRes(com.example.uos_lms.core.domain.model.UserStatus status) {
        switch (status) {
            case PENDING: return R.color.amber_tertiary_container;
            case REJECTED:
            case SUSPENDED: return R.color.error_container;
            default: return R.color.student_container;
        }
    }

    @androidx.annotation.ColorRes
    private static int statusOnContainerColorRes(com.example.uos_lms.core.domain.model.UserStatus status) {
        switch (status) {
            case PENDING: return R.color.on_amber_tertiary_container;
            case REJECTED:
            case SUSPENDED: return R.color.on_error_container;
            default: return R.color.on_student_container;
        }
    }

    private void bindStatCard(View card, @androidx.annotation.DrawableRes int iconRes, String label, String value,
            @androidx.annotation.ColorRes int accentColorRes, @androidx.annotation.ColorRes int containerColorRes,
            @androidx.annotation.ColorRes int onContainerColorRes) {
        int accent = ContextCompat.getColor(requireContext(), accentColorRes);
        int container = ContextCompat.getColor(requireContext(), containerColorRes);
        int onContainer = ContextCompat.getColor(requireContext(), onContainerColorRes);

        com.google.android.material.card.MaterialCardView cardSurface = card.findViewById(R.id.cardSurface);
        cardSurface.setCardBackgroundColor(container);
        cardSurface.setStrokeColor(android.content.res.ColorStateList.valueOf(
                com.example.uos_lms.core.ui.AccentColors.withAlpha(accent, 0.35f)));

        card.findViewById(R.id.glowHalo).setBackgroundTintList(android.content.res.ColorStateList.valueOf(accent));
        card.findViewById(R.id.decorShapeLarge).setBackgroundTintList(android.content.res.ColorStateList.valueOf(onContainer));
        card.findViewById(R.id.decorShapeSmall).setBackgroundTintList(android.content.res.ColorStateList.valueOf(onContainer));

        card.findViewById(R.id.iconBackground).setBackgroundTintList(android.content.res.ColorStateList.valueOf(accent));
        android.widget.ImageView icon = card.findViewById(R.id.imageIcon);
        icon.setImageResource(iconRes);
        icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white));

        TextView textLabel = card.findViewById(R.id.textLabel);
        textLabel.setText(label);
        textLabel.setTextColor(onContainer);

        TextView textValue = card.findViewById(R.id.textValue);
        textValue.setText(value);
        textValue.setTextColor(onContainer);
        textValue.setTextSize(value.length() > 8 ? 13f : 21f);

        View textTrend = card.findViewById(R.id.textTrend);
        ((View) textTrend.getParent()).setVisibility(View.GONE);
    }

    /** Contact/identity fields every role has in common - per spec, an empty field is omitted
     * entirely rather than shown blank. */
    private void bindPersonalInformation(LinearLayout container, User user, ProfileUiState state) {
        container.removeAllViews();
        addRow(container, R.drawable.ic_person, R.string.full_name, user.getFullName(), R.color.status_info);
        addRow(container, R.drawable.ic_person, R.string.father_name, user.getFatherName(), R.color.role_hod_start);
        addRow(container, R.drawable.ic_fingerprint, R.string.cnic, user.getCnic(), R.color.indigo_primary);
        addRow(container, R.drawable.ic_phone, R.string.phone, user.getPhone(), R.color.role_student_start);
        addRow(container, R.drawable.ic_email, R.string.email, user.getEmail(), R.color.amber_tertiary);
        // Account Status is already shown in the floating stat card at the top of the screen -
        // no need to repeat it as a row here too.
    }

    /** Role-specific academic/professional identity - hidden entirely for Admin, which has no
     * academic context. Student sees department/session/semester/registration/roll/enrolled
     * subjects, Teacher sees department(s)/employee id/designation/assigned subjects, HOD sees
     * department + designation. */
    private void bindAcademicInformation(View view, User user, ProfileUiState state) {
        View card = view.findViewById(R.id.cardAcademic);
        if (user.getRole() == UserRole.ADMIN) {
            card.setVisibility(View.GONE);
            return;
        }
        card.setVisibility(View.VISIBLE);

        LinearLayout container = view.findViewById(R.id.academicRowsContainer);
        container.removeAllViews();
        switch (user.getRole()) {
            case STUDENT:
                addRow(container, R.drawable.ic_apartment, R.string.label_department, state.getDepartmentName(), R.color.status_info);
                addRow(container, R.drawable.ic_calendar_month, R.string.stat_session, state.getSessionLabel(), R.color.role_admin_start);
                addRow(container, R.drawable.ic_school, R.string.label_semester, state.getSemesterLabel(), R.color.emerald_secondary);
                addRow(container, R.drawable.ic_badge, R.string.stat_registration_number, user.getRegistrationNumber(), R.color.indigo_primary);
                addRow(container, R.drawable.ic_badge, R.string.stat_roll_number, user.getRollNumber(), R.color.role_hod_start);
                addRow(container, R.drawable.ic_folder_open, R.string.stat_enrolled_subjects, countText(state.getSubjectsCount()), R.color.amber_tertiary);
                break;
            case TEACHER:
                addRow(container, R.drawable.ic_apartment, R.string.label_department, state.getDepartmentName(), R.color.status_info);
                addRow(container, R.drawable.ic_badge, R.string.stat_employee_id, user.getEmployeeId(), R.color.emerald_secondary);
                addRow(container, R.drawable.ic_person_role, R.string.stat_designation, user.getDesignation(), R.color.role_hod_start);
                addRow(container, R.drawable.ic_folder_open, R.string.stat_assigned_subjects, countText(state.getSubjectsCount()), R.color.amber_tertiary);
                break;
            case HOD:
                addRow(container, R.drawable.ic_apartment, R.string.label_department, state.getDepartmentName(), R.color.status_info);
                addRow(container, R.drawable.ic_person_role, R.string.stat_designation, user.getDesignation(), R.color.role_hod_start);
                break;
            default:
                break;
        }
        card.setVisibility(container.getChildCount() == 0 ? View.GONE : View.VISIBLE);
    }

    @Nullable
    private String countText(Integer count) {
        return count != null ? String.valueOf(count) : null;
    }

    /** Each row gets its own distinct icon color (the classic "colorful settings list" pattern)
     * rather than one flat accent repeated down the whole list - only the icon badge varies,
     * everything else (typography, card, header) stays the unified indigo identity. */
    private void addRow(LinearLayout container, int iconRes, int labelRes, @Nullable String value, @androidx.annotation.ColorRes int accentColorRes) {
        if (value == null || value.isBlank()) return;
        View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_profile_detail_row, container, false);
        row.findViewById(R.id.iconBackgroundRow).setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), accentColorRes)));
        ((android.widget.ImageView) row.findViewById(R.id.imageRowIcon)).setImageResource(iconRes);
        ((TextView) row.findViewById(R.id.textLabel)).setText(labelRes);
        ((TextView) row.findViewById(R.id.textValue)).setText(value);
        container.addView(row);
    }

    private void bindBottomNav(BottomNavigationView bottomNav, UserRole role) {
        int menuRes;
        switch (role) {
            case ADMIN:
                menuRes = R.menu.menu_admin_bottom_nav;
                break;
            case HOD:
                menuRes = R.menu.menu_hod_bottom_nav;
                break;
            case TEACHER:
                menuRes = R.menu.menu_teacher_bottom_nav;
                break;
            default:
                menuRes = R.menu.menu_student_bottom_nav;
                break;
        }
        bottomNav.getMenu().clear();
        bottomNav.inflateMenu(menuRes);
        bottomNav.setSelectedItemId(R.id.navProfile);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navProfile) return true;
            navigateFromProfile(role, id);
            return false;
        });
    }

    private void navigateFromProfile(UserRole role, int itemId) {
        androidx.navigation.NavController navController = NavHostFragment.findNavController(this);
        if (itemId == R.id.navHome) {
            switch (role) {
                case ADMIN:
                    navController.popBackStack(R.id.adminHomeFragment, false);
                    return;
                case HOD:
                    navController.popBackStack(R.id.hodDashboardFragment, false);
                    return;
                case TEACHER:
                    navController.popBackStack(R.id.teacherDashboardFragment, false);
                    return;
                default:
                    navController.popBackStack(R.id.studentDashboardFragment, false);
                    return;
            }
        }
        if (role == UserRole.ADMIN) {
            if (itemId == R.id.navResult) navController.navigate(R.id.adminResultsDepartmentListFragment);
            else if (itemId == R.id.navDepartments) navController.navigate(R.id.departmentListFragment);
            else if (itemId == R.id.navReports) navController.navigate(R.id.adminReportsFragment);
        } else if (role == UserRole.HOD) {
            if (itemId == R.id.navTeachers) navController.navigate(R.id.hodTeachersFragment);
            else if (itemId == R.id.navStudents) navController.navigate(R.id.hodStudentsFragment);
            else if (itemId == R.id.navReports) navController.navigate(R.id.hodReportsFragment);
        } else if (role == UserRole.TEACHER) {
            if (itemId == R.id.navStudents) navController.navigate(R.id.teacherStudentsFragment);
            else if (itemId == R.id.navAttendance) navController.navigate(R.id.teacherAttendanceReportsFragment);
            else if (itemId == R.id.navExamResult) navController.navigate(R.id.teacherExamResultWorkspaceFragment);
        } else {
            if (itemId == R.id.navAttendance) navController.navigate(R.id.studentAttendanceFragment);
            else if (itemId == R.id.navSubmissions) navController.navigate(R.id.studentSubmissionsFragment);
            else if (itemId == R.id.navResult) navController.navigate(R.id.studentResultsFragment);
        }
    }
}
