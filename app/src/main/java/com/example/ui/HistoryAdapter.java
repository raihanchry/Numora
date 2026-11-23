package com.example.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.data.CalculationRecord;
import com.example.engine.AngleMode;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter displaying saved calculation history entries.
 * Tap an entry to reuse its result; long-press to load the full original expression.
 */
public final class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.RecordViewHolder> {

    public interface OnRecordActionListener {
        void onUseResult(@NonNull CalculationRecord record);
        void onUseExpression(@NonNull CalculationRecord record);
    }

    private final List<CalculationRecord> items = new ArrayList<>();
    private final OnRecordActionListener listener;

    public HistoryAdapter(@NonNull OnRecordActionListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<CalculationRecord> records) {
        items.clear();
        items.addAll(records);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history_record, parent, false);
        return new RecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder holder, int position) {
        CalculationRecord record = items.get(position);
        holder.bind(record, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class RecordViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvAngleBadge;
        private final TextView tvExpression;
        private final TextView tvResult;

        RecordViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAngleBadge = itemView.findViewById(R.id.tvHistoryAngleBadge);
            tvExpression = itemView.findViewById(R.id.tvHistoryExpression);
            tvResult = itemView.findViewById(R.id.tvHistoryResult);
        }

        void bind(@NonNull CalculationRecord record, @NonNull OnRecordActionListener listener) {
            tvExpression.setText(record.getExpression());
            tvResult.setText("= " + record.getFormattedResult());
            boolean hasTrig = containsTrigFunction(record.getExpression());
            if (hasTrig) {
                tvAngleBadge.setVisibility(View.VISIBLE);
                tvAngleBadge.setText(record.getAngleMode() == AngleMode.RADIANS
                        ? R.string.angle_rad
                        : R.string.angle_deg);
            } else {
                tvAngleBadge.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> listener.onUseResult(record));
            itemView.setOnLongClickListener(v -> {
                listener.onUseExpression(record);
                return true;
            });
        }

        private boolean containsTrigFunction(@NonNull String expr) {
            return expr.contains("sin") || expr.contains("cos") || expr.contains("tan");
        }
    }
}
