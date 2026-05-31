package com.upreyvan.carti;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import androidx.activity.OnBackPressedCallback;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.local.db.AppDatabase;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.ui.auth.LoginActivity;
import com.upreyvan.carti.ui.common.AddOptionsActivity;
import com.upreyvan.carti.ui.profile.ProfileActivity;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.databinding.ActivityMainBinding;
import com.upreyvan.carti.databinding.LayoutNavItemBinding;
import com.upreyvan.carti.ui.debt.DebtTrackerFragment;
import com.upreyvan.carti.ui.track.IncomeModeFragment;
import com.upreyvan.carti.ui.track.TrackFragment;
import com.upreyvan.carti.ui.ai.AiAssistantFragment;
import com.upreyvan.carti.ui.family.FamilyChatFragment;
import com.upreyvan.carti.ui.home.HomeFragment;
import com.upreyvan.carti.util.SecurityGuard;
import java.util.Map;

public class MainActivity extends BaseActivity<ActivityMainBinding> {
    private LayoutNavItemBinding[] navTabs;
    private boolean isInit = false;

    @Override protected ActivityMainBinding inflateBinding(LayoutInflater inflater) { return ActivityMainBinding.inflate(inflater); }

    @Override protected void onCreate(Bundle savedInstanceState) {
        SecurityGuard.checkIntegrity(this);
        super.onCreate(savedInstanceState);
        validateGate();
    }

    private void validateGate() {
        PreferenceManager pref = new PreferenceManager(this);
        if (pref.getUserId().isEmpty()) { forceLogout(); return; }
        if (pref.getFamilyId().isEmpty()) {
            startActivity(new Intent(this, com.upreyvan.carti.ui.onboarding.StartActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish(); return;
        }
        proceed();
        new ApiHelper(this).getUser(new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override public void onSuccess(Map<String, Object> r) {
                String fid = (r.get("familyId") != null && !"null".equals(String.valueOf(r.get("familyId")))) ? String.valueOf(r.get("familyId")) : "";
                if (!fid.isEmpty()) pref.setFamilyId(fid);
            }
            @Override public void onError(Throwable e) { if (e.getMessage() != null && e.getMessage().contains("401")) forceLogout(); }
        });
    }

    private void forceLogout() {
        new Thread(() -> {
            try { AppDatabase.getInstance(this).clearAllTables(); } catch (Exception ignored) {}
            runOnUiThread(() -> {
                new PreferenceManager(this).clear();
                startActivity(new Intent(this, LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                finish();
            });
        }).start();
    }

    private void proceed() {
        if (isInit) return; isInit = true;
        setupEdgeToEdge();
        navTabs = new LayoutNavItemBinding[]{ getBinding().tabHome, getBinding().tabExpenses, getBinding().tabAi, getBinding().tabAllocate, getBinding().tabProfile };
        setupTabs();
        setTabActive(getBinding().tabHome);
        getBinding().tabAddContainer.setOnClickListener(v -> startActivity(new Intent(this, AddOptionsActivity.class)));
        com.upreyvan.carti.data.repository.RealtimeRepository.getInstance(this).startListening();
        setupBackPress();
        if (getSupportFragmentManager().findFragmentById(R.id.fragment_container) == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new HomeFragment()).commit();
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
        initTab(getBinding().tabHome, R.drawable.ic_home, getString(R.string.nav_home), 1);
        initTab(getBinding().tabExpenses, R.drawable.ic_chart, getString(R.string.nav_track), 2);
        initTab(getBinding().tabAi, R.drawable.ai_holder, getString(R.string.menu_ai), 5);
        initTab(getBinding().tabAllocate, R.drawable.ic_calendar, getString(R.string.nav_allocate), 3);
        initTab(getBinding().tabProfile, R.drawable.ic_person, getString(R.string.nav_profile), 4);
    }

    private void initTab(LayoutNavItemBinding tab, int icon, String label, int navId) {
        tab.navIcon.setImageResource(icon); tab.navLabel.setText(label);
        tab.getRoot().setPadding(0, 0, 0, 0); 
        tab.getRoot().setOnClickListener(v -> { setTabActive(tab); navigateTo(navId); });
    }

    private void setTabActive(LayoutNavItemBinding selected) {
        int activeColor = ContextCompat.getColor(this, R.color.green_primary);
        int inactiveColor = ContextCompat.getColor(this, R.color.text_tertiary);

        for (LayoutNavItemBinding t : navTabs) {
            boolean isActive = t == selected;
            t.navIcon.setColorFilter(isActive ? activeColor : inactiveColor);
            t.navLabel.setTextColor(isActive ? activeColor : inactiveColor);
            t.navIconContainer.setBackgroundResource(isActive ? R.drawable.bg_nav_pill : 0);
            
            // AI Icon specific handling if needed
            if (t == getBinding().tabAi && isActive) t.navIcon.clearColorFilter();
        }
    }

    public void navigateTo(int id) {
        Fragment f = null;
        if (id == 1) f = new HomeFragment();
        else if (id == 2) f = new TrackFragment();
        else if (id == 3) f = com.upreyvan.carti.ui.allocate.AllocateFragment.newInstance(false);
        else if (id == 5) f = new FamilyChatFragment();
        else if (id == 6) f = new com.upreyvan.carti.ui.goals.GoalFragment();
        else if (id == 8) f = new IncomeModeFragment();
        else if (id == 4) { startActivity(new Intent(this, ProfileActivity.class)); return; }
        else if (id == 7) { startActivity(new Intent(this, AddOptionsActivity.class)); return; }
        if (f != null) getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, f).setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out).commit();
    }

    private void setupBackPress() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                new MaterialAlertDialogBuilder(MainActivity.this).setTitle("Exit").setMessage("Exit Carti?").setPositiveButton("Yes", (d, w) -> finishAffinity()).setNegativeButton("No", null).show();
            }
        });
    }
}
