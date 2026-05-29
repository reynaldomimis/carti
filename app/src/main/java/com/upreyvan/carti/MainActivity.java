package com.upreyvan.carti;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.ui.common.AddOptionsActivity;
import com.upreyvan.carti.ui.profile.ProfileActivity;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.databinding.ActivityMainBinding;
import com.upreyvan.carti.databinding.LayoutNavItemBinding;
import com.upreyvan.carti.ui.debt.DebtTrackerFragment;
import com.upreyvan.carti.ui.track.TrackFragment;
import com.upreyvan.carti.ui.family.FamilyChatFragment;
import com.upreyvan.carti.ui.home.HomeFragment;
import com.upreyvan.carti.util.Utils;

public class MainActivity extends BaseActivity<ActivityMainBinding> {

    private LayoutNavItemBinding[] tabBindings;

    @Override
    protected ActivityMainBinding inflateBinding(LayoutInflater inflater) {
        return ActivityMainBinding.inflate(inflater);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ELITE SECURITY: Check if app has been tampered with or re-signed
        com.upreyvan.carti.util.SecurityGuard.checkIntegrity(this);

        super.onCreate(savedInstanceState);

        new PreferenceManager(this).setOnboardingFinished(true);

        setupBottomNavInsets();

        tabBindings = new LayoutNavItemBinding[]{
                getBinding().tabHome, getBinding().tabExpenses, getBinding().tabAdd, getBinding().tabAi, getBinding().tabProfile
        };

        setupTabs();
        setTabSelected(getBinding().tabHome);

        com.upreyvan.carti.data.repository.RealtimeRepository.getInstance(this).startListening();

        setupBackPress();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.getBooleanExtra("show_home", false)) {
            setTabSelected(getBinding().tabHome);
            navigateTo(1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure Realtime is active when returning to the app
        com.upreyvan.carti.data.repository.RealtimeRepository.getInstance(this).startListening();
    }

    private void setupBackPress() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                new MaterialAlertDialogBuilder(MainActivity.this)
                        .setTitle("Exit Application")
                        .setMessage("Are you sure you want to exit Carti?")
                        .setCancelable(false)
                        .setPositiveButton("Yes, Exit", (dialog, which) -> finishAffinity())
                        .setNegativeButton("No", null)
                        .show();
            }
        });
    }

    private void setupBottomNavInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(getBinding().bottomNavContainer, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });
    }

    private void setupTabs() {
        initTab(getBinding().tabHome, R.drawable.ic_home, getString(R.string.nav_home), 1);
        initTab(getBinding().tabExpenses, R.drawable.ic_chart, getString(R.string.nav_track), 2);
        initTab(getBinding().tabAdd, R.drawable.ic_add, getString(R.string.nav_add), 7);
        getBinding().tabAdd.navIcon.getLayoutParams().width = getResources().getDimensionPixelSize(R.dimen.icon_size_nav);
        getBinding().tabAdd.navIcon.getLayoutParams().height = getResources().getDimensionPixelSize(R.dimen.icon_size_nav);
        getBinding().tabAdd.navLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        initTab(getBinding().tabAi, R.drawable.ai_holder, getString(R.string.menu_ai), 5);
        getBinding().tabAi.navIcon.getLayoutParams().width = getResources().getDimensionPixelSize(R.dimen.icon_size_nav);
        getBinding().tabAi.navIcon.getLayoutParams().height = getResources().getDimensionPixelSize(R.dimen.icon_size_nav);
        getBinding().tabAi.navIcon.setScaleType(ImageView.ScaleType.FIT_XY);
        getBinding().tabAi.navIcon.setImageTintList(null);
        initTab(getBinding().tabProfile, R.drawable.ic_person, getString(R.string.nav_profile), 4);
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
            getBinding().curvedBg.animateCurveTo(centerX);

            float indicatorX = centerX - (getBinding().navIndicator.getWidth() / 2f);
            getBinding().navIndicator.animate()
                    .x(indicatorX)
                    .setDuration(450)
                    .setInterpolator(new OvershootInterpolator(1.0f))
                    .start();
        });

        if (selectedTab == getBinding().tabAi) {
            icon.setImageTintList(null);
            icon.clearColorFilter();
        } else {
            icon.setColorFilter(ContextCompat.getColor(this, R.color.white));
        }

        label.setTextColor(ContextCompat.getColor(this, R.color.green_primary));

        int targetY = (selectedTab == getBinding().tabAdd) ? -42 : -38;

        icon.animate()
                .translationY(Utils.dpToPx(this, targetY))
                .scaleX(1.15f)
                .scaleY(1.15f)
                .setDuration(450)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();

        label.animate()
                .translationY(getResources().getDimensionPixelSize(R.dimen.spacing_xs))
                .setDuration(450)
                .start();
    }

    private void resetAllTabs() {
        int inactiveColor = ContextCompat.getColor(this, R.color.text_tertiary);
        int greenColor = ContextCompat.getColor(this, R.color.green_primary);

        for (LayoutNavItemBinding tab : tabBindings) {
            if (tab == getBinding().tabAdd) {
                tab.navIcon.setColorFilter(greenColor);
                tab.navLabel.setTextColor(greenColor);
                tab.navLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            } else if (tab == getBinding().tabAi) {
                tab.navIcon.setImageTintList(null);
                tab.navIcon.clearColorFilter();
                tab.navLabel.setTextColor(inactiveColor);
                tab.navLabel.setTypeface(null, android.graphics.Typeface.NORMAL);
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

    public void navigateTo(int id) {
        Fragment fragment = null;

        if (id == 1) {
            fragment = new HomeFragment();
        } else if (id == 2) {
            fragment = new TrackFragment();
        } else if (id == 3) {
            fragment = new DebtTrackerFragment();
        } else if (id == 5) {
            fragment = new FamilyChatFragment();
        } else if (id == 7) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                startActivity(new Intent(this, AddOptionsActivity.class));
            }, 300);
            return;
        } else if (id == 4) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                startActivity(new Intent(this, ProfileActivity.class));
            }, 300);
            return;
        }

        if (fragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .commit();
        }
    }
}
