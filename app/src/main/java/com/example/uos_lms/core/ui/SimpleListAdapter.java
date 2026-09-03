package com.example.uos_lms.core.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

/** Generic RecyclerView adapter for the many simple "inflate a row, bind one item" lists
 * across the app - avoids writing a bespoke Adapter class per screen. */
public class SimpleListAdapter<T> extends RecyclerView.Adapter<SimpleListAdapter.ViewHolder> {

    public interface Binder<T> {
        void bind(@NonNull View itemView, T item, int position);
    }

    @LayoutRes
    private final int layoutRes;
    private final Binder<T> binder;
    private List<T> items = Collections.emptyList();

    public SimpleListAdapter(@LayoutRes int layoutRes, Binder<T> binder) {
        this.layoutRes = layoutRes;
        this.binder = binder;
    }

    public void submitList(List<T> newItems) {
        this.items = newItems != null ? newItems : Collections.emptyList();
        notifyDataSetChanged();
    }

    /** Swaps the backing data without notifying the RecyclerView - for callers that already
     * know the currently bound rows still show up-to-date content (e.g. a row updated its own
     * views directly) and want to avoid the rebind churn a full notifyDataSetChanged() causes,
     * which can steal focus from a row's in-progress EditText. Keeps future rebinds (e.g. after
     * scrolling) reading fresh data. */
    public void setItemsQuietly(List<T> newItems) {
        this.items = newItems != null ? newItems : Collections.emptyList();
    }

    public List<T> getItems() {
        return items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        binder.bind(holder.itemView, items.get(position), position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
