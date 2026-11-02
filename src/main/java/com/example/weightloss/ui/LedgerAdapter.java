package com.example.weightloss.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weightloss.R;
import com.example.weightloss.databinding.ItemMonthHeaderBinding;
import com.example.weightloss.databinding.ItemRecordLedgerBinding;
import com.example.weightloss.model.WeightRecord;
import com.example.weightloss.util.DateUtils;
import com.example.weightloss.util.HealthUtils;
import com.example.weightloss.util.MoodUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 记录列表适配器：按月插入分组头，并显示与上一条记录的差值。 */
public class LedgerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onEdit(WeightRecord record);

        void onDelete(WeightRecord record);
    }

    private static final int TYPE_MONTH = 0;
    private static final int TYPE_RECORD = 1;

    private final Context context;
    private final Listener listener;
    private final List<Row> rows = new ArrayList<>();

    public LedgerAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void submit(List<WeightRecord> descending, List<WeightRecord> allAscending) {
        // 建立“当前记录 -> 上一条记录”的映射用于计算差值
        rows.clear();

        Map<Long, WeightRecord> previousById = new HashMap<>();
        for (int i = 1; i < allAscending.size(); i++) {
            previousById.put(allAscending.get(i).getId(), allAscending.get(i - 1));
        }

        Map<String, Integer> monthCounts = new HashMap<>();
        for (WeightRecord record : descending) {
            String key = monthKey(record.getDate());
            Integer count = monthCounts.get(key);
            monthCounts.put(key, count == null ? 1 : count + 1);
        }

        String currentMonth = null;
        for (WeightRecord record : descending) {
            String key = monthKey(record.getDate());
            if (!key.equals(currentMonth)) {
                currentMonth = key;
                int[] parts = DateUtils.getYearMonth(record.getDate());
                Row header = new Row();
                header.type = TYPE_MONTH;
                header.year = parts[0];
                header.month = parts[1] + 1;
                Integer count = monthCounts.get(key);
                header.count = count == null ? 0 : count;
                rows.add(header);
            }

            Row row = new Row();
            row.type = TYPE_RECORD;
            row.record = record;
            row.previous = previousById.get(record.getId());
            rows.add(row);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_MONTH) {
            return new MonthHolder(ItemMonthHeaderBinding.inflate(inflater, parent, false));
        }
        return new RecordHolder(ItemRecordLedgerBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (row.type == TYPE_MONTH) {
            MonthHolder monthHolder = (MonthHolder) holder;
            monthHolder.binding.tvMonth.setText(context.getString(
                    R.string.month_header_format, row.year, row.month));
            monthHolder.binding.tvCount.setText(context.getString(
                    R.string.month_count_format, row.count));
            return;
        }

        RecordHolder recordHolder = (RecordHolder) holder;
        ItemRecordLedgerBinding binding = recordHolder.binding;
        WeightRecord record = row.record;

        binding.tvDay.setText(DateUtils.formatDayNumber(record.getDate()));
        binding.tvMonthWeek.setText(DateUtils.formatMonthWeek(record.getDate()));
        binding.tvWeight.setText(HealthUtils.formatWeight(record.getWeight()));

        String note = record.getNote() == null ? "" : record.getNote().trim();
        if (note.isEmpty()) {
            binding.tvNote.setVisibility(View.GONE);
        } else {
            binding.tvNote.setVisibility(View.VISIBLE);
            binding.tvNote.setText(note);
        }

        binding.tvMood.setText(context.getString(
                R.string.records_mood_format,
                context.getString(MoodUtils.labelRes(record.getMood()))));

        if (row.previous == null) {
            binding.tvDelta.setText(R.string.first_record);
            binding.tvDelta.setTextColor(ContextCompat.getColor(
                    context, R.color.ink_faint));
        } else {
            double delta = record.getWeight() - row.previous.getWeight();
            if (Math.abs(delta) < 0.049) {
                binding.tvDelta.setText(R.string.stat_flat);
                binding.tvDelta.setTextColor(ContextCompat.getColor(
                        context, R.color.ink_soft));
            } else if (delta < 0) {
                binding.tvDelta.setText(context.getString(
                        R.string.stat_down, HealthUtils.formatDelta(delta)));
                binding.tvDelta.setTextColor(ContextCompat.getColor(
                        context, R.color.forest));
            } else {
                binding.tvDelta.setText(context.getString(
                        R.string.stat_up, HealthUtils.formatDelta(delta)));
                binding.tvDelta.setTextColor(ContextCompat.getColor(
                        context, R.color.clay));
            }
        }

        binding.getRoot().setOnClickListener(v -> listener.onEdit(record));
        binding.btnMore.setOnClickListener(v -> showMenu(binding.btnMore, record));
    }

    private void showMenu(ImageButton anchor, WeightRecord record) {
        PopupMenu popupMenu = new PopupMenu(anchor.getContext(), anchor);
        popupMenu.inflate(R.menu.menu_record);
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_edit) {
                listener.onEdit(record);
                return true;
            }
            if (item.getItemId() == R.id.action_delete) {
                listener.onDelete(record);
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    private String monthKey(long date) {
        int[] parts = DateUtils.getYearMonth(date);
        return parts[0] + "-" + parts[1];
    }

    static class Row {
        int type;
        int year;
        int month;
        int count;
        WeightRecord record;
        WeightRecord previous;
    }

    static class MonthHolder extends RecyclerView.ViewHolder {
        final ItemMonthHeaderBinding binding;

        MonthHolder(ItemMonthHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static class RecordHolder extends RecyclerView.ViewHolder {
        final ItemRecordLedgerBinding binding;

        RecordHolder(ItemRecordLedgerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
