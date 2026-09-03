package com.example.uos_lms.feature.calendar.presentation;

import android.app.DatePickerDialog;
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
import com.example.uos_lms.core.domain.model.CalendarEvent;
import com.example.uos_lms.core.domain.model.CalendarEventType;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AcademicCalendarFragment extends Fragment {

    private AcademicCalendarViewModel viewModel;
    private boolean latestIsAdmin;

    public AcademicCalendarFragment() {
        super(R.layout.fragment_academic_calendar);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_academic_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AcademicCalendarViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.academic_calendar_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_calendar_entries_yet);
        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<CalendarEvent> adapter = new SimpleListAdapter<>(R.layout.item_calendar_event_card, (itemView, event, position) -> {
            ((TextView) itemView.findViewById(R.id.textType)).setText(event.getType().name());
            ((TextView) itemView.findViewById(R.id.textTitle)).setText(event.getTitle());
            ((TextView) itemView.findViewById(R.id.textDate)).setText(DateKeyUtils.millisToDisplay(event.getDateMillis()));
            com.example.uos_lms.core.ui.AccentColors.applyBar(itemView.findViewById(R.id.accentBar), colorForEventType(event.getType()));

            TextView textDescription = itemView.findViewById(R.id.textDescription);
            if (event.getDescription() != null && !event.getDescription().isBlank()) {
                textDescription.setText(event.getDescription());
                textDescription.setVisibility(View.VISIBLE);
            } else {
                textDescription.setVisibility(View.GONE);
            }

            ImageButton buttonDelete = itemView.findViewById(R.id.buttonDelete);
            buttonDelete.setVisibility(latestIsAdmin ? View.VISIBLE : View.GONE);
            buttonDelete.setOnClickListener(v -> viewModel.deleteEvent(event.getId()));
        });
        recyclerList.setAdapter(adapter);

        view.findViewById(R.id.fabAdd).setOnClickListener(v -> showAddEventDialog());

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            latestIsAdmin = state.isAdmin();
            view.findViewById(R.id.fabAdd).setVisibility(state.isAdmin() ? View.VISIBLE : View.GONE);

            boolean hasEvents = !state.getEvents().isEmpty();
            emptyState.setVisibility(hasEvents || state.isLoading() ? View.GONE : View.VISIBLE);
            recyclerList.setVisibility(hasEvents ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getEvents());

            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
                viewModel.clearError();
            }
        });
    }

    private void showAddEventDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_calendar_event, null);
        MaterialButtonToggleGroup toggleType = dialogView.findViewById(R.id.toggleType);
        TextInputEditText editTitle = dialogView.findViewById(R.id.editTitle);
        TextInputEditText editDescription = dialogView.findViewById(R.id.editDescription);
        MaterialButton buttonPickDate = dialogView.findViewById(R.id.buttonPickDate);
        TextView textError = dialogView.findViewById(R.id.textError);

        toggleType.check(R.id.buttonTypeEvent);
        Long[] pickedDate = {null};

        buttonPickDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            if (pickedDate[0] != null) calendar.setTimeInMillis(pickedDate[0]);
            new DatePickerDialog(requireContext(), (dp, year, month, dayOfMonth) -> {
                Calendar picked = Calendar.getInstance();
                picked.set(year, month, dayOfMonth, 0, 0, 0);
                picked.set(Calendar.MILLISECOND, 0);
                pickedDate[0] = picked.getTimeInMillis();
                buttonPickDate.setText(DateKeyUtils.millisToDisplay(pickedDate[0]));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        Dialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_calendar_entry_title)
                .setView(dialogView)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel_button, null)
                .show();

        dialog.findViewById(android.R.id.button1).setOnClickListener(v -> {
            String title = editTitle.getText() == null ? "" : editTitle.getText().toString().trim();
            if (title.isEmpty()) {
                textError.setText(R.string.title_required_no_period);
                textError.setVisibility(View.VISIBLE);
                return;
            }
            if (pickedDate[0] == null) {
                textError.setText(R.string.pick_a_date_error);
                textError.setVisibility(View.VISIBLE);
                return;
            }
            String description = editDescription.getText() == null ? "" : editDescription.getText().toString().trim();
            CalendarEventType type = typeForButtonId(toggleType.getCheckedButtonId());
            viewModel.addEvent(title, description, type, pickedDate[0]);
            dialog.dismiss();
        });
    }

    private int colorForEventType(CalendarEventType type) {
        switch (type) {
            case HOLIDAY:
                return R.color.status_success;
            case EXAM:
                return R.color.status_warning;
            case EVENT:
            default:
                return R.color.status_info;
        }
    }

    private CalendarEventType typeForButtonId(int buttonId) {
        if (buttonId == R.id.buttonTypeHoliday) return CalendarEventType.HOLIDAY;
        if (buttonId == R.id.buttonTypeExam) return CalendarEventType.EXAM;
        return CalendarEventType.EVENT;
    }
}
