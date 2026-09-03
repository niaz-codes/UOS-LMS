package com.example.uos_lms.feature.teacher.presentation.material;

import android.content.Intent;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.uos_lms.R;
import com.example.uos_lms.core.common.DateKeyUtils;
import com.example.uos_lms.core.domain.model.MaterialType;
import com.example.uos_lms.core.domain.model.StudyMaterial;
import com.example.uos_lms.core.ui.ConfirmDialogHelper;
import com.example.uos_lms.core.ui.RefreshUx;
import com.example.uos_lms.core.ui.SelectDialogHelper;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Arrays;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TeacherSubjectMaterialsFragment extends Fragment {

    /** New uploads only ever pick from these - PPT/NOTE stay in the MaterialType enum purely
     * for backward compatibility with any pre-existing material (see MaterialType.java). */
    private static final List<MaterialType> SELECTABLE_TYPES =
            Arrays.asList(MaterialType.PDF, MaterialType.VIDEO, MaterialType.DOCUMENT, MaterialType.IMAGE, MaterialType.OTHER);

    private TeacherSubjectMaterialsViewModel viewModel;
    private ActivityResultLauncher<String> filePicker;

    private MaterialType selectedType = MaterialType.PDF;
    private Uri pickedFileUri;
    private MaterialButton dialogButtonPickType;
    private MaterialButton dialogButtonChooseFile;

    public TeacherSubjectMaterialsFragment() {
        super(R.layout.fragment_teacher_subject_materials);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        filePicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null) return;
            pickedFileUri = uri;
            if (dialogButtonChooseFile != null) {
                dialogButtonChooseFile.setText(R.string.file_selected_tap_to_change);
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_teacher_subject_materials, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TeacherSubjectMaterialsViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.study_material_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.swipeRefresh);
        RefreshUx.Binding refreshBinding = RefreshUx.bindToolbarIcon(
                toolbar.findViewById(R.id.toolbarActionSlot), swipeRefresh, () -> viewModel.refresh());

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_study_material_tap_upload);
        ((android.widget.ImageView) emptyState.findViewById(R.id.imageEmptyIcon)).setImageResource(R.drawable.ic_folder_open);

        LinearProgressIndicator progressUpload = view.findViewById(R.id.progressUpload);
        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<StudyMaterial> adapter = new SimpleListAdapter<>(R.layout.item_study_material, (itemView, material, position) -> {
            ((TextView) itemView.findViewById(R.id.textType)).setText(material.getMaterialType().name());
            ((TextView) itemView.findViewById(R.id.textTitle)).setText(material.getTitle());
            ((TextView) itemView.findViewById(R.id.textFileName)).setText(material.getFileName());
            com.example.uos_lms.core.ui.AccentColors.applyBar(itemView.findViewById(R.id.accentBar),
                    com.example.uos_lms.core.ui.MaterialTypeColors.colorFor(material.getMaterialType()));

            TextView textDescription = itemView.findViewById(R.id.textDescription);
            if (material.getDescription() != null && !material.getDescription().isBlank()) {
                textDescription.setText(material.getDescription());
                textDescription.setVisibility(View.VISIBLE);
            } else {
                textDescription.setVisibility(View.GONE);
            }
            ((TextView) itemView.findViewById(R.id.textUploadedDate)).setText(
                    getString(R.string.uploaded_on_format, DateKeyUtils.millisToDisplay(material.getUploadedAt())));

            itemView.findViewById(R.id.clickableContent).setOnClickListener(v -> {
                if (material.getFileUrl() != null) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(material.getFileUrl())));
                }
            });
            itemView.findViewById(R.id.buttonEdit).setVisibility(View.VISIBLE);
            itemView.findViewById(R.id.buttonEdit).setOnClickListener(v -> showMaterialDialog(material));
            itemView.findViewById(R.id.buttonDelete).setOnClickListener(v ->
                    ConfirmDialogHelper.show(requireContext(),
                            getString(R.string.delete_material_title),
                            getString(R.string.delete_material_message_format, material.getTitle()),
                            getString(R.string.delete),
                            () -> viewModel.delete(material.getId())));
        });
        recyclerList.setAdapter(adapter);

        view.findViewById(R.id.fabAdd).setOnClickListener(v -> showMaterialDialog(null));

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            boolean hasMaterials = !state.getMaterials().isEmpty();
            emptyState.setVisibility(hasMaterials || state.isLoading() ? View.GONE : View.VISIBLE);
            recyclerList.setVisibility(hasMaterials ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getMaterials());

            if (state.isUploading()) {
                progressUpload.setVisibility(View.VISIBLE);
                if (state.getUploadProgress() != null) {
                    progressUpload.setIndeterminate(false);
                    progressUpload.setProgress(state.getUploadProgress());
                } else {
                    progressUpload.setIndeterminate(true);
                }
            } else {
                progressUpload.setVisibility(View.GONE);
            }

            refreshBinding.setRefreshing(state.isRefreshing());
            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    /** Null editing = upload a brand-new material (file required); non-null = metadata-only
     * edit of an existing one (title/description/type - see materialController.update for why
     * replacing the file itself isn't part of "edit"). */
    private void showMaterialDialog(@Nullable StudyMaterial editing) {
        boolean isEdit = editing != null;
        selectedType = isEdit ? editing.getMaterialType() : MaterialType.PDF;
        pickedFileUri = null;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_upload_material, null);
        TextInputEditText editTitle = dialogView.findViewById(R.id.editTitle);
        TextInputEditText editDescription = dialogView.findViewById(R.id.editDescription);
        dialogButtonPickType = dialogView.findViewById(R.id.buttonPickType);
        dialogButtonChooseFile = dialogView.findViewById(R.id.buttonChooseFile);
        TextView textError = dialogView.findViewById(R.id.textError);

        if (isEdit) {
            editTitle.setText(editing.getTitle());
            editDescription.setText(editing.getDescription());
            dialogButtonChooseFile.setVisibility(View.GONE);
        }

        dialogButtonPickType.setText(selectedType.name());
        dialogButtonPickType.setOnClickListener(v ->
                SelectDialogHelper.show(requireContext(), getString(R.string.material_type_title),
                        SELECTABLE_TYPES, Enum::name, "", type -> {
                            selectedType = type;
                            dialogButtonPickType.setText(type.name());
                        }));

        dialogButtonChooseFile.setOnClickListener(v -> filePicker.launch("*/*"));

        android.app.Dialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(isEdit ? R.string.edit_material_title : R.string.upload_material_title)
                .setView(dialogView)
                .setPositiveButton(isEdit ? R.string.save : R.string.upload_button, null)
                .setNegativeButton(R.string.cancel_button, null)
                .setOnDismissListener(d -> {
                    dialogButtonPickType = null;
                    dialogButtonChooseFile = null;
                })
                .show();

        dialog.findViewById(android.R.id.button1).setOnClickListener(v -> {
            String title = editTitle.getText() == null ? "" : editTitle.getText().toString().trim();
            String description = editDescription.getText() == null ? "" : editDescription.getText().toString().trim();
            if (title.isEmpty()) {
                textError.setText(R.string.title_required_period);
                textError.setVisibility(View.VISIBLE);
                return;
            }
            if (isEdit) {
                viewModel.update(editing.getId(), title, description, selectedType);
            } else {
                if (pickedFileUri == null) {
                    textError.setText(R.string.choose_a_file_to_upload);
                    textError.setVisibility(View.VISIBLE);
                    return;
                }
                viewModel.upload(title, description, selectedType, pickedFileUri);
            }
            dialog.dismiss();
        });
    }
}
