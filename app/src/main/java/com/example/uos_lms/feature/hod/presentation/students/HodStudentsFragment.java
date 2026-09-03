package com.example.uos_lms.feature.hod.presentation.students;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.example.uos_lms.core.ui.SelectDialogHelper;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.example.uos_lms.core.ui.StatusChipHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HodStudentsFragment extends Fragment {

    private HodStudentsViewModel viewModel;
    private boolean suppressSearchWatcher;

    public HodStudentsFragment() {
        super(R.layout.fragment_hod_students);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_hod_students, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HodStudentsViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.students_title);
        toolbar.findViewById(R.id.buttonBack).setVisibility(View.GONE);

        SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.swipeRefresh);
        RefreshUx.Binding refreshBinding = RefreshUx.bindToolbarIcon(
                toolbar.findViewById(R.id.toolbarActionSlot), swipeRefresh, () -> viewModel.refresh());

        TextInputEditText editSearch = view.findViewById(R.id.editSearch);
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!suppressSearchWatcher) viewModel.onSearchQueryChange(s.toString());
            }
        });

        Chip chipSemesterFilter = view.findViewById(R.id.chipSemesterFilter);
        chipSemesterFilter.setOnClickListener(v ->
                SelectDialogHelper.show(requireContext(), getString(R.string.filter_by_semester),
                        viewModel.getUiState().getValue().getSemesters(), com.example.uos_lms.core.domain.model.Semester::getDisplayName,
                        getString(R.string.no_semesters_exist_in_department), viewModel::onSemesterSelected));

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_students_found);
        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<com.example.uos_lms.core.domain.model.User> adapter = new SimpleListAdapter<>(R.layout.item_hod_student_row, (itemView, student, position) -> {
            ((TextView) itemView.findViewById(R.id.textName)).setText(student.getFullName());
            ((TextView) itemView.findViewById(R.id.textEmail)).setText(student.getEmail());
            com.example.uos_lms.core.ui.AccentColors.applyBar(itemView.findViewById(R.id.accentBar),
                    StatusChipHelper.bind(itemView.findViewById(R.id.textStatusChip), student.getStatus()));
        });
        recyclerList.setAdapter(adapter);

        BottomNavigationView bottomNav = view.findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.navStudents);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navStudents) return true;
            if (id == R.id.navHome) NavHostFragment.findNavController(this).popBackStack(R.id.hodDashboardFragment, false);
            else if (id == R.id.navTeachers) NavHostFragment.findNavController(this).navigate(R.id.hodTeachersFragment);
            else if (id == R.id.navReports) NavHostFragment.findNavController(this).navigate(R.id.hodReportsFragment);
            else if (id == R.id.navProfile) NavHostFragment.findNavController(this).navigate(R.id.profileFragment);
            return false;
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            suppressSearchWatcher = true;
            String current = editSearch.getText() == null ? "" : editSearch.getText().toString();
            if (!current.equals(state.getSearchQuery())) editSearch.setText(state.getSearchQuery());
            suppressSearchWatcher = false;

            chipSemesterFilter.setChecked(state.getSelectedSemester() != null);
            chipSemesterFilter.setText(state.getSelectedSemester() != null ? state.getSelectedSemester().getDisplayName() : getString(R.string.all_semesters));

            boolean hasStudents = !state.getFilteredStudents().isEmpty();
            ((TextView) view.findViewById(R.id.textCount)).setText(getString(R.string.student_count_format, state.getFilteredStudents().size()));
            emptyState.setVisibility(hasStudents || state.isLoading() ? View.GONE : View.VISIBLE);
            recyclerList.setVisibility(hasStudents ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getFilteredStudents());
            refreshBinding.setRefreshing(state.isRefreshing());
            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
