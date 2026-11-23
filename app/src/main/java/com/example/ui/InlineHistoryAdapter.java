package com.example.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.data.CalculationRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Compact right-aligned inline history tape adapter rendered directly above the active expression.
 */
public final class InlineHistoryAdapter
        extends RecyclerView.Adapter<InlineHistoryAdapter.InlineViewHolder> {

    public interface OnInlineItemClickListener {
        void onInlineItemClick(@NonNull CalculationRecord record);
        void onInlineItemLongClick(@NonNull CalculationRecord record);
    }

    private final List<CalculationRecord> chronologicalItems = new ArrayList<>();
    private final OnInlineItemClickListener listener;

    public InlineHistoryAdapter(@NonNull OnInlineItemClickListener listener) {
        this.listener = listener;
    }

    /**
     * Accepts newest-first history records and renders them oldest-to-newest (bottom-anchored).
     * When {@code excludeMostRecent} is true, hides index 0 so it isn't duplicated while shown large.
     */
    public void submitNewestFirst(@NonNull List<CalculationRecord> records, boolean excludeMostRecent) {
        chronologicalItems.clear();
        int startIndex = excludeMostRecent ? 1 : 0;
        if (records.size() > startIndex) {
            int limit = Math.min(records.size(), startIndex + 20);
            List<CalculationRecord> slice = new ArrayList<>(records.subList(startIndex, limit));
            Collections.reverse(slice);
            chronologicalItems.addAll(slice);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public InlineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_inline_history, parent, false);
        return new InlineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InlineViewHolder holder, int position) {
        holder.bind(chronologicalItems.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return chronologicalItems.size();
    }

    static final class InlineViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvExpression;
        private final TextView tvResult;

        InlineViewHolder(@NonNull View itemView) {
            super(itemView);
            tvExpression = itemView.findViewById(R.id.tvInlineExpression);
            tvResult = itemView.findViewById(R.id.tvInlineResult);
        }

        void bind(@NonNull CalculationRecord record, @NonNull OnInlineItemClickListener listener) {
            tvExpression.setText(record.getExpression());
            tvResult.setText("=" + record.getFormattedResult());
            itemView.setOnClickListener(v -> listener.onInlineItemClick(record));
            itemView.setOnLongClickListener(v -> {
                listener.onInlineItemLongClick(record);
                return true;
            });
        }
    }
}
