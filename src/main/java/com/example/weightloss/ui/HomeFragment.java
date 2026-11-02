package com.example.weightloss.ui;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.weightloss.R;
import com.example.weightloss.WeightApplication;
import com.example.weightloss.data.WeightRepository;
import com.example.weightloss.databinding.FragmentHomeBinding;
import com.example.weightloss.databinding.ItemRecentRowBinding;
import com.example.weightloss.model.WeightRecord;
import com.example.weightloss.util.DateUtils;
import com.example.weightloss.util.HealthUtils;
import com.example.weightloss.util.Preferences;
import com.example.weightloss.util.StatsCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 首页：展示最新体重、目标进度、本周统计与趋势图。 */
public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private WeightRepository repository;
    private Preferences preferences;
    private final List<WeightRecord> currentRecords = new ArrayList<>();
    private int rangeDays = 30;

    private static final String STATE_RANGE = "range_days";


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        repository = WeightApplication.repository(requireContext());
        preferences = repository.getPreferences();

        if (savedInstanceState != null) {
            rangeDays = savedInstanceState.getInt(STATE_RANGE, 30);
        }


        binding.btnRecord.setOnClickListener(v -> {
            WeightRecord today = findToday(currentRecords);
            if (today != null) {
                ((MainActivity) requireActivity()).openEditRecord(today);
            } else {
                ((MainActivity) requireActivity()).openAddRecord();
            }
        });
        binding.btnViewAll.setOnClickListener(v ->
                ((MainActivity) requireActivity()).selectTab(1));
        binding.goalSection.setOnClickListener(v ->
                GoalDialog.show(requireContext(), preferences, this::reload));

        binding.tabRange7.setOnClickListener(v -> setRange(7));
        binding.tabRange30.setOnClickListener(v -> setRange(30));
        binding.tabRangeAll.setOnClickListener(v -> setRange(0));

        updateRangeTabs();
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
        // 重新读取数据并刷新首页各区块
        currentRecords.clear();
        currentRecords.addAll(repository.getRecords());
        updateHeader();
        updateGoal();
        updateWeek();
        updateChart();
        updateInsight();
        updateRecent();

        boolean empty = currentRecords.isEmpty();
        binding.emptyBlock.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.contentBlock.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void updateHeader() {
        long today = DateUtils.today();
        String date = DateUtils.formatListDate(today) + " "
                + DateUtils.formatWeekday(today);
        binding.tvToday.setText(getString(R.string.home_today_prefix, date));

        WeightRecord latest = latest();
        WeightRecord todayRecord = findToday(currentRecords);

        if (latest == null) {
            binding.tvWeight.setText(R.string.dash);
            binding.tvUpdated.setText(R.string.home_no_record);
            binding.tvDelta.setVisibility(View.GONE);
            binding.tvMetaDot.setVisibility(View.GONE);
            binding.tvBmi.setText(R.string.bmi_unknown);
            binding.tvBmi.setTextColor(ContextCompat.getColor(
                    requireContext(), R.color.ink_faint));
            binding.tvHealthyRange.setVisibility(View.GONE);
            binding.btnRecord.setText(todayRecord != null
                    ? R.string.home_update_today : R.string.home_empty_action);
            return;
        }

        binding.tvDelta.setVisibility(View.VISIBLE);
        binding.tvMetaDot.setVisibility(View.VISIBLE);
        binding.tvWeight.setText(HealthUtils.formatWeight(latest.getWeight()));
        binding.tvUpdated.setText(getString(
                R.string.home_updated_on, DateUtils.formatRelative(latest.getDate())));
        binding.btnRecord.setText(todayRecord != null
                ? R.string.home_update_today : R.string.home_record_today);

        WeightRecord first = currentRecords.get(0);
        double change = latest.getWeight() - first.getWeight();
        binding.tvDelta.setText(getString(
                R.string.change_from_start, changeText(change)));
        binding.tvDelta.setTextColor(changeColor(change));

        double height = preferences.getHeightCm();
        double bmi = HealthUtils.calculateBmi(latest.getWeight(), height);
        binding.tvBmi.setText(getString(
                R.string.bmi_format,
                HealthUtils.formatBmi(bmi),
                getString(HealthUtils.bmiCategoryRes(bmi))));
        binding.tvBmi.setTextColor(ContextCompat.getColor(
                requireContext(), HealthUtils.bmiColorRes(bmi)));

        double[] healthy = StatsCalculator.healthyWeightRange((float) height);
        if (healthy[1] > 0) {
            binding.tvHealthyRange.setVisibility(View.VISIBLE);
            binding.tvHealthyRange.setText(getString(
                    R.string.healthy_range_format,
                    HealthUtils.formatWeight(healthy[0]),
                    HealthUtils.formatWeight(healthy[1])));
        } else {
            binding.tvHealthyRange.setVisibility(View.GONE);
        }
    }

    private void updateGoal() {
        double target = preferences.getTargetWeight();
        if (target <= 0) {
            binding.tvTarget.setText(R.string.home_target_unset);
            binding.tvTargetRemaining.setText(R.string.home_set_target);
            binding.progressTarget.setProgressCompat(0, false);
            return;
        }

        binding.tvTarget.setText(getString(
                R.string.home_target_format, HealthUtils.formatWeight(target)));

        if (currentRecords.isEmpty()) {
            binding.tvTargetRemaining.setText(R.string.home_target_waiting);
            binding.progressTarget.setProgressCompat(0, false);
            return;
        }

        WeightRecord first = currentRecords.get(0);
        WeightRecord latest = latest();
        if (target >= first.getWeight()) {
            binding.tvTargetRemaining.setText(R.string.home_target_invalid);
            binding.progressTarget.setProgressCompat(0, false);
            return;
        }

        double remaining = latest.getWeight() - target;
        if (remaining <= 0) {
            binding.tvTargetRemaining.setText(R.string.home_target_reached);
            binding.progressTarget.setProgressCompat(100, false);
            return;
        }

        double total = first.getWeight() - target;
        double lost = first.getWeight() - latest.getWeight();
        int percent = (int) Math.round(Math.max(0, Math.min(1, lost / total)) * 100);
        binding.tvTargetRemaining.setText(getString(
                R.string.home_target_remaining, HealthUtils.formatWeight(remaining)));
        binding.progressTarget.setProgressCompat(percent, false);
    }

    private void updateWeek() {
        List<WeightRecord> last7 = StatsCalculator.lastDays(currentRecords, 7);
        double change = StatsCalculator.changeInLastDays(currentRecords, 7);
        if (Double.isNaN(change)) {
            binding.tvWeekChange.setText(R.string.dash);
            binding.tvWeekChange.setTextColor(ContextCompat.getColor(
                    requireContext(), R.color.ink_soft));
        } else {
            binding.tvWeekChange.setText(compactChangeText(change));
            binding.tvWeekChange.setTextColor(changeColor(change));
        }

        double average = StatsCalculator.average(last7);
        binding.tvWeekAverage.setText(Double.isNaN(average)
                ? getString(R.string.dash) : HealthUtils.formatWeight(average));
        binding.tvWeekStreak.setText(String.format(
                Locale.CHINA, "%d", StatsCalculator.currentStreak(currentRecords)));
    }

    private void updateChart() {
        List<WeightRecord> source = currentRecords;
        if (rangeDays > 0) {
            source = StatsCalculator.lastDays(currentRecords, rangeDays);
        }
        binding.chart.setData(source, preferences.getTargetWeight());
    }

    private void updateInsight() {
        if (currentRecords.size() == 1) {
            binding.tvInsight.setText(R.string.insight_first_day);
            return;
        }

        List<WeightRecord> last7 = StatsCalculator.lastDays(currentRecords, 7);
        if (last7.isEmpty()) {
            binding.tvInsight.setText(R.string.insight_no_recent);
            return;
        }

        double average = StatsCalculator.average(last7);
        double previousAverage = StatsCalculator.averagePreviousDays(currentRecords, 7);
        StringBuilder insight = new StringBuilder();
        insight.append(getString(
                R.string.insight_last7, HealthUtils.formatWeight(average)));

        if (!Double.isNaN(previousAverage)) {
            double difference = average - previousAverage;
            if (Math.abs(difference) < 0.05) {
                insight.append(getString(R.string.insight_wow_flat));
            } else if (difference < 0) {
                insight.append(getString(
                        R.string.insight_wow_down, HealthUtils.formatDelta(difference)));
            } else {
                insight.append(getString(
                        R.string.insight_wow_up, HealthUtils.formatDelta(difference)));
            }
        }

        int streak = StatsCalculator.currentStreak(currentRecords);
        if (streak > 0) {
            insight.append(getString(R.string.insight_streak, streak));
        }

        double target = preferences.getTargetWeight();
        WeightRecord latest = latest();
        WeightRecord first = currentRecords.get(0);
        if (target > 0 && latest != null) {
            if (target >= first.getWeight()) {
                insight.append(getString(R.string.insight_goal_invalid));
            } else {
                double remaining = latest.getWeight() - target;
                if (remaining <= 0) {
                    insight.append(getString(R.string.insight_goal_reached));
                } else {
                    insight.append(getString(
                            R.string.insight_goal, HealthUtils.formatWeight(remaining)));
                }
            }
        }
        binding.tvInsight.setText(insight.toString());
    }

    private void updateRecent() {
        binding.recentContainer.removeAllViews();
        int start = Math.max(0, currentRecords.size() - 3);
        for (int i = currentRecords.size() - 1; i >= start; i--) {
            WeightRecord record = currentRecords.get(i);
            WeightRecord previous = i > 0 ? currentRecords.get(i - 1) : null;
            ItemRecentRowBinding row = ItemRecentRowBinding.inflate(
                    getLayoutInflater(), binding.recentContainer, false);

            String relative = DateUtils.isToday(record.getDate())
                    ? getString(R.string.tab_today)
                    : DateUtils.formatWeekday(record.getDate());
            row.tvRecentDate.setText(getString(
                    R.string.recent_date_format,
                    DateUtils.formatListDate(record.getDate()), relative));
            row.tvRecentWeight.setText(getString(
                    R.string.weight_with_unit, HealthUtils.formatWeight(record.getWeight())));

            if (previous == null) {
                row.tvRecentDelta.setText(R.string.dash);
                row.tvRecentDelta.setTextColor(ContextCompat.getColor(
                        requireContext(), R.color.ink_faint));
            } else {
                double delta = record.getWeight() - previous.getWeight();
                row.tvRecentDelta.setText(compactChangeText(delta));
                row.tvRecentDelta.setTextColor(changeColor(delta));
            }

            row.getRoot().setOnClickListener(v ->
                    ((MainActivity) requireActivity()).openEditRecord(record));
            binding.recentContainer.addView(row.getRoot());
        }
    }

    private WeightRecord latest() {
        return currentRecords.isEmpty()
                ? null : currentRecords.get(currentRecords.size() - 1);
    }

    private WeightRecord findToday(List<WeightRecord> records) {
        long today = DateUtils.today();
        for (WeightRecord record : records) {
            if (DateUtils.normalizeDate(record.getDate()) == today) {
                return record;
            }
        }
        return null;
    }

    private String changeText(double change) {
        if (Math.abs(change) < 0.049) {
            return getString(R.string.change_same);
        }
        if (change < 0) {
            return getString(R.string.change_down, HealthUtils.formatDelta(change));
        }
        return getString(R.string.change_up, HealthUtils.formatDelta(change));
    }

    private String compactChangeText(double change) {
        if (Math.abs(change) < 0.049) {
            return getString(R.string.stat_flat);
        }
        if (change < 0) {
            return getString(R.string.stat_down, HealthUtils.formatDelta(change));
        }
        return getString(R.string.stat_up, HealthUtils.formatDelta(change));
    }

    private int changeColor(double change) {
        if (Math.abs(change) < 0.049) {
            return ContextCompat.getColor(requireContext(), R.color.ink_soft);
        }
        return ContextCompat.getColor(requireContext(),
                change < 0 ? R.color.forest : R.color.clay);
    }

    private void setRange(int days) {
        rangeDays = days;
        updateRangeTabs();
        updateChart();
    }

    private void updateRangeTabs() {
        styleRangeTab(binding.tabRange7, binding.labelRange7,
                binding.indicatorRange7, rangeDays == 7);
        styleRangeTab(binding.tabRange30, binding.labelRange30,
                binding.indicatorRange30, rangeDays == 30);
        styleRangeTab(binding.tabRangeAll, binding.labelRangeAll,
                binding.indicatorRangeAll, rangeDays == 0);
    }

    private void styleRangeTab(View tab, android.widget.TextView label,
                               View indicator, boolean selected) {
        int color = ContextCompat.getColor(requireContext(),
                selected ? R.color.ink : R.color.ink_soft);
        label.setTextColor(color);
        label.setTypeface(Typeface.create(
                selected ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        indicator.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        tab.setContentDescription(label.getText());
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_RANGE, rangeDays);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

