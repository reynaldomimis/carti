package com.upreyvan.carti;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.repository.RealtimeRepository;
import com.upreyvan.carti.repository.TransactionRepository;
import com.upreyvan.carti.databinding.ActivityMainBinding;
import com.upreyvan.carti.databinding.LayoutNavItemBinding;
import com.upreyvan.carti.ui.allocate.PlanFragment;
import com.upreyvan.carti.ui.common.AddOptionsActivity;
import com.upreyvan.carti.ui.common.QuickAddBottomSheetFragment;
import com.upreyvan.carti.ui.family.FamilyChatFragment;
import com.upreyvan.carti.ui.home.HomeFragment;
import com.upreyvan.carti.ui.profile.ProfileFragment;
import com.upreyvan.carti.ui.track.TrackFragment;
import com.upreyvan.carti.utils.Constants;
import com.upreyvan.carti.utils.SecurityGuard;

public class MainActivity extends BaseActivity<ActivityMainBinding> {
    private LayoutNavItemBinding[] navTabs;
    private boolean isInit = false;

    private Fragment homeFragment, trackFragment, planFragment, chatFragment, profileFragment, notificationsFragment;
    private Fragment activeFragment;

    @Override
    protected ActivityMainBinding inflateBinding(LayoutInflater inflater) {
        return ActivityMainBinding.inflate(inflater);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SecurityGuard.checkIntegrity(this);
        super.onCreate(savedInstanceState);
        proceed();
    }

