package com.example.weightloss.ui;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.weightloss.R;
import com.example.weightloss.WeightApplication;
import com.example.weightloss.data.WeightRepository;
import com.example.weightloss.databinding.ActivityMainBinding;
import com.example.weightloss.model.WeightRecord;
import com.example.weightloss.util.UiUtils;

/** 主界面：底部标签切换「今日 / 记录 / 我的」三个页面。 */
public class MainActivity extends AppCompatActivity {

    private static final String STATE_TAB = "selected_tab";
    private static final String TAG_HOME = "home";
    private static final String TAG_RECORDS = "records";
    private static final String TAG_PROFILE = "profile";

    private ActivityMainBinding binding;
    private WeightRepository repository;
    private int selectedTab;
    private Typeface mediumTypeface;
    private Typeface regularTypeface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.applyEdgeToEdge(this, binding.getRoot());

        repository = WeightApplication.repository(this);
        selectedTab = savedInstanceState == null ? 0 : savedInstanceState.getInt(STATE_TAB, 0);
        mediumTypeface = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        regularTypeface = Typeface.create("sans-serif", Typeface.NORMAL);

        binding.tabToday.setOnClickListener(v -> selectTab(0));
        binding.tabRecords.setOnClickListener(v -> selectTab(1));
        binding.tabProfile.setOnClickListener(v -> selectTab(2));

        selectTab(selectedTab);
    }

    public WeightRepository getRepository() {
        return repository;
    }

    /** 切换标签，复用已创建的 Fragment。 */
    public void selectTab(int index) {
        if (index < 0 || index > 2) {
            return;
        }
        selectedTab = index;
        updateTabs();

        String tag = index == 1 ? TAG_RECORDS : index == 2 ? TAG_PROFILE : TAG_HOME;
        FragmentManager manager = getSupportFragmentManager();
        Fragment target = manager.findFragmentByTag(tag);
        FragmentTransaction transaction = manager.beginTransaction();

        for (Fragment fragment : manager.getFragments()) {
            if (fragment != null && fragment.isAdded() && fragment != target) {
                transaction.hide(fragment);
            }
        }

        if (target == null) {
            target = createFragment(index);
            transaction.add(R.id.fragmentContainer, target, tag);
        } else {
            transaction.show(target);
        }

        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .commitAllowingStateLoss();
    }

    private Fragment createFragment(int index) {
        if (index == 1) {
            return new RecordsFragment();
        }
        if (index == 2) {
            return new ProfileFragment();
        }
        return new HomeFragment();
    }

    private void updateTabs() {
        int active = ContextCompat.getColor(this, R.color.forest);
        int inactive = ContextCompat.getColor(this, R.color.ink_soft);

        binding.labelToday.setTextColor(selectedTab == 0 ? active : inactive);
        binding.labelToday.setTypeface(selectedTab == 0 ? mediumTypeface : regularTypeface);
        binding.indicatorToday.setVisibility(
                selectedTab == 0 ? View.VISIBLE : View.INVISIBLE);

        binding.labelRecords.setTextColor(selectedTab == 1 ? active : inactive);
        binding.labelRecords.setTypeface(selectedTab == 1 ? mediumTypeface : regularTypeface);
        binding.indicatorRecords.setVisibility(
                selectedTab == 1 ? View.VISIBLE : View.INVISIBLE);

        binding.labelProfile.setTextColor(selectedTab == 2 ? active : inactive);
        binding.labelProfile.setTypeface(selectedTab == 2 ? mediumTypeface : regularTypeface);
        binding.indicatorProfile.setVisibility(
                selectedTab == 2 ? View.VISIBLE : View.INVISIBLE);
    }

    public void openAddRecord() {
        startActivity(new Intent(this, EditRecordActivity.class));
    }

    public void openEditRecord(WeightRecord record) {
        Intent intent = new Intent(this, EditRecordActivity.class);
        intent.putExtra(EditRecordActivity.EXTRA_RECORD_ID, record.getId());
        startActivity(intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_TAB, selectedTab);
    }
}
