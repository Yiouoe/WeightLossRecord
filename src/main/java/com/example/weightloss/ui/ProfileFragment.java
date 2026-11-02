package com.example.weightloss.ui;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.weightloss.R;
import com.example.weightloss.WeightApplication;
import com.example.weightloss.data.WeightRepository;
import com.example.weightloss.databinding.FragmentProfileBinding;
import com.example.weightloss.databinding.ItemAchievementBinding;
import com.example.weightloss.databinding.ItemWeekDayBinding;
import com.example.weightloss.model.WeightRecord;
import com.example.weightloss.reminder.ReminderScheduler;
import com.example.weightloss.util.CsvUtils;
import com.example.weightloss.util.DateUtils;
import com.example.weightloss.util.HealthUtils;
import com.example.weightloss.util.Preferences;
import com.example.weightloss.util.StatsCalculator;
import com.example.weightloss.util.ThemeUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** 我的页：目标与统计、成就、提醒、主题以及数据导入导出。 */
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private WeightRepository repository;
    private Preferences preferences;
    private final List<WeightRecord> records = new ArrayList<>();
    private boolean updatingReminderSwitch;
    private boolean pendingReminderEnable;

    private final ActivityResultLauncher<String> exportLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("text/csv"),
                    this::onExportUri);

    private final ActivityResultLauncher<String[]> importLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                    this::onImportUri);

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    this::onNotificationPermission);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        repository = WeightApplication.repository(requireContext());
        preferences = repository.getPreferences();

        binding.btnEditGoal.setOnClickListener(v ->
                GoalDialog.show(requireContext(), preferences, this::reload));
        binding.rowTheme.setOnClickListener(v -> showThemeDialog());
        binding.rowReminder.setOnClickListener(v -> showTimePicker());
        binding.switchReminder.setOnCheckedChangeListener((button, checked) -> {
            if (updatingReminderSwitch) {
                return;
            }
            if (checked) {
                requestEnableReminder();
            } else {
                disableReminder();
            }
        });
        binding.rowExport.setOnClickListener(v -> exportRecords());
        binding.rowImport.setOnClickListener(v -> importLauncher.launch(
                new String[]{"text/*", "text/comma-separated-values", "application/csv"}));
        binding.rowClear.setOnClickListener(v -> confirmClear());
        binding.rowAbout.setOnClickListener(v -> showAbout());

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            reload();
        }
    }

    private void reload() {
        records.clear();
        records.addAll(repository.getRecords());
        updateGoal();
        updateStats();
        updateWeekStrip();
        updateAchievements();
        updateReminderUi();
        binding.tvThemeValue.setText(ThemeUtils.labelRes(preferences.getThemeMode()));
    }

    private void updateGoal() {
        float height = preferences.getHeightCm();
        double target = preferences.getTargetWeight();

        binding.tvHeight.setText(getString(
                R.string.profile_height_format, HealthUtils.formatWeight(height)));
        double[] healthy = StatsCalculator.healthyWeightRange(height);
        if (healthy[1] > 0) {
            binding.tvHealthyRange.setText(getString(
                    R.string.healthy_range_format,
                    HealthUtils.formatWeight(healthy[0]),
                    HealthUtils.formatWeight(healthy[1])));
        } else {
            binding.tvHealthyRange.setText(R.string.dash);
        }

        if (records.isEmpty()) {
            binding.tvStartWeight.setText(getString(
                    R.string.profile_start_weight, getString(R.string.dash)));
            binding.tvTotalLost.setText(getString(
                    R.string.profile_total_lost, getString(R.string.dash)));
        } else {
            WeightRecord first = records.get(0);
            WeightRecord latest = records.get(records.size() - 1);
            double lost = first.getWeight() - latest.getWeight();
            binding.tvStartWeight.setText(getString(
                    R.string.profile_start_weight,
                    HealthUtils.formatWeight(first.getWeight())));
            if (lost < -0.049) {
                binding.tvTotalLost.setText(getString(
                        R.string.profile_total_gained,
                        HealthUtils.formatWeight(-lost)));
            } else {
                binding.tvTotalLost.setText(getString(
                        R.string.profile_total_lost,
                        HealthUtils.formatWeight(Math.max(0, lost))));
            }
        }

        if (target <= 0) {
            binding.tvTargetWeight.setText(R.string.dash);
            binding.tvTargetStatus.setText(R.string.profile_goal_unset);
            binding.tvGoalRemaining.setText(R.string.profile_target_weight);
            return;
        }

        binding.tvTargetWeight.setText(HealthUtils.formatWeight(target));
        binding.tvGoalRemaining.setText(getString(
                R.string.profile_target_value, HealthUtils.formatWeight(target)));

        if (records.isEmpty()) {
            binding.tvTargetStatus.setText(R.string.home_target_waiting);
            return;
        }

        WeightRecord first = records.get(0);
        WeightRecord latest = records.get(records.size() - 1);
        if (target >= first.getWeight()) {
            binding.tvTargetStatus.setText(R.string.home_target_invalid);
            return;
        }

        double remaining = latest.getWeight() - target;
        if (remaining <= 0) {
            binding.tvTargetStatus.setText(R.string.profile_goal_reached);
        } else {
            binding.tvTargetStatus.setText(getString(
                    R.string.profile_goal_remaining, HealthUtils.formatWeight(remaining)));
        }
    }

    private void updateStats() {
        int streak = StatsCalculator.currentStreak(records);
        int best = StatsCalculator.bestStreak(records);
        binding.tvStreakNumber.setText(String.format(Locale.CHINA, "%d", streak));
        binding.tvBestStreak.setText(getString(R.string.profile_best_format, best));
    }

    private void updateWeekStrip() {
        if (binding == null) {
            return;
        }
        // 生成以周一为首的本周七日打卡指示
        binding.weekStrip.removeAllViews();
        String[] labels = getResources().getStringArray(R.array.week_day_short);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(DateUtils.today());
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int offset = (dayOfWeek + 5) % 7;
        calendar.add(Calendar.DAY_OF_MONTH, -offset);

        for (int i = 0; i < 7; i++) {
            ItemWeekDayBinding item = ItemWeekDayBinding.inflate(
                    getLayoutInflater(), binding.weekStrip, false);
            long day = calendar.getTimeInMillis();

            item.tvWeekDay.setText(labels[i]);
            item.tvWeekDate.setText(String.format(
                    Locale.CHINA, "%d", calendar.get(Calendar.DAY_OF_MONTH)));
            item.dot.setBackgroundResource(StatsCalculator.hasRecordOn(day, records)
                    ? R.drawable.bg_dot_on : R.drawable.bg_dot_off);

            if (DateUtils.isToday(day)) {
                item.tvWeekDate.setTextColor(ContextCompat.getColor(
                        requireContext(), R.color.forest));
            } else {
                item.tvWeekDate.setTextColor(ContextCompat.getColor(
                        requireContext(), R.color.ink_soft));
            }

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            binding.weekStrip.addView(item.getRoot(), params);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private void updateAchievements() {
        binding.achievementContainer.removeAllViews();

        int distinctDays = StatsCalculator.distinctDays(records);
        int bestStreak = StatsCalculator.bestStreak(records);
        double lost = records.isEmpty() ? 0
                : records.get(0).getWeight() - records.get(records.size() - 1).getWeight();
        double target = preferences.getTargetWeight();
        boolean goalReached = target > 0 && !records.isEmpty()
                && target < records.get(0).getWeight()
                && records.get(records.size() - 1).getWeight() <= target;

        addAchievement(R.string.achievement_first, R.string.achievement_first_desc,
                !records.isEmpty());
        addAchievement(R.string.achievement_streak, R.string.achievement_streak_desc,
                bestStreak >= 3);
        addAchievement(R.string.achievement_lost, R.string.achievement_lost_desc,
                lost >= 1);
        addAchievement(R.string.achievement_ten, R.string.achievement_ten_desc,
                distinctDays >= 10);
        addAchievement(R.string.achievement_goal, R.string.achievement_goal_desc,
                goalReached);
    }

    private void addAchievement(int titleRes, int descRes, boolean done) {
        ItemAchievementBinding item = ItemAchievementBinding.inflate(
                getLayoutInflater(), binding.achievementContainer, false);
        item.tvTitle.setText(titleRes);
        item.tvDesc.setText(descRes);
        item.tvStatus.setText(done ? R.string.achievement_done : R.string.achievement_pending);
        item.tvStatus.setTextColor(ContextCompat.getColor(
                requireContext(), done ? R.color.forest : R.color.ink_faint));
        item.ivCheck.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(
                requireContext(), done ? R.color.forest : R.color.line)));
        item.ivCheck.setContentDescription(getString(done
                ? R.string.cd_achievement_done : R.string.cd_achievement_pending));
        binding.achievementContainer.addView(item.getRoot());
    }

    private void updateReminderUi() {
        if (binding == null) {
            return;
        }
        boolean enabled = preferences.isReminderEnabled();
        updatingReminderSwitch = true;
        binding.switchReminder.setChecked(enabled);
        updatingReminderSwitch = false;

        String time = getString(R.string.time_format,
                preferences.getReminderHour(), preferences.getReminderMinute());
        binding.tvReminderValue.setText(enabled
                ? getString(R.string.reminder_on, time)
                : getString(R.string.reminder_off));
    }

    private void showTimePicker() {
        TimePickerDialog dialog = new TimePickerDialog(
                requireContext(),
                (view, hour, minute) -> {
                    preferences.setReminderTime(hour, minute);
                    if (preferences.isReminderEnabled()) {
                        ReminderScheduler.schedule(requireContext(), hour, minute);
                    }
                    updateReminderUi();
                },
                preferences.getReminderHour(),
                preferences.getReminderMinute(),
                true);
        dialog.show();
    }

    private void requestEnableReminder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            pendingReminderEnable = true;
            updatingReminderSwitch = true;
            binding.switchReminder.setChecked(false);
            updatingReminderSwitch = false;
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }
        enableReminder();
    }

    private void onNotificationPermission(boolean granted) {
        if (granted && pendingReminderEnable) {
            enableReminder();
        } else {
            if (!granted) {
                Toast.makeText(requireContext(),
                        R.string.reminder_permission_denied, Toast.LENGTH_SHORT).show();
            }
            updateReminderUi();
        }
        pendingReminderEnable = false;
    }

    private void enableReminder() {
        preferences.setReminderEnabled(true);
        ReminderScheduler.schedule(requireContext(),
                preferences.getReminderHour(), preferences.getReminderMinute());
        updateReminderUi();
        Toast.makeText(requireContext(), R.string.reminder_enabled, Toast.LENGTH_SHORT).show();
    }

    private void disableReminder() {
        preferences.setReminderEnabled(false);
        ReminderScheduler.cancel(requireContext());
        updateReminderUi();
        Toast.makeText(requireContext(), R.string.reminder_disabled, Toast.LENGTH_SHORT).show();
    }

    private void showThemeDialog() {
        String[] labels = {
                getString(R.string.theme_system),
                getString(R.string.theme_light),
                getString(R.string.theme_dark)
        };
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.theme_title)
                .setSingleChoiceItems(labels, preferences.getThemeMode(),
                        (dialog, which) -> {
                            preferences.setThemeMode(which);
                            ThemeUtils.apply(which);
                            dialog.dismiss();
                        })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void exportRecords() {
        if (records.isEmpty()) {
            Toast.makeText(requireContext(), R.string.export_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        String date = new SimpleDateFormat("yyyyMMdd", Locale.CHINA).format(new Date());
        exportLauncher.launch("weight-records-" + date + ".csv");
    }

    private void onExportUri(@Nullable Uri uri) {
        if (uri == null || binding == null) {
            return;
        }
        try (OutputStream outputStream = requireContext().getContentResolver()
                .openOutputStream(uri)) {
            if (outputStream == null) {
                throw new IOException("Unable to open output stream");
            }
            CsvUtils.export(outputStream, records);
            Toast.makeText(requireContext(), getString(
                    R.string.export_success, records.size()), Toast.LENGTH_SHORT).show();
        } catch (IOException exception) {
            Toast.makeText(requireContext(), R.string.export_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void onImportUri(@Nullable Uri uri) {
        // 逐条导入，已存在的日期跳过
        if (uri == null || binding == null) {
            return;
        }
        int imported = 0;
        int skipped = 0;
        try (InputStream inputStream = requireContext().getContentResolver()
                .openInputStream(uri)) {
            if (inputStream == null) {
                throw new IOException("Unable to open input stream");
            }
            List<CsvUtils.CsvRecord> csvRecords = CsvUtils.read(inputStream);
            for (CsvUtils.CsvRecord csvRecord : csvRecords) {
                if (repository.findByDate(csvRecord.date) != null) {
                    skipped++;
                    continue;
                }
                repository.insert(new WeightRecord(
                        csvRecord.weight, csvRecord.date, csvRecord.note, csvRecord.mood));
                imported++;
            }
            reload();
            Toast.makeText(requireContext(), getString(
                    R.string.import_result, imported, skipped), Toast.LENGTH_LONG).show();
        } catch (IOException exception) {
            Toast.makeText(requireContext(), R.string.import_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmClear() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_all_title)
                .setMessage(R.string.delete_all_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    repository.deleteAll();
                    reload();
                    Toast.makeText(requireContext(),
                            R.string.data_cleared, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showAbout() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.about_app)
                .setMessage(R.string.about_message)
                .setPositiveButton(R.string.confirm, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