    private void proceed() {
        if (isInit) return;
        isInit = true;
        
        setupDynamicPadding(getBinding().fragmentContainer, getBinding().bottomNavContainer);

        navTabs = new LayoutNavItemBinding[]{
                getBinding().tabHome,
                getBinding().tabExpenses,
                getBinding().tabChat,
                getBinding().tabPlan,
                getBinding().tabProfile
        };
        setupTabs();
        setupFragments();

        getBinding().tabAddContainer.setOnClickListener(v -> QuickAddBottomSheetFragment.newInstance().show(getSupportFragmentManager(), "QuickAdd"));
        RealtimeRepository.getInstance(this).startListening();
        setupBackPress();

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                setBottomNavVisibility(false);
            } else {
                boolean show = activeFragment != chatFragment && activeFragment != profileFragment;
                setBottomNavVisibility(show);
            }
        });

        if (getIntent().getBooleanExtra("show_home", false)) {
            navigateTo(Constants.Navigation.HOME);
        }

        getBinding().bottomNavContainer.setVisibility(View.INVISIBLE);
        getBinding().bottomNavContainer.setAlpha(0f);
        getBinding().bottomNavContainer.setTranslationY(200f);
        new Handler(Looper.getMainLooper()).postDelayed(() -> setBottomNavVisibility(true), 200);
    }

    private void setupFragments() {
        homeFragment = new HomeFragment();
        trackFragment = new TrackFragment();
        planFragment = PlanFragment.newInstance(false);
        chatFragment = new FamilyChatFragment();
        profileFragment = new ProfileFragment();
        notificationsFragment = new com.upreyvan.carti.ui.notifications.NotificationsFragment();

        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, homeFragment, "1")
                .add(R.id.fragment_container, trackFragment, "2").hide(trackFragment)
                .add(R.id.fragment_container, planFragment, "3").hide(planFragment)
                .add(R.id.fragment_container, chatFragment, "4").hide(chatFragment)
                .add(R.id.fragment_container, profileFragment, "5").hide(profileFragment)
                .add(R.id.fragment_container, notificationsFragment, "6").hide(notificationsFragment)
                .commit();

        activeFragment = homeFragment;
        setTabActive(getBinding().tabHome);

        TransactionRepository.getInstance(this)
                .getSyncingStatus().observe(this, isSyncing -> {
                    if (!isSyncing) {
                        hideSyncOverlay();
                    }
                });
        new Handler(Looper.getMainLooper()).postDelayed(this::hideSyncOverlay, 5000);
    }

    private void hideSyncOverlay() {
        if (getBinding().syncOverlay != null && getBinding().syncOverlay.getVisibility() == View.VISIBLE) {
            getBinding().syncOverlay.animate()
                    .alpha(0f)
                    .setDuration(500)
                    .withEndAction(() -> getBinding().syncOverlay.setVisibility(View.GONE))
                    .start();
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        if (intent.getBooleanExtra("show_home", false)) {
            navigateTo(Constants.Navigation.HOME);
        }
    }

    private void setupTabs() {
        initTab(getBinding().tabHome, R.drawable.ic_home, getString(R.string.nav_home), Constants.Navigation.HOME);
        initTab(getBinding().tabExpenses, R.drawable.ic_chart, getString(R.string.nav_track), Constants.Navigation.TRACK);
        initTab(getBinding().tabChat, R.drawable.ai_holder, getString(R.string.menu_chat), Constants.Navigation.CHAT);
        initTab(getBinding().tabPlan, R.drawable.ic_calendar, getString(R.string.nav_plan), Constants.Navigation.PLAN);
        initTab(getBinding().tabProfile, R.drawable.ic_person, getString(R.string.nav_profile), Constants.Navigation.PROFILE);
    }

    private void initTab(LayoutNavItemBinding tab, int icon, String label, int navId) {
        tab.navIcon.setImageResource(icon);
        tab.navLabel.setText(label);
        tab.getRoot().setPadding(0, 0, 0, 0);
        tab.getRoot().setOnClickListener(v -> {
            setTabActive(tab);
            navigateTo(navId);
        });
    }

    private void setTabActive(LayoutNavItemBinding selected) {
        int activeColor = ContextCompat.getColor(this, R.color.green_primary);
        int inactiveColor = ContextCompat.getColor(this, R.color.text_tertiary);

        for (LayoutNavItemBinding t : navTabs) {
            boolean isActive = (t == selected);
            t.navIcon.setColorFilter(isActive ? activeColor : inactiveColor);
            t.navLabel.setTextColor(isActive ? activeColor : inactiveColor);
            t.navIconContainer.setBackgroundResource(isActive ? R.drawable.bg_nav_pill : 0);
        }
    }

    public void navigateTo(int id) {
        Fragment target = null;
        boolean showBottomNav = true;

        if (isInit) {
            if (id == Constants.Navigation.HOME) {
                target = homeFragment;
                setTabActive(getBinding().tabHome);
            } else if (id == Constants.Navigation.TRACK) {
                target = trackFragment;
                setTabActive(getBinding().tabExpenses);
            } else if (id == Constants.Navigation.CHAT) {
                target = chatFragment;
                setTabActive(getBinding().tabChat);
                showBottomNav = false;
            } else if (id == Constants.Navigation.PLAN) {
                target = planFragment;
                setTabActive(getBinding().tabPlan);
            } else if (id == Constants.Navigation.PROFILE) {
                target = profileFragment;
                setTabActive(getBinding().tabProfile);
                showBottomNav = false;
            } else if (id == Constants.Navigation.INCOME) {
                target = new com.upreyvan.carti.ui.track.IncomeModeFragment();
                setTabActive(getBinding().tabExpenses);
            } else if (id == Constants.Navigation.ADD) {
                startActivity(new Intent(this, AddOptionsActivity.class));
                return;
            } else if (id == Constants.Navigation.NOTIFICATIONS) {
                target = notificationsFragment;
                showBottomNav = false;
            }
        }

        if (target != null && target != activeFragment) {
            getSupportFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            int enterAnim, exitAnim;
            if (id == Constants.Navigation.CHAT || id == Constants.Navigation.PROFILE || id == Constants.Navigation.NOTIFICATIONS) {
                enterAnim = R.anim.slide_in_right;
                exitAnim = R.anim.slide_out_left;
            } else if (activeFragment == chatFragment || activeFragment == profileFragment || activeFragment == notificationsFragment) {
                enterAnim = R.anim.slide_in_left;
                exitAnim = R.anim.slide_out_right;
            } else {
                enterAnim = android.R.anim.fade_in;
                exitAnim = android.R.anim.fade_out;
            }

            transaction.setCustomAnimations(enterAnim, exitAnim);

            if (!target.isAdded()) {
                transaction.add(R.id.fragment_container, target);
            }

            transaction.hide(activeFragment).show(target).commit();
            activeFragment = target;
            setBottomNavVisibility(showBottomNav);
        }
    }

    public void setBottomNavVisibility(boolean show) {
        if (getBinding().bottomNavContainer == null) return;

        if (show) {
            if (getBinding().bottomNavContainer.getVisibility() == android.view.View.VISIBLE && getBinding().bottomNavContainer.getAlpha() == 1f) return;
            
            getBinding().bottomNavContainer.setVisibility(android.view.View.VISIBLE);
            getBinding().bottomNavContainer.animate()
                    .alpha(1f)
                    .translationY(0)
                    .setDuration(400)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.1f))
                    .start();
        } else {
            if (getBinding().bottomNavContainer.getVisibility() == android.view.View.GONE) return;

            getBinding().bottomNavContainer.animate()
                    .alpha(0f)
                    .translationY(100f)
                    .setDuration(300)
                    .withEndAction(() -> getBinding().bottomNavContainer.setVisibility(android.view.View.GONE))
                    .start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isInit) {
            RealtimeRepository.getInstance(this).stopListening();
        }
    }

    private void setupBackPress() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                    return;
                }

                if (activeFragment != homeFragment) {
                    navigateTo(Constants.Navigation.HOME);
                } else if (activeFragment == homeFragment) {
                    new MaterialAlertDialogBuilder(MainActivity.this)
                            .setTitle("Exit")
                            .setMessage("Are you sure you want to exit?")
                            .setPositiveButton("Yes", (d, w) -> finishAffinity())
                            .setNegativeButton("No", null)
                            .show();
                }
            }
        });
    }
}
