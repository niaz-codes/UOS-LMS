package com.example.uos_lms.feature.notifications;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiNotificationDataSource;
import com.example.uos_lms.core.domain.model.AppNotification;
import com.example.uos_lms.core.domain.model.NotificationCategory;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class NotificationsViewModel extends ViewModel {

    private final ApiNotificationDataSource notificationDataSource;

    private final MutableLiveData<NotificationsUiState> uiState = new MutableLiveData<>(NotificationsUiState.initial());

    @Inject
    public NotificationsViewModel(ApiNotificationDataSource notificationDataSource) {
        this.notificationDataSource = notificationDataSource;
        load(true);
    }

    public LiveData<NotificationsUiState> getUiState() {
        return uiState;
    }

    private NotificationsUiState state() {
        return uiState.getValue();
    }

    public void refresh() {
        NotificationsUiState current = state();
        uiState.setValue(current.toBuilder().refreshing(true).errorMessage(null).build());
        fetch(1, false);
    }

    public void loadMore() {
        NotificationsUiState current = state();
        if (current.isLoading() || current.isLoadingMore() || current.isRefreshing() || !current.isHasMore()) return;
        uiState.setValue(current.toBuilder().loadingMore(true).build());
        fetch(current.getPage() + 1, true);
    }

    public void onCategorySelected(@Nullable NotificationCategory category) {
        uiState.setValue(state().toBuilder().selectedCategory(category).build());
        load(true);
    }

    public void onUnreadOnlyToggled(boolean unreadOnly) {
        uiState.setValue(state().toBuilder().unreadOnly(unreadOnly).build());
        load(true);
    }

    public void onSearchSubmit(String query) {
        uiState.setValue(state().toBuilder().searchQuery(query == null || query.isBlank() ? null : query.trim()).build());
        load(true);
    }

    private void load(boolean showLoading) {
        NotificationsUiState current = state();
        uiState.setValue(current.toBuilder().loading(showLoading).errorMessage(null).build());
        fetch(1, false);
    }

    private void fetch(int page, boolean append) {
        NotificationsUiState current = state();
        Boolean read = current.isUnreadOnly() ? Boolean.FALSE : null;
        notificationDataSource.list(current.getSelectedCategory(), read, current.getSearchQuery(), page, 30)
                .addOnSuccessListener(result -> {
                    NotificationsUiState latest = state();
                    List<AppNotification> items = new ArrayList<>(append ? latest.getItems() : List.of());
                    items.addAll(result.getItems());
                    boolean hasMore = (long) page * result.getLimit() < result.getTotal();
                    uiState.setValue(latest.toBuilder()
                            .items(items)
                            .loading(false)
                            .refreshing(false)
                            .loadingMore(false)
                            .hasMore(hasMore)
                            .page(page)
                            .unreadCount(result.getUnreadCount())
                            .build());
                })
                .addOnFailureListener(e -> uiState.setValue(state().toBuilder()
                        .loading(false).refreshing(false).loadingMore(false)
                        .errorMessage(e.getMessage())
                        .build()));
    }

    public void markRead(AppNotification item) {
        if (item.isRead()) return;
        applyLocalUpdate(item.toBuilder().read(true).build());
        notificationDataSource.markRead(item.getId());
    }

    public void markAllRead() {
        NotificationsUiState current = state();
        List<AppNotification> updated = new ArrayList<>();
        for (AppNotification item : current.getItems()) {
            updated.add(item.isRead() ? item : item.toBuilder().read(true).build());
        }
        uiState.setValue(current.toBuilder().items(updated).unreadCount(0).build());
        notificationDataSource.markAllRead()
                .addOnFailureListener(e -> uiState.setValue(state().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void delete(AppNotification item) {
        NotificationsUiState current = state();
        List<AppNotification> updated = new ArrayList<>(current.getItems());
        updated.removeIf(existing -> existing.getId().equals(item.getId()));
        int newUnreadCount = item.isRead() ? current.getUnreadCount() : Math.max(0, current.getUnreadCount() - 1);
        uiState.setValue(current.toBuilder().items(updated).unreadCount(newUnreadCount).build());
        notificationDataSource.delete(item.getId())
                .addOnFailureListener(e -> uiState.setValue(state().toBuilder().errorMessage(e.getMessage()).build()));
    }

    private void applyLocalUpdate(AppNotification updatedItem) {
        NotificationsUiState current = state();
        List<AppNotification> updated = new ArrayList<>();
        boolean wasUnread = false;
        for (AppNotification item : current.getItems()) {
            if (item.getId().equals(updatedItem.getId())) {
                wasUnread = !item.isRead();
                updated.add(updatedItem);
            } else {
                updated.add(item);
            }
        }
        int newUnreadCount = wasUnread ? Math.max(0, current.getUnreadCount() - 1) : current.getUnreadCount();
        uiState.setValue(current.toBuilder().items(updated).unreadCount(newUnreadCount).build());
    }

    public void clearError() {
        uiState.setValue(state().toBuilder().errorMessage(null).build());
    }
}
