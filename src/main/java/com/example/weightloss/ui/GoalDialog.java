package com.example.weightloss.ui;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.weightloss.R;
import com.example.weightloss.databinding.DialogGoalBinding;
import com.example.weightloss.util.Preferences;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

/** 目标设置对话框：校验身高与目标体重后保存。 */
public final class GoalDialog {

    private GoalDialog() {
    }

    public static void show(Context context, Preferences preferences, Runnable onSaved) {
        // 回填已保存的身高与目标体重
        DialogGoalBinding binding = DialogGoalBinding.inflate(
                LayoutInflater.from(context));
        binding.etHeight.setText(String.format(
                Locale.CHINA, "%.0f", preferences.getHeightCm()));
        if (preferences.hasTarget()) {
            binding.etTarget.setText(String.format(
                    Locale.CHINA, "%.1f", preferences.getTargetWeight()));
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.goal_dialog_title)
                .setView(binding.getRoot())
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    binding.tvHeightError.setVisibility(android.view.View.GONE);
                    binding.tvTargetError.setVisibility(android.view.View.GONE);

                    Double height = parse(binding.etHeight.getText());
                    Double target = parse(binding.etTarget.getText());

                    if (height == null || height < 100 || height > 250) {
                        binding.tvHeightError.setText(R.string.height_invalid);
                        binding.tvHeightError.setVisibility(android.view.View.VISIBLE);
                        return;
                    }

                    if (!TextUtils.isEmpty(binding.etTarget.getText()) && target == null) {
                        binding.tvTargetError.setText(R.string.target_invalid);
                        binding.tvTargetError.setVisibility(android.view.View.VISIBLE);
                        return;
                    }

                    if (target != null && (target < 20 || target > 300)) {
                        binding.tvTargetError.setText(R.string.target_range_invalid);
                        binding.tvTargetError.setVisibility(android.view.View.VISIBLE);
                        return;
                    }

                    preferences.saveProfile(height.floatValue(),
                            target == null ? 0f : target.floatValue());
                    if (onSaved != null) {
                        onSaved.run();
                    }
                    Toast.makeText(context, R.string.settings_saved, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }));

        dialog.show();
    }

    private static Double parse(CharSequence text) {
        if (text == null) {
            return null;
        }
        String value = text.toString().trim().replace(',', '.');
        if (value.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
