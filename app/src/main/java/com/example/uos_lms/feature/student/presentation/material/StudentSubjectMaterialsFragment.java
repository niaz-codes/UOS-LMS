package com.example.uos_lms.feature.student.presentation.material;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.uos_lms.R;
import com.example.uos_lms.core.common.DateKeyUtils;
import com.example.uos_lms.core.domain.model.StudyMaterial;
import com.example.uos_lms.core.ui.MaterialDownloader;
import com.example.uos_lms.core.ui.RefreshUx;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.snackbar.Snackbar;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class StudentSubjectMaterialsFragment extends Fragment {

    private StudentSubjectMaterialsViewModel viewModel;
    private ActivityResultLauncher<String> storagePermissionLauncher;
    private StudyMaterial pendingDownload;

    public StudentSubjectMaterialsFragment() {
        super(R.layout.fragment_student_subject_materials);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        storagePermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            StudyMaterial material = pendingDownload;
            pendingDownload = null;
            if (material == null) return;
            if (granted) {
                startDownload(material);
            } else {
                showSnackbar(R.string.download_permission_required_message);
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student_subject_materials, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(StudentSubjectMaterialsViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.study_material_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.swipeRefresh);
        RefreshUx.Binding refreshBinding = RefreshUx.bindToolbarIcon(
                toolbar.findViewById(R.id.toolbarActionSlot), swipeRefresh, () -> viewModel.refresh());

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_materials_this_subject_message);
        ((android.widget.ImageView) emptyState.findViewById(R.id.imageEmptyIcon)).setImageResource(R.drawable.ic_folder_open);
        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<StudyMaterial> adapter = new SimpleListAdapter<>(R.layout.item_study_material, (itemView, material, position) -> {
            ((TextView) itemView.findViewById(R.id.textType)).setText(material.getMaterialType().name());
            ((TextView) itemView.findViewById(R.id.textTitle)).setText(material.getTitle());
            ((TextView) itemView.findViewById(R.id.textFileName)).setText(
                    getString(R.string.file_by_uploader_format, material.getFileName(), material.getUploadedByName()));
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

            itemView.findViewById(R.id.buttonDelete).setVisibility(View.GONE);
            itemView.findViewById(R.id.buttonEdit).setVisibility(View.GONE);

            View buttonDownload = itemView.findViewById(R.id.buttonDownload);
            buttonDownload.setVisibility(View.VISIBLE);
            buttonDownload.setOnClickListener(v -> requestDownload(material));

            // Tapping the row itself previews the file (opens it in whatever viewer the device
            // has for its type) - Download is the separate, explicit action above.
            itemView.setOnClickListener(v -> {
                if (material.getFileUrl() != null) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(material.getFileUrl())));
                }
            });
        });
        recyclerList.setAdapter(adapter);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            boolean hasMaterials = !state.getMaterials().isEmpty();
            emptyState.setVisibility(hasMaterials || state.isLoading() ? View.GONE : View.VISIBLE);
            recyclerList.setVisibility(hasMaterials ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getMaterials());
            refreshBinding.setRefreshing(state.isRefreshing());
            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void requestDownload(StudyMaterial material) {
        if (material.getFileUrl() == null) return;
        boolean needsRuntimePermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                && ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED;
        if (needsRuntimePermission) {
            pendingDownload = material;
            storagePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return;
        }
        startDownload(material);
    }

    private void startDownload(StudyMaterial material) {
        boolean started = MaterialDownloader.download(requireContext(), material.getFileUrl(), material.getFileName(), material.getTitle());
        showSnackbar(started ? R.string.download_started_message : R.string.download_failed_message);
    }

    private void showSnackbar(int messageRes) {
        View view = getView();
        if (view != null) Snackbar.make(view, messageRes, Snackbar.LENGTH_LONG).show();
    }
}
