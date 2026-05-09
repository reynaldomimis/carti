package com.upreyvan.carti;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.graphics.Insets;

import com.upreyvan.carti.databinding.ActivityMainBinding;
import com.upreyvan.carti.databinding.LayoutNavItemBinding;
import com.upreyvan.carti.fragments.GoalFragment;
import com.upreyvan.carti.fragments.HomeFragment;
import com.upreyvan.carti.util.Utils;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private LayoutNavItemBinding[] tabBindings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupSystemUI();

        tabBindings = new LayoutNavItemBinding[]{
                binding.tabHome, binding.tabExpenses, binding.tabAdd, binding.tabDebt, binding.tabProfile
        };

        setupTabs();
        setTabSelected(binding.tabHome);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }
    }

    private void setupTabs() {
        initTab(binding.tabHome, R.drawable.ic_home, getString(R.string.nav_home), 1);
        initTab(binding.tabExpenses, R.drawable.ic_chart, getString(R.string.nav_expenses), 2);
        initTab(binding.tabAdd, R.drawable.ic_add, getString(R.string.nav_add), -1);
        binding.tabAdd.navIcon.getLayoutParams().width = Utils.dpToPx(this, 32);
        binding.tabAdd.navIcon.getLayoutParams().height = Utils.dpToPx(this, 32);
        binding.tabAdd.navLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        initTab(binding.tabDebt, R.drawable.ic_trophy, getString(R.string.nav_debt), 3);
        initTab(binding.tabProfile, R.drawable.ic_person, getString(R.string.nav_profile), 4);
    }

    private void initTab(LayoutNavItemBinding tab, int iconRes, String label, int navId) {
        tab.navIcon.setImageResource(iconRes);
        tab.navLabel.setText(label);
        tab.getRoot().setOnClickListener(v -> {
            setTabSelected(tab);
            if (navId != -1) navigateTo(navId);
        });
    }

    private void setTabSelected(LayoutNavItemBinding selectedTab) {
        resetAllTabs();

        View tabRoot = selectedTab.getRoot();
        ImageView icon = selectedTab.navIcon;
        TextView label = selectedTab.navLabel;

        tabRoot.post(() -> {
            float centerX = tabRoot.getX() + (tabRoot.getWidth() / 2f);
            binding.curvedBg.animateCurveTo(centerX);

            float indicatorX = centerX - (binding.navIndicator.getWidth() / 2f);
            binding.navIndicator.animate()
                    .x(indicatorX)
                    .setDuration(450)
                    .setInterpolator(new OvershootInterpolator(1.0f))
                    .start();
        });

        icon.setColorFilter(ContextCompat.getColor(this, R.color.white));
        label.setTextColor(ContextCompat.getColor(this, R.color.green_primary));

        int targetY = (selectedTab == binding.tabAdd) ? -42 : -38;

        icon.animate()
                .translationY(Utils.dpToPx(this, targetY))
                .scaleX(1.15f)
                .scaleY(1.15f)
                .setDuration(450)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();

        label.animate()
                .translationY(Utils.dpToPx(this, 5))
                .setDuration(450)
                .start();
    }

    private void resetAllTabs() {
        int inactiveColor = ContextCompat.getColor(this, R.color.text_tertiary);
        int greenColor = ContextCompat.getColor(this, R.color.green_primary);

        for (LayoutNavItemBinding tab : tabBindings) {
            if (tab == binding.tabAdd) {
                tab.navIcon.setColorFilter(greenColor);
                tab.navLabel.setTextColor(greenColor);
                tab.navLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                tab.navIcon.setColorFilter(inactiveColor);
                tab.navLabel.setTextColor(inactiveColor);
                tab.navLabel.setTypeface(null, android.graphics.Typeface.NORMAL);
            }

            tab.navIcon.animate()
                    .translationY(0)
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(300)
                    .start();

            tab.navLabel.animate()
                    .translationY(0)
                    .setDuration(300)
                    .start();
        }
    }

    private void navigateTo(int id) {
        androidx.fragment.app.Fragment fragment = null;

        if (id == 1) {
            fragment = new HomeFragment();
        } else if (id == 3) {
            fragment = new GoalFragment();
        }

        if (fragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .commit();
        }
    }

    private void setupSystemUI() {
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        View decorView = window.getDecorView();
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(window, decorView);

        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);

        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavContainer, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });
    }
}
