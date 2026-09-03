package com.example.uos_lms.feature.hod.presentation.teachers;

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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.uos_lms.R;
import com.example.uos_lms.core.ui.RefreshUx;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HodTeachersFragment extends Fragment {

    private HodTeachersViewModel viewModel;

    public HodTeachersFragment() {
        super(R.layout.fragment_hod_teachers);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_hod_teachers, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HodTeachersViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.teachers_title);
        toolbar.findViewById(R.id.buttonBack).setVisibility(View.GONE);

        SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.swipeRefresh);
        RefreshUx.Binding refreshBinding = RefreshUx.bindToolbarIcon(
                toolbar.findViewById(R.id.toolbarActionSlot), swipeRefresh, () -> viewModel.refresh());

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_approved_teachers_in_your_department);
        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<TeacherWithLoad> adapter = new SimpleListAdapter<>(R.layout.item_hod_teacher_row, (itemView, entry, position) -> {
            ((TextView) itemView.findViewById(R.id.textName)).setText(entry.getTeacher().getFullName());
            ((TextView) itemView.findViewById(R.id.textEmail)).setText(entry.getTeacher().getEmail());
            ((TextView) itemView.findViewById(R.id.textSubjectCount)).setText(getString(R.string.subject_count_format, entry.getSubjectCount()));
            com.example.uos_lms.core.ui.AccentColors.applyBar(itemView.findViewById(R.id.accentBar), R.color.role_teacher_start);
        });
        recyclerList.setAdapter(adapter);

        BottomNavigationView bottomNav = view.findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.navTeachers);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navTeachers) return true;
            if (id == R.id.navHome) NavHostFragment.findNavController(this).popBackStack(R.id.hodDashboardFragment, false);
            else if (id == R.id.navStudents) NavHostFragment.findNavController(this).navigate(R.id.hodStudentsFragment);
            else if (id == R.id.navReports) NavHostFragment.findNavController(this).navigate(R.id.hodReportsFragment);
            else if (id == R.id.navProfile) NavHostFragment.findNavController(this).navigate(R.id.profileFragment);
            return false;
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            boolean hasTeachers = !state.getTeachers().isEmpty();
            emptyState.setVisibility(hasTeachers || state.isLoading() ? View.GONE : View.VISIBLE);
            recyclerList.setVisibility(hasTeachers ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getTeachers());
            refreshBinding.setRefreshing(state.isRefreshing());
            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
