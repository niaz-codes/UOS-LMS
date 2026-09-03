package com.example.uos_lms.feature.admin.presentation.results;

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
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.ui.AccentColors;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.snackbar.Snackbar;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminResultsSemesterListFragment extends Fragment {

    private AdminResultsSemesterListViewModel viewModel;

    public AdminResultsSemesterListFragment() {
        super(R.layout.fragment_admin_results_semester_list);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_results_semester_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AdminResultsSemesterListViewModel.class);

        Bundle args = getArguments();
        String departmentId = args != null ? args.getString("departmentId") : null;
        String sessionId = args != null ? args.getString("sessionId") : null;
        String sessionLabel = args != null ? args.getString("sessionLabel") : null;

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(sessionLabel != null ? sessionLabel : getString(R.string.select_semester_title));
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_semesters_exist);
        View progressLoading = view.findViewById(R.id.progressLoading);
        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<Semester> adapter = new SimpleListAdapter<>(R.layout.item_simple_nav_row, (itemView, semester, position) -> {
            ((TextView) itemView.findViewById(R.id.textTitle)).setText(semester.getDisplayName());
            AccentColors.applyBar(itemView.findViewById(R.id.accentBar), R.color.role_admin_start);

            itemView.setOnClickListener(v -> {
                Bundle navArgs = new Bundle();
                navArgs.putString("departmentId", departmentId);
                navArgs.putString("sessionId", sessionId);
                navArgs.putString("sessionLabel", sessionLabel);
                navArgs.putString("semesterId", semester.getId());
                navArgs.putString("semesterLabel", semester.getDisplayName());
                NavHostFragment.findNavController(this).navigate(R.id.adminSemesterResultFragment, navArgs);
            });
        });
        recyclerList.setAdapter(adapter);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            progressLoading.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
            boolean hasSemesters = !state.getSemesters().isEmpty();
            emptyState.setVisibility(!state.isLoading() && !hasSemesters ? View.VISIBLE : View.GONE);
            recyclerList.setVisibility(hasSemesters ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getSemesters());

            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
