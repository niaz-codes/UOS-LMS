package com.example.uos_lms.feature.notifications;

import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.uos_lms.R;
import com.example.uos_lms.core.domain.model.AppNotification;
import com.example.uos_lms.core.domain.model.NotificationCategory;
import com.example.uos_lms.core.notifications.NotificationIcons;
import com.example.uos_lms.core.notifications.NotificationRouter;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class NotificationsFragment extends Fragment {

    private NotificationsViewModel viewModel;
    private SimpleListAdapter<AppNotification> adapter;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private View emptyState;
    private ProgressBar progressLoading;
    private ChipGroup chipGroupFilters;

    public NotificationsFragment() {
        super(R.layout.fragment_notifications);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(NotificationsViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.notifications_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        FrameLayout actionSlot = toolbar.findViewById(R.id.toolbarActionSlot);
        ImageButton buttonMarkAllRead = (ImageButton) LayoutInflater.from(requireContext())
                .inflate(R.layout.item_toolbar_icon_button, actionSlot, false);
        buttonMarkAllRead.setImageResource(R.drawable.ic_done_all);
        buttonMarkAllRead.setContentDescription(getString(R.string.mark_all_read_desc));
        buttonMarkAllRead.setOnClickListener(v -> viewModel.markAllRead());
        actionSlot.addView(buttonMarkAllRead);

        emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_notifications_yet);
        progressLoading = view.findViewById(R.id.progressLoading);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());

        recyclerView = view.findViewById(R.id.recyclerNotifications);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SimpleListAdapter<>(R.layout.item_notification_row, this::bindRow);
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (lm == null || dy <= 0) return;
                int lastVisible = lm.findLastVisibleItemPosition();
                if (lastVisible >= adapter.getItemCount() - 3) {
                    viewModel.loadMore();
                }
            }
        });

        chipGroupFilters = view.findViewById(R.id.chipGroupFilters);
        chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            onChipChecked(checkedIds.get(0));
        });

        TextInputEditText editSearch = view.findViewById(R.id.editSearch);
        editSearch.setOnEditorActionListener((v, actionId, event) -> {
            viewModel.onSearchSubmit(editSearch.getText() != null ? editSearch.getText().toString() : null);
            return true;
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> render(view, state));
    }

    private void onChipChecked(int checkedId) {
        if (checkedId == R.id.chipAll) {
            viewModel.onUnreadOnlyToggled(false);
            viewModel.onCategorySelected(null);
        } else if (checkedId == R.id.chipUnread) {
            viewModel.onCategorySelected(null);
            viewModel.onUnreadOnlyToggled(true);
        } else {
            viewModel.onUnreadOnlyToggled(false);
            viewModel.onCategorySelected(categoryForChip(checkedId));
        }
    }

    @Nullable
    private NotificationCategory categoryForChip(int chipId) {
        if (chipId == R.id.chipAccount) return NotificationCategory.ACCOUNT;
        if (chipId == R.id.chipAcademic) return NotificationCategory.ACADEMIC_CONTENT;
        if (chipId == R.id.chipAttendance) return NotificationCategory.ATTENDANCE;
        if (chipId == R.id.chipExamResult) return NotificationCategory.EXAM_RESULT;
        if (chipId == R.id.chipLeave) return NotificationCategory.LEAVE;
        if (chipId == R.id.chipAnnouncement) return NotificationCategory.ANNOUNCEMENT;
        if (chipId == R.id.chipCalendar) return NotificationCategory.CALENDAR;
        if (chipId == R.id.chipMessage) return NotificationCategory.MESSAGE;
        if (chipId == R.id.chipPromotion) return NotificationCategory.PROMOTION;
        if (chipId == R.id.chipSystem) return NotificationCategory.SYSTEM;
        return null;
    }

    private void render(View rootView, NotificationsUiState state) {
        boolean initialLoad = state.isLoading() && state.getItems().isEmpty();
        progressLoading.setVisibility(initialLoad ? View.VISIBLE : View.GONE);
        swipeRefresh.setRefreshing(state.isRefreshing());
        swipeRefresh.setVisibility(initialLoad ? View.GONE : View.VISIBLE);

        adapter.submitList(state.getItems());
        boolean showEmpty = !initialLoad && !state.isRefreshing() && state.getItems().isEmpty();
        emptyState.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
        if (showEmpty) {
            boolean filtered = state.getSelectedCategory() != null || state.isUnreadOnly() || state.getSearchQuery() != null;
            ((TextView) emptyState.findViewById(R.id.textEmptyMessage))
                    .setText(filtered ? R.string.no_notifications_match_filter : R.string.no_notifications_yet);
        }

        if (state.getErrorMessage() != null) {
            Snackbar.make(rootView, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
            viewModel.clearError();
        }
    }

    private void bindRow(View row, AppNotification item, int position) {
        ((ImageView) row.findViewById(R.id.imageIcon)).setImageResource(NotificationIcons.iconFor(item.getCategory()));
        ((TextView) row.findViewById(R.id.textTitle)).setText(item.getTitle());
        TextView textBody = row.findViewById(R.id.textBody);
        if (item.getBody() == null || item.getBody().isBlank()) {
            textBody.setVisibility(View.GONE);
        } else {
            textBody.setVisibility(View.VISIBLE);
            textBody.setText(item.getBody());
        }
        ((TextView) row.findViewById(R.id.textTime)).setText(relativeTime(item.getCreatedAtMillis()));
        row.findViewById(R.id.unreadDot).setVisibility(item.isRead() ? View.GONE : View.VISIBLE);
        com.example.uos_lms.core.ui.AccentColors.applyBar(row.findViewById(R.id.accentBar),
                com.example.uos_lms.core.ui.AccentColors.colorForNotificationCategory(item.getCategory()));

        row.setOnClickListener(v -> {
            viewModel.markRead(item);
            navigateForCategory(item.getCategory());
        });
        row.findViewById(R.id.buttonDelete).setOnClickListener(v -> viewModel.delete(item));
    }

    private void navigateForCategory(NotificationCategory category) {
        NotificationRouter.navigate(NavHostFragment.findNavController(this), category);
    }

    private CharSequence relativeTime(long timestampMillis) {
        if (timestampMillis <= 0) return "";
        long now = System.currentTimeMillis();
        long clamped = Math.min(timestampMillis, now);
        return DateUtils.getRelativeTimeSpanString(clamped, now, DateUtils.MINUTE_IN_MILLIS);
    }
}
