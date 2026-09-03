package com.example.uos_lms.feature.hod.presentation.results;

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
import com.example.uos_lms.core.domain.model.Session;
import com.example.uos_lms.core.ui.AccentColors;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.snackbar.Snackbar;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HodResultsSessionListFragment extends Fragment {

    private HodResultsSessionListViewModel viewModel;

    public HodResultsSessionListFragment() {
        super(R.layout.fragment_hod_results_session_list);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_hod_results_session_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HodResultsSessionListViewModel.class);

        Bundle args = getArguments();
        String departmentId = args != null ? args.getString("departmentId") : null;
        String departmentName = args != null ? args.getString("departmentName") : null;

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.select_session_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_sessions_yet_short);
        View progressLoading = view.findViewById(R.id.progressLoading);
        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<Session> adapter = new SimpleListAdapter<>(R.layout.item_simple_nav_row, (itemView, session, position) -> {
            ((TextView) itemView.findViewById(R.id.textTitle)).setText(session.getLabel());
            AccentColors.applyBar(itemView.findViewById(R.id.accentBar), R.color.role_hod_start);

            itemView.setOnClickListener(v -> {
                Bundle navArgs = new Bundle();
                navArgs.putString("departmentId", departmentId);
                navArgs.putString("departmentName", departmentName);
                navArgs.putString("sessionId", session.getId());
                navArgs.putString("sessionLabel", session.getLabel());
                NavHostFragment.findNavController(this).navigate(R.id.hodResultsSemesterListFragment, navArgs);
            });
        });
        recyclerList.setAdapter(adapter);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            progressLoading.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
            boolean hasSessions = !state.getSessions().isEmpty();
            emptyState.setVisibility(!state.isLoading() && !hasSessions ? View.VISIBLE : View.GONE);
            recyclerList.setVisibility(hasSessions ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getSessions());

            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
