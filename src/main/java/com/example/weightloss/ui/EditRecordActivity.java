package com.example.weightloss.ui;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.weightloss.R;
import com.example.weightloss.WeightApplication;
import com.example.weightloss.data.WeightRepository;
import com.example.weightloss.databinding.ActivityEditRecordBinding;
import com.example.weightloss.model.WeightRecord;
import com.example.weightloss.util.DateUtils;
import com.example.weightloss.util.HealthUtils;
import com.example.weightloss.util.MoodUtils;
import com.example.weightloss.util.UiUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

/** 新增/编辑记录页：录入体重、日期、心情与备注。 */
public class EditRecordActivity extends AppCompatActivity {

    public static final String EXTRA_RECORD_ID = "record_id";

    private ActivityEditRecordBinding binding;
    private WeightRepository repository;
    private WeightRecord editingRecord;
    private long selectedDate;
    private int mood = WeightRecord.MOOD_NORMAL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditRecordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.applyEdgeToEdge(this, binding.getRoot());

        repository = WeightApplication.repository(this);
        selectedDate = DateUtils.today();

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSave.setOnClickListener(v -> saveRecord());
        binding.btnDelete.setOnClickListener(v -> confirmDelete());
        binding.dateRow.setOnClickListener(v -> showDatePicker());
        binding.tvMoodRelaxed.setOnClickListener(v ->
                setMood(WeightRecord.MOOD_RELAXED));
        binding.tvMoodNormal.setOnClickListener(v ->
                setMood(WeightRecord.MOOD_NORMAL));
        binding.tvMoodTired.setOnClickListener(v ->
                setMood(WeightRecord.MOOD_TIRED));
        binding.etNote.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateNoteCounter();
            }
        });

        long recordId = getIntent().getLongExtra(EXTRA_RECORD_ID, -1L);
        if (recordId > 0) {
            editingRecord = repository.findById(recordId);
            if (editingRecord == null) {
                Toast.makeText(this, R.string.record_missing, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            binding.tvTitle.setText(R.string.edit_record);
            binding.btnDelete.setVisibility(View.VISIBLE);
            binding.etWeight.setText(HealthUtils.formatWeight(editingRecord.getWeight()));
            binding.etNote.setText(editingRecord.getNote());
            binding.etNote.setSelection(binding.etNote.getText() == null
                    ? 0 : binding.etNote.getText().length());
            selectedDate = editingRecord.getDate();
            mood = MoodUtils.normalize(editingRecord.getMood());
        } else {
            binding.tvTitle.setText(R.string.add_record);
            prefillLatestWeight();
        }

        updateDateText();
        updateMoodUi();
        updateNoteCounter();
    }

    private void prefillLatestWeight() {
        List<WeightRecord> records = repository.getRecords();
        if (!records.isEmpty()) {
            double latestWeight = records.get(records.size() - 1).getWeight();
            binding.etWeight.setText(HealthUtils.formatWeight(latestWeight));
            binding.etWeight.setSelection(binding.etWeight.getText().length());
        }
    }

    private void updateNoteCounter() {
        int length = binding.etNote.getText() == null
                ? 0 : binding.etNote.getText().length();
        binding.tvNoteCounter.setText(getString(R.string.note_counter, length));
    }

    private void updateDateText() {
        binding.tvDate.setText(DateUtils.formatListDate(selectedDate)
                + " · " + DateUtils.formatWeekday(selectedDate));
    }

    private void setMood(int newMood) {
        mood = MoodUtils.normalize(newMood);
        updateMoodUi();
    }

    private void updateMoodUi() {
        styleMood(binding.tvMoodRelaxed, mood == WeightRecord.MOOD_RELAXED);
        styleMood(binding.tvMoodNormal, mood == WeightRecord.MOOD_NORMAL);
        styleMood(binding.tvMoodTired, mood == WeightRecord.MOOD_TIRED);
    }

    private void styleMood(TextView view, boolean selected) {
        view.setBackgroundResource(selected
                ? R.drawable.bg_segment_selected : R.drawable.bg_segment_unselected);
        view.setTextColor(ContextCompat.getColor(this,
                selected ? R.color.on_forest : R.color.ink_soft));
        view.setSelected(selected);
    }

    private void showDatePicker() {
        hideKeyboard();
        int[] parts = DateUtils.getDateParts(selectedDate);
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate = DateUtils.fromDateParts(year, month, dayOfMonth);
                    updateDateText();
                },
                parts[0], parts[1], parts[2]);
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }

    private void hideKeyboard() {
        View focused = getCurrentFocus();
        if (focused == null) {
            return;
        }
        InputMethodManager manager = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);
        if (manager != null) {
            manager.hideSoftInputFromWindow(focused.getWindowToken(), 0);
        }
    }

    private void saveRecord() {
        // 校验体重/日期/备注，同日记录会提示覆盖
        binding.tvWeightError.setVisibility(View.GONE);

        String weightText = textOf(binding.etWeight.getText())
                .replace(',', '.');
        double weight;
        try {
            weight = Double.parseDouble(weightText);
        } catch (NumberFormatException exception) {
            showWeightError();
            return;
        }

        if (Double.isNaN(weight) || weight < 20 || weight > 300) {
            showWeightError();
            return;
        }

        if (selectedDate > DateUtils.today()) {
            Toast.makeText(this, R.string.date_future, Toast.LENGTH_SHORT).show();
            return;
        }

        String note = textOf(binding.etNote.getText()).trim();
        if (note.length() > 100) {
            Toast.makeText(this, R.string.note_too_long, Toast.LENGTH_SHORT).show();
            return;
        }

        if (editingRecord != null) {
            WeightRecord sameDay = repository.findByDate(selectedDate);
            if (sameDay != null && sameDay.getId() != editingRecord.getId()) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.overwrite_title)
                        .setMessage(getString(
                                R.string.overwrite_message,
                                DateUtils.formatFull(sameDay.getDate())))
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.overwrite, (dialog, which) -> {
                            repository.delete(sameDay.getId());
                            applyAndSave(editingRecord, weight, selectedDate, note, mood);
                        })
                        .show();
                return;
            }
            applyAndSave(editingRecord, weight, selectedDate, note, mood);
            return;
        }

        WeightRecord sameDay = repository.findByDate(selectedDate);
        if (sameDay != null) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.overwrite_title)
                    .setMessage(getString(
                            R.string.overwrite_message,
                            DateUtils.formatFull(sameDay.getDate())))
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.overwrite, (dialog, which) ->
                            applyAndSave(sameDay, weight, selectedDate, note, mood))
                    .show();
            return;
        }

        repository.insert(new WeightRecord(weight, selectedDate, note, mood));
        finishSaved();
    }

    private void applyAndSave(WeightRecord record, double weight, long date,
                              String note, int mood) {
        record.setWeight(weight);
        record.setDate(date);
        record.setNote(note);
        record.setMood(mood);
        repository.update(record);
        finishSaved();
    }

    private void showWeightError() {
        binding.tvWeightError.setText(R.string.weight_invalid);
        binding.tvWeightError.setVisibility(View.VISIBLE);
        binding.etWeight.requestFocus();
    }

    private void finishSaved() {
        Toast.makeText(this, R.string.record_saved, Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private void confirmDelete() {
        if (editingRecord == null) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_record_title)
                .setMessage(R.string.delete_record_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    repository.delete(editingRecord.getId());
                    Toast.makeText(this, R.string.record_deleted, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .show();
    }

    private String textOf(CharSequence text) {
        return TextUtils.isEmpty(text) ? "" : text.toString().trim();
    }
}

