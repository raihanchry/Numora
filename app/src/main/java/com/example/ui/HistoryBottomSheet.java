package com.example.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.data.CalculationRecord;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

/**
 * Material 3 modal bottom sheet presenting the local calculation history tape.
 */
public final class HistoryBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "HistoryBottomSheet";

    private CalculatorViewModel viewModel;
    private HistoryAdapter adapter;
    private View emptyHistoryView;
    private RecyclerView rvHistory;
    private MaterialButton btnClearHistory;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.bottom_sheet_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(CalculatorViewModel.class);
        emptyHistoryView = view.findViewById(R.id.emptyHistoryView);
        rvHistory = view.findViewById(R.id.rvHistory);
        btnClearHistory = view.findViewById(R.id.btnClearHistory);

        adapter = new HistoryAdapter(new HistoryAdapter.OnRecordActionListener() {
            @Override
            public void onUseResult(@NonNull CalculationRecord record) {
                viewModel.onSelectHistoryRecord(record, false);
                dismiss();
            }

            @Override
            public void onUseExpression(@NonNull CalculationRecord record) {
                viewModel.onSelectHistoryRecord(record, true);
                dismiss();
            }
        });

        rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvHistory.setAdapter(adapter);

        btnClearHistory.setOnClickListener(v -> confirmClearHistory());

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> renderHistory(state.getHistory()));
    }

    private void renderHistory(@NonNull List<CalculationRecord> records) {
        boolean isEmpty = records.isEmpty();
        emptyHistoryView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvHistory.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        btnClearHistory.setVisibility(isEmpty ? View.INVISIBLE : View.VISIBLE);
        adapter.submitList(records);
    }

    private void confirmClearHistory() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.clear_history_dialog_title)
                .setMessage(R.string.clear_history_dialog_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_clear, (dialog, which) -> viewModel.onClearHistory())
                .show();
    }
}
