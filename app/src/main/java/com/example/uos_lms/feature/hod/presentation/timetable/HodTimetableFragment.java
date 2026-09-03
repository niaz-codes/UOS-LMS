package com.example.uos_lms.feature.hod.presentation.timetable;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uos_lms.R;
import com.example.uos_lms.core.common.TimeOfDayUtils;
import com.example.uos_lms.core.domain.model.DayOfWeek;
import com.example.uos_lms.core.domain.model.TimetableSlot;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HodTimetableFragment extends Fragment {

    private HodTimetableViewModel viewModel;

    public HodTimetableFragment() {
        super(R.layout.fragment_hod_timetable);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_hod_timetable, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HodTimetableViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.timetable_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        ProgressBar progressLoading = view.findViewById(R.id.progressLoading);
        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.timetable_no_slots_this_day);
        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<TimetableSlot> adapter = new SimpleListAdapter<>(R.layout.item_timetable_slot_card, (itemView, slot, position) -> {
            ((TextView) itemView.findViewById(R.id.textTime)).setText(TimeOfDayUtils.formatRange(slot.getStartTimeMinutes(), slot.getEndTimeMinutes()));
            ((TextView) itemView.findViewById(R.id.textSubject)).setText(slot.getSubjectCode() + " • " + slot.getSubjectTitle());
            ((TextView) itemView.findViewById(R.id.textTeacher)).setText(slot.getTeacherName().isBlank()
                    ? getString(R.string.teacher_not_assigned) : getString(R.string.teacher_label_format, slot.getTeacherName()));
            ((TextView) itemView.findViewById(R.id.textRoom)).setText(getString(R.string.timetable_room_format, slot.getRoom()));
            com.example.uos_lms.core.ui.AccentColors.applyBar(itemView.findViewById(R.id.accentBar), R.color.role_hod_start);
            itemView.findViewById(R.id.buttonDelete).setVisibility(View.GONE);
        });
        recyclerList.setAdapter(adapter);

        TabLayout tabLayout = view.findViewById(R.id.tabLayoutDays);
        for (DayOfWeek day : DayOfWeek.values()) {
            tabLayout.addTab(tabLayout.newTab().setText(day.getDisplayName().substring(0, 3)));
        }
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewModel.selectDay(DayOfWeek.values()[tab.getPosition()]);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            progressLoading.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);

            boolean hasSlots = !state.getSlotsForSelectedDay().isEmpty();
            emptyState.setVisibility(!state.isLoading() && !hasSlots ? View.VISIBLE : View.GONE);
            recyclerList.setVisibility(hasSlots ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getSlotsForSelectedDay());

            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
