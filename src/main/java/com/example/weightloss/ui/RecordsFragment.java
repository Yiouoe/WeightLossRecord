package com.example.weightloss.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.weightloss.R;
import com.example.weightloss.WeightApplication;
import com.example.weightloss.data.WeightRepository;
import com.example.weightloss.databinding.FragmentRecordsBinding;
import com.example.weightloss.model.WeightRecord;
import com.example.weightloss.util.DateUtils;
import com.example.weightloss.util.HealthUtils;
import com.example.weightloss.util.StatsCalculator;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 记录列表页：按月分组展示、关键字搜索与删除。 */
public class RecordsFragment extends Fragment implements LedgerAdapter.Listener {

    private FragmentRecordsBinding binding;
    private WeightRepository repository;
    private LedgerAdapter adapter;
    private final List<WeightRecord> allRecords = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRecordsBinding.inflate(inflater, container, false);
        repository = WeightApplication.repository(requireContext());
        adapter = new LedgerAdapter(requireContext(), this);

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);
        binding.btnAdd.setOnClickListener(v ->
                ((MainActivity) requireActivity()).openAddRecord());
        binding.btnClearSearch.setOnClickListener(v -> {
            binding.etSearch.setText("");
            hideKeyboard();
        });

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                applyFilter();
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            loadRecords();
        }
    }

    private void loadRecords() {
        allRecords.clear();
        allRecords.addAll(repository.getRecords());
        binding.tvSubtitle.setText(getString(
                R.string.records_subtitle,
                allRecords.size(),
                StatsCalculator.distinctDays(allRecords)));
        applyFilter();
    }

    private void applyFilter() {
        // 按备注、体重或日期匹配关键字，结果倒序展示
        String query = binding.etSearch.getText() == null
                ? "" : binding.etSearch.getText().toString().trim().toLowerCase(Locale.CHINA);

        List<WeightRecord> filtered = new ArrayList<>();
        for (int i = allRecords.size() - 1; i >= 0; i--) {
            WeightRecord record = allRecords.get(i);
            if (query.isEmpty() || matches(record, query)) {
                filtered.add(record);
            }
        }

        adapter.submit(filtered, allRecords);
        binding.btnClearSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);

        if (filtered.isEmpty()) {
            binding.emptyState.setVisibility(View.VISIBLE);
            if (allRecords.isEmpty()) {
                binding.tvEmptyTitle.setText(R.string.records_empty_title);
                binding.tvEmptyDesc.setText(R.string.records_empty_desc);
            } else {
                binding.tvEmptyTitle.setText(R.string.records_no_result);
                binding.tvEmptyDesc.setText(R.string.records_no_result_desc);
            }
        } else {
            binding.emptyState.setVisibility(View.GONE);
        }
    }

    private boolean matches(WeightRecord record, String query) {
        String note = record.getNote() == null ? "" : record.getNote().toLowerCase(Locale.CHINA);
        String weight = HealthUtils.formatWeight(record.getWeight());
        String date = DateUtils.formatListDate(record.getDate()) + " "
                + DateUtils.formatFull(record.getDate()) + " "
                + DateUtils.formatMonthWeek(record.getDate());
        return note.contains(query)
                || weight.contains(query)
                || date.toLowerCase(Locale.CHINA).contains(query);
    }

    @Override
    public void onEdit(WeightRecord record) {
        ((MainActivity) requireActivity()).openEditRecord(record);
    }

    @Override
    public void onDelete(WeightRecord record) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_record_title)
                .setMessage(DateUtils.formatFull(record.getDate()) + " · "
                        + getString(R.string.weight_with_unit,
                        HealthUtils.formatWeight(record.getWeight()))
                        + "\n\n" + getString(R.string.delete_record_message))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    repository.delete(record.getId());
                    loadRecords();
                    Toast.makeText(requireContext(),
                            R.string.record_deleted, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void hideKeyboard() {
        InputMethodManager manager = (InputMethodManager) requireContext()
                .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (manager != null && binding != null) {
            manager.hideSoftInputFromWindow(
                    binding.etSearch.getWindowToken(), 0);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

