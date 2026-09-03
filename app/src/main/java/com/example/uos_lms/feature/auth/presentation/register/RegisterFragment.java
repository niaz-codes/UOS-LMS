package com.example.uos_lms.feature.auth.presentation.register;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import com.example.uos_lms.R;
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.Session;
import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.core.domain.model.UserRole;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class RegisterFragment extends Fragment {

    private static final int PHOTO_MAX_DIMENSION_PX = 800;
    private static final int PHOTO_JPEG_QUALITY = 85;

    private RegisterViewModel viewModel;
    private ActivityResultLauncher<String> photoPicker;
    private ActivityResultLauncher<CropImageContractOptions> cropImage;
    private ActivityResultLauncher<String> photoPermissionLauncher;

    private ShapeableImageView imagePhotoPreview;
    private TextView textAddPhoto;
    private TextView buttonRemovePhoto;

    private TextInputLayout layoutFullName;
    private TextInputLayout layoutFatherName;
    private TextInputLayout layoutCnic;
    private TextInputLayout layoutPhone;
    private TextInputLayout layoutEmail;
    private TextInputLayout layoutPassword;
    private TextInputLayout layoutConfirmPassword;
    private TextInputEditText editFullName;
    private TextInputEditText editFatherName;
    private TextInputEditText editCnic;
    private TextInputEditText editPhone;
    private TextInputEditText editEmail;
    private TextInputEditText editPassword;
    private TextInputEditText editConfirmPassword;

    private MaterialCardView chipStudent;
    private MaterialCardView chipTeacher;
    private MaterialCardView chipHod;

    private MaterialButton buttonSubmit;
    private CircularProgressIndicator progressSubmit;
    private android.widget.LinearLayout rowSubmitError;
    private TextView textSubmitError;

    private View sectionStudentAcademic;
    private View sectionTeacherAcademic;
    private View sectionHodAcademic;

    private OptionTabGroup tabsStudentDepartment;
    private OptionTabGroup tabsStudentSession;
    private OptionTabGroup tabsStudentSemester;
    private OptionTabGroup tabsTeacherDepartment;
    private OptionTabGroup tabsHodDepartment;

    private CircularProgressIndicator progressStudentDepartment;
    private CircularProgressIndicator progressStudentSession;
    private CircularProgressIndicator progressStudentSemester;
    private CircularProgressIndicator progressTeacherDepartment;
    private CircularProgressIndicator progressHodDepartment;

    private android.widget.LinearLayout errorRowStudentDepartment;
    private android.widget.LinearLayout errorRowStudentSession;
    private android.widget.LinearLayout errorRowStudentSemester;
    private android.widget.LinearLayout errorRowTeacherDepartment;
    private android.widget.LinearLayout errorRowHodDepartment;

    private TextView textErrorStudentDepartment;
    private TextView textErrorStudentSession;
    private TextView textErrorStudentSemester;
    private TextView textErrorTeacherDepartment;
    private TextView textErrorHodDepartment;

    private boolean suppressWatchers;

    public RegisterFragment() {
        super(R.layout.fragment_register);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        photoPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) launchCrop(uri);
        });
        cropImage = registerForActivityResult(new CropImageContract(), result -> {
            if (result.isSuccessful()) {
                viewModel.onPhotoPicked(result.getUriContent());
            } else if (result.getError() != null) {
                Snackbar.make(requireView(), R.string.crop_error_message, Snackbar.LENGTH_LONG).show();
            }
        });
        photoPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) {
                photoPicker.launch("image/*");
            } else {
                Snackbar.make(requireView(), R.string.photo_permission_denied_message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

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

    private void launchCrop(Uri sourceUri) {
        int primaryColor = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimary, android.graphics.Color.BLACK);
        int onPrimaryColor = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnPrimary, android.graphics.Color.WHITE);

        CropImageOptions options = new CropImageOptions();
        options.fixAspectRatio = true;
        options.aspectRatioX = 1;
        options.aspectRatioY = 1;
        options.cropShape = CropImageView.CropShape.RECTANGLE;
        options.outputRequestWidth = PHOTO_MAX_DIMENSION_PX;
        options.outputRequestHeight = PHOTO_MAX_DIMENSION_PX;
        options.outputRequestSizeOptions = CropImageView.RequestSizeOptions.RESIZE_INSIDE;
        options.outputCompressFormat = android.graphics.Bitmap.CompressFormat.JPEG;
        options.outputCompressQuality = PHOTO_JPEG_QUALITY;
        options.activityTitle = getString(R.string.crop_photo_title);
        options.toolbarColor = primaryColor;
        options.toolbarTitleColor = onPrimaryColor;
        options.toolbarBackButtonColor = onPrimaryColor;
        options.toolbarTintColor = onPrimaryColor;
        options.activityMenuIconColor = onPrimaryColor;
        options.activityMenuTextColor = onPrimaryColor;
        cropImage.launch(new CropImageContractOptions(sourceUri, options));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);

        imagePhotoPreview = view.findViewById(R.id.imagePhotoPreview);
        textAddPhoto = view.findViewById(R.id.textAddPhoto);
        buttonRemovePhoto = view.findViewById(R.id.buttonRemovePhoto);
        View cameraBadgeRegister = view.findViewById(R.id.cameraBadgeRegister);

        imagePhotoPreview.setOnClickListener(v -> launchPhotoPicker());
        cameraBadgeRegister.setOnClickListener(v -> launchPhotoPicker());
        textAddPhoto.setOnClickListener(v -> launchPhotoPicker());
        buttonRemovePhoto.setOnClickListener(v -> viewModel.onPhotoRemoved());

        layoutFullName = view.findViewById(R.id.layoutFullName);
        layoutFatherName = view.findViewById(R.id.layoutFatherName);
        layoutCnic = view.findViewById(R.id.layoutCnic);
        layoutPhone = view.findViewById(R.id.layoutPhone);
        layoutEmail = view.findViewById(R.id.layoutEmail);
        layoutPassword = view.findViewById(R.id.layoutPassword);
        layoutConfirmPassword = view.findViewById(R.id.layoutConfirmPassword);
        editFullName = view.findViewById(R.id.editFullName);
        editFatherName = view.findViewById(R.id.editFatherName);
        editCnic = view.findViewById(R.id.editCnic);
        editPhone = view.findViewById(R.id.editPhone);
        editEmail = view.findViewById(R.id.editEmail);
        editPassword = view.findViewById(R.id.editPassword);
        editConfirmPassword = view.findViewById(R.id.editConfirmPassword);

        chipStudent = view.findViewById(R.id.chipStudent);
        chipTeacher = view.findViewById(R.id.chipTeacher);
        chipHod = view.findViewById(R.id.chipHod);

        buttonSubmit = view.findViewById(R.id.buttonSubmit);
        progressSubmit = view.findViewById(R.id.progressSubmit);
        rowSubmitError = view.findViewById(R.id.rowSubmitError);
        textSubmitError = view.findViewById(R.id.textSubmitError);
        MaterialButton buttonBackToLogin = view.findViewById(R.id.buttonBackToLogin);

        editFullName.addTextChangedListener(watcher(viewModel::onFullNameChange));
        editFatherName.addTextChangedListener(watcher(viewModel::onFatherNameChange));
        editCnic.addTextChangedListener(watcher(viewModel::onCnicChange));
        editPhone.addTextChangedListener(watcher(viewModel::onPhoneChange));
        editEmail.addTextChangedListener(watcher(viewModel::onEmailChange));
        editPassword.addTextChangedListener(watcher(viewModel::onPasswordChange));
        editConfirmPassword.addTextChangedListener(watcher(viewModel::onConfirmPasswordChange));

        chipStudent.setOnClickListener(v -> viewModel.onRoleChange(UserRole.STUDENT));
        chipTeacher.setOnClickListener(v -> viewModel.onRoleChange(UserRole.TEACHER));
        chipHod.setOnClickListener(v -> viewModel.onRoleChange(UserRole.HOD));

        bindAcademicOptions(view);

        buttonSubmit.setOnClickListener(v -> viewModel.submit());
        buttonBackToLogin.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);
        viewModel.getRegistrationSubmitted().observe(getViewLifecycleOwner(), user -> {
            if (user != null) showRegistrationSuccess(user);
        });
    }

    /** The backend generates the student's Registration Number at signup - surface it here since
     * the applicant would otherwise never see it. A student who picked a department + session on
     * the form also receives their Roll Number immediately; for everyone else it stays "Not
     * assigned" until the Admin handles their placement. */
    private void showRegistrationSuccess(User user) {
        String registrationNumber = user.getRegistrationNumber() != null
                ? user.getRegistrationNumber() : getString(R.string.not_assigned);
        String rollNumber = user.getRollNumber() != null
                ? user.getRollNumber() : getString(R.string.not_assigned);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.registration_submitted_title)
                .setMessage(getString(R.string.registration_submitted_message, registrationNumber, rollNumber))
                .setCancelable(false)
                .setPositiveButton(R.string.done, (d, w) -> {
                    NavOptions options = new NavOptions.Builder().setPopUpTo(R.id.registerFragment, true).build();
                    NavHostFragment.findNavController(this).navigate(R.id.loginFragment, null, options);
                })
                .show();
    }

    private void render(RegisterUiState state) {
        renderPhoto(state.getPhotoUri());

        suppressWatchers = true;
        setIfChanged(editFullName, state.getFullName());
        setIfChanged(editFatherName, state.getFatherName());
        setIfChanged(editCnic, state.getCnic());
        setIfChanged(editPhone, state.getPhone());
        setIfChanged(editEmail, state.getEmail());
        setIfChanged(editPassword, state.getPassword());
        setIfChanged(editConfirmPassword, state.getConfirmPassword());
        suppressWatchers = false;

        layoutFullName.setError(state.getFullNameError());
        layoutFatherName.setError(state.getFatherNameError());
        layoutCnic.setError(state.getCnicError());
        layoutPhone.setError(state.getPhoneError());
        layoutEmail.setError(state.getEmailError());
        layoutPassword.setError(state.getPasswordError());
        layoutConfirmPassword.setError(state.getConfirmPasswordError());

        updateRoleSelection(state.getRole());
        renderAcademicSections(state);

        boolean loading = state.isLoading();
        boolean formEnabled = !loading;
        editFullName.setEnabled(formEnabled);
        editFatherName.setEnabled(formEnabled);
        editCnic.setEnabled(formEnabled);
        editPhone.setEnabled(formEnabled);
        editEmail.setEnabled(formEnabled);
        editPassword.setEnabled(formEnabled);
        editConfirmPassword.setEnabled(formEnabled);
        chipStudent.setEnabled(formEnabled);
        chipTeacher.setEnabled(formEnabled);
        chipHod.setEnabled(formEnabled);
        imagePhotoPreview.setEnabled(formEnabled);
        textAddPhoto.setEnabled(formEnabled);
        buttonRemovePhoto.setEnabled(formEnabled);
        setAcademicEnabled(formEnabled);
        buttonSubmit.setEnabled(formEnabled);
        buttonSubmit.setText(loading ? "" : getString(R.string.create_account_button));
        progressSubmit.setVisibility(loading ? View.VISIBLE : View.GONE);

        if (state.getSubmitError() != null) {
            rowSubmitError.setVisibility(View.VISIBLE);
            textSubmitError.setText(state.getSubmitError());
        } else {
            rowSubmitError.setVisibility(View.GONE);
        }
    }

    private void bindAcademicOptions(View view) {
        sectionStudentAcademic = view.findViewById(R.id.sectionStudentAcademic);
        sectionTeacherAcademic = view.findViewById(R.id.sectionTeacherAcademic);
        sectionHodAcademic = view.findViewById(R.id.sectionHodAcademic);

        tabsStudentDepartment = view.findViewById(R.id.tabsStudentDepartment);
        tabsStudentSession = view.findViewById(R.id.tabsStudentSession);
        tabsStudentSemester = view.findViewById(R.id.tabsStudentSemester);
        tabsTeacherDepartment = view.findViewById(R.id.tabsTeacherDepartment);
        tabsHodDepartment = view.findViewById(R.id.tabsHodDepartment);

        progressStudentDepartment = view.findViewById(R.id.progressStudentDepartment);
        progressStudentSession = view.findViewById(R.id.progressStudentSession);
        progressStudentSemester = view.findViewById(R.id.progressStudentSemester);
        progressTeacherDepartment = view.findViewById(R.id.progressTeacherDepartment);
        progressHodDepartment = view.findViewById(R.id.progressHodDepartment);

        errorRowStudentDepartment = view.findViewById(R.id.errorRowStudentDepartment);
        errorRowStudentSession = view.findViewById(R.id.errorRowStudentSession);
        errorRowStudentSemester = view.findViewById(R.id.errorRowStudentSemester);
        errorRowTeacherDepartment = view.findViewById(R.id.errorRowTeacherDepartment);
        errorRowHodDepartment = view.findViewById(R.id.errorRowHodDepartment);

        textErrorStudentDepartment = view.findViewById(R.id.textErrorStudentDepartment);
        textErrorStudentSession = view.findViewById(R.id.textErrorStudentSession);
        textErrorStudentSemester = view.findViewById(R.id.textErrorStudentSemester);
        textErrorTeacherDepartment = view.findViewById(R.id.textErrorTeacherDepartment);
        textErrorHodDepartment = view.findViewById(R.id.textErrorHodDepartment);

        tabsStudentDepartment.setSingleSelect(true);
        tabsStudentSession.setSingleSelect(true);
        tabsStudentSemester.setSingleSelect(true);
        tabsTeacherDepartment.setSingleSelect(false);
        tabsHodDepartment.setSingleSelect(true);

        tabsStudentDepartment.setListener((group, ids) -> viewModel.onStudentDepartmentSelected(first(ids)));
        tabsStudentSession.setListener((group, ids) -> viewModel.onStudentSessionSelected(first(ids)));
        tabsStudentSemester.setListener((group, ids) -> viewModel.onStudentSemesterSelected(first(ids)));
        tabsTeacherDepartment.setListener((group, ids) -> viewModel.onTeacherDepartmentsSelected(ids));
        tabsHodDepartment.setListener((group, ids) -> viewModel.onHodDepartmentSelected(first(ids)));

        errorRowStudentDepartment.setOnClickListener(v -> viewModel.retryLoadDepartments());
        errorRowStudentSession.setOnClickListener(v -> viewModel.retryLoadStudentSelection());
        errorRowStudentSemester.setOnClickListener(v -> viewModel.retryLoadStudentSelection());
        errorRowTeacherDepartment.setOnClickListener(v -> viewModel.retryLoadDepartments());
        errorRowHodDepartment.setOnClickListener(v -> viewModel.retryLoadDepartments());
    }

    private void renderAcademicSections(RegisterUiState state) {
        UserRole role = state.getRole();
        boolean showStudent = role == UserRole.STUDENT;
        boolean showTeacher = role == UserRole.TEACHER;
        boolean showHod = role == UserRole.HOD;
        sectionStudentAcademic.setVisibility(showStudent ? View.VISIBLE : View.GONE);
        sectionTeacherAcademic.setVisibility(showTeacher ? View.VISIBLE : View.GONE);
        sectionHodAcademic.setVisibility(showHod ? View.VISIBLE : View.GONE);

        boolean departmentsLoading = state.isDepartmentsLoading();
        String departmentsError = state.getDepartmentsError();
        boolean selectionLoading = state.isSelectionLoading();
        String selectionError = state.getSelectionError();

        List<OptionTabGroup.Option> departments = departmentOptions(state.getDepartments());

        renderOptionsGroup(tabsStudentDepartment, progressStudentDepartment, errorRowStudentDepartment,
                departmentsLoading, departmentsError, departments);
        renderOptionsGroup(tabsTeacherDepartment, progressTeacherDepartment, errorRowTeacherDepartment,
                departmentsLoading, departmentsError, departments);
        renderOptionsGroup(tabsHodDepartment, progressHodDepartment, errorRowHodDepartment,
                departmentsLoading, departmentsError, departments);

        renderOptionsGroup(tabsStudentSession, progressStudentSession, errorRowStudentSession,
                selectionLoading, selectionError, sessionOptions(state.getStudentSessions()));
        renderOptionsGroup(tabsStudentSemester, progressStudentSemester, errorRowStudentSemester,
                selectionLoading, selectionError, semesterOptions(state.getStudentSemesters()));

        tabsStudentDepartment.setSelectedIds(singleOrEmpty(state.getStudentDepartmentId()));
        tabsStudentSession.setSelectedIds(singleOrEmpty(state.getStudentSessionId()));
        tabsStudentSemester.setSelectedIds(singleOrEmpty(state.getStudentSemesterId()));
        tabsTeacherDepartment.setSelectedIds(state.getTeacherDepartmentIds());
        tabsHodDepartment.setSelectedIds(singleOrEmpty(state.getHodDepartmentId()));

        renderSectionError(textErrorStudentDepartment, state.getDepartmentError());
        renderSectionError(textErrorStudentSession, state.getSessionError());
        renderSectionError(textErrorStudentSemester, state.getSemesterError());
        renderSectionError(textErrorTeacherDepartment, state.getDepartmentError());
        renderSectionError(textErrorHodDepartment, state.getDepartmentError());
    }

    private void renderOptionsGroup(OptionTabGroup tabs, CircularProgressIndicator progress,
                                    android.widget.LinearLayout errorRow, boolean loading,
                                    String error, List<OptionTabGroup.Option> options) {
        tabs.setOptions(options);
        boolean showTabs = !loading && !options.isEmpty();
        tabs.setVisibility(showTabs ? View.VISIBLE : View.GONE);
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        errorRow.setVisibility(!loading && error != null ? View.VISIBLE : View.GONE);
    }

    private void renderSectionError(TextView textError, String error) {
        if (error != null) {
            textError.setText(error);
            textError.setVisibility(View.VISIBLE);
        } else {
            textError.setVisibility(View.GONE);
        }
    }

    private void setAcademicEnabled(boolean enabled) {
        tabsStudentDepartment.setEnabled(enabled);
        tabsStudentSession.setEnabled(enabled);
        tabsStudentSemester.setEnabled(enabled);
        tabsTeacherDepartment.setEnabled(enabled);
        tabsHodDepartment.setEnabled(enabled);
    }

    private static List<OptionTabGroup.Option> departmentOptions(List<Department> departments) {
        List<OptionTabGroup.Option> options = new ArrayList<>();
        if (departments != null) {
            for (Department department : departments) {
                options.add(new OptionTabGroup.Option(department.getId(), department.getName()));
            }
        }
        return options;
    }

    private static List<OptionTabGroup.Option> sessionOptions(List<Session> sessions) {
        List<OptionTabGroup.Option> options = new ArrayList<>();
        if (sessions != null) {
            for (Session session : sessions) {
                options.add(new OptionTabGroup.Option(session.getId(), session.getLabel()));
            }
        }
        return options;
    }

    private static List<OptionTabGroup.Option> semesterOptions(List<Semester> semesters) {
        List<OptionTabGroup.Option> options = new ArrayList<>();
        if (semesters != null) {
            for (Semester semester : semesters) {
                options.add(new OptionTabGroup.Option(semester.getId(), semester.getDisplayName()));
            }
        }
        return options;
    }

    private static List<String> singleOrEmpty(@Nullable String id) {
        return id == null || id.isEmpty() ? Collections.<String>emptyList() : Collections.singletonList(id);
    }

    @Nullable
    private static String first(List<String> ids) {
        return (ids == null || ids.isEmpty()) ? null : ids.get(0);
    }

    /** Shows the picked+cropped photo in the form itself (not just a one-off confirmation
     * dialog) so it stays visible as a preview while the user fills out the rest of the form. */
    private void renderPhoto(@Nullable Uri photoUri) {
        if (photoUri != null) {
            Glide.with(this).load(photoUri).circleCrop().into(imagePhotoPreview);
            textAddPhoto.setText(R.string.change_photo_label);
            buttonRemovePhoto.setVisibility(View.VISIBLE);
        } else {
            imagePhotoPreview.setImageResource(R.drawable.ic_person);
            textAddPhoto.setText(R.string.add_profile_photo_label);
            buttonRemovePhoto.setVisibility(View.GONE);
        }
    }

    private void updateRoleSelection(UserRole role) {
        applyChipState(chipStudent, role == UserRole.STUDENT, R.color.role_student_start);
        applyChipState(chipTeacher, role == UserRole.TEACHER, R.color.role_teacher_start);
        applyChipState(chipHod, role == UserRole.HOD, R.color.role_hod_start);
    }

    private void applyChipState(MaterialCardView card, boolean selected, int selectedColorRes) {
        if (selected) {
            card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), selectedColorRes));
            card.setStrokeColor(ContextCompat.getColor(requireContext(), android.R.color.white));
        } else {
            card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white_alpha_14));
            card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.white_alpha_22));
        }
    }

    private void setIfChanged(TextInputEditText editText, String value) {
        String current = editText.getText() == null ? "" : editText.getText().toString();
        if (!current.equals(value)) {
            editText.setText(value);
            if (editText.getText() != null) editText.setSelection(editText.getText().length());
        }
    }

    private TextWatcher watcher(java.util.function.Consumer<String> onChanged) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!suppressWatchers) onChanged.accept(s.toString());
            }
        };
    }
}
