package com.upreyvan.carti;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.data.repository.RealtimeRepository;
import com.upreyvan.carti.databinding.ActivityMainBinding;
import com.upreyvan.carti.databinding.LayoutNavItemBinding;
import com.upreyvan.carti.ui.allocate.AllocateFragment;
import com.upreyvan.carti.ui.auth.LoginActivity;
import com.upreyvan.carti.ui.common.AddOptionsActivity;
import com.upreyvan.carti.ui.family.FamilyChatFragment;
import com.upreyvan.carti.ui.goals.GoalFragment;
import com.upreyvan.carti.ui.home.HomeFragment;
import com.upreyvan.carti.ui.main.MainViewModel;
import com.upreyvan.carti.ui.onboarding.StartActivity;
import com.upreyvan.carti.ui.profile.ProfileActivity;
import com.upreyvan.carti.ui.track.IncomeModeFragment;
import com.upreyvan.carti.ui.track.TrackFragment;
import com.upreyvan.carti.util.Constants;
import com.upreyvan.carti.util.SecurityGuard;
import com.upreyvan.carti.util.ToastHelper;

public class MainActivity extends BaseActivity<ActivityMainBinding> {
    private MainViewModel viewModel;
    private LayoutNavItemBinding[] navTabs;
    private boolean isInit = false;

    private Fragment homeFragment, trackFragment, allocateFragment, chatFragment;
    private Fragment activeFragment;

    @Override
    protected ActivityMainBinding inflateBinding(LayoutInflater inflater) {
        return ActivityMainBinding.inflate(inflater);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        SecurityGuard.checkIntegrity(this);
        super.onCreate(savedInstanceState);
        setupViewModel();
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        viewModel.getAuthState().observe(this, state -> {
            switch (state) {
                case AUTHENTICATED: proceed(); break;
                case UNAUTHENTICATED: forceLogout(); break;
                case NO_FAMILY: navigateToOnboarding(); break;
                case ERROR: showToast("Connection required for first-time sync", ToastHelper.Status.ERROR); break;
            }
        });
        viewModel.validateGate();
    }

    private void forceLogout() {
        viewModel.forceLogout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToOnboarding() {
        Intent intent = new Intent(this, StartActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void proceed() {
        if (isInit) return;
        isInit = true;
        setupEdgeToEdge();
        navTabs = new LayoutNavItemBinding[]{
            getBinding().tabHome, 
            getBinding().tabExpenses, 
            getBinding().tabChat, 
            getBinding().tabAllocate, 
            getBinding().tabProfile
        };
        setupTabs();
        setupFragments();
        
        getBinding().tabAddContainer.setOnClickListener(v -> startActivity(new Intent(this, AddOptionsActivity.class)));
        RealtimeRepository.getInstance(this).startListening();
        setupBackPress();
        
        if (getIntent().getBooleanExtra("show_home", false)) {
            navigateTo(Constants.Navigation.HOME);
        }
    }

    private void setupFragments() {
        homeFragment = new HomeFragment();
        activeFragment = homeFragment;

        getSupportFragmentManager().beginTransaction()
            .add(R.id.fragment_container, homeFragment, "1")
            .commit();
        
        setTabActive(getBinding().tabHome);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null && intent.getBooleanExtra("show_home", false)) {
            navigateTo(Constants.Navigation.HOME);
        }
    }

    private void setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(getBinding().bottomNavContainer, (v, insets) -> {
            int bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            int extraPadding = getResources().getDimensionPixelSize(R.dimen.spacing_small);
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottom + extraPadding);
            return insets;
        });
    }

    private void setupTabs() {
        initTab(getBinding().tabHome, R.drawable.ic_home, getString(R.string.nav_home), Constants.Navigation.HOME);
        initTab(getBinding().tabExpenses, R.drawable.ic_chart, getString(R.string.nav_track), Constants.Navigation.TRACK);
        initTab(getBinding().tabChat, R.drawable.ai_holder, getString(R.string.menu_chat), Constants.Navigation.CHAT);
        initTab(getBinding().tabAllocate, R.drawable.ic_calendar, getString(R.string.nav_allocate), Constants.Navigation.ALLOCATE);
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
                if (trackFragment == null) trackFragment = new TrackFragment();
                target = trackFragment; 
                setTabActive(getBinding().tabExpenses); 
            } else if (id == Constants.Navigation.CHAT) { 
                if (chatFragment == null) chatFragment = new FamilyChatFragment();
                target = chatFragment; 
                setTabActive(getBinding().tabChat); 
                showBottomNav = false; 
            } else if (id == Constants.Navigation.ALLOCATE) { 
                if (allocateFragment == null) allocateFragment = AllocateFragment.newInstance(false);
                target = allocateFragment; 
                setTabActive(getBinding().tabAllocate); 
            } else if (id == Constants.Navigation.PROFILE) { 
                startActivity(new Intent(this, ProfileActivity.class)); 
                return; 
            } else if (id == Constants.Navigation.ADD) { 
                startActivity(new Intent(this, AddOptionsActivity.class)); 
                return; 
            }
        }

        if (target != null && target != activeFragment) {
            androidx.fragment.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
            
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
        
        boolean isCurrentlyVisible = getBinding().bottomNavContainer.getVisibility() == android.view.View.VISIBLE;
        if (show == isCurrentlyVisible) return;

        if (show) {
            getBinding().bottomNavContainer.setVisibility(android.view.View.VISIBLE);
            getBinding().bottomNavContainer.setAlpha(0f);
            getBinding().bottomNavContainer.setTranslationY(100f);
            getBinding().bottomNavContainer.animate()
                    .alpha(1f)
                    .translationY(0)
                    .setDuration(400)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.1f))
                    .start();
        } else {
            getBinding().bottomNavContainer.animate()
                    .alpha(0f)
                    .translationY(100f)
                    .setDuration(300)
                    .withEndAction(() -> getBinding().bottomNavContainer.setVisibility(android.view.View.GONE))
                    .start();
        }
    }

    private void setupBackPress() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                new MaterialAlertDialogBuilder(MainActivity.this)
                    .setTitle("Exit")
                    .setMessage("Are you sure you want to exit?")
                    .setPositiveButton("Yes", (d, w) -> finishAffinity())
                    .setNegativeButton("No", null)
                    .show();
            }
        });
    }
}
