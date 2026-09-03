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

import com.example.uos_lms.R;
import com.google.android.material.snackbar.Snackbar;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HodResultsFragment extends Fragment {

    private HodResultsViewModel viewModel;

    public HodResultsFragment() {
        super(R.layout.fragment_hod_results);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_hod_results, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HodResultsViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.results_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        View progressLoading = view.findViewById(R.id.progressLoading);
        View contentContainer = view.findViewById(R.id.contentContainer);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            progressLoading.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
            contentContainer.setVisibility(!state.isLoading() ? View.VISIBLE : View.GONE);

            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
            }
            if (state.isLoading() || state.getDepartmentId() == null) return;

            ((TextView) view.findViewById(R.id.textDepartmentName)).setText(state.getDepartmentName());
            view.findViewById(R.id.cardBrowseSessions).setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString("departmentId", state.getDepartmentId());
                args.putString("departmentName", state.getDepartmentName());
                NavHostFragment.findNavController(this).navigate(R.id.hodResultsSessionListFragment, args);
            });
        });
    }
}
