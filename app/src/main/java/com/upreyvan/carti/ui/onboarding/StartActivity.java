package com.upreyvan.carti.ui.onboarding;


import android.os.Bundle;
import android.view.LayoutInflater;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.databinding.ActivityStartBinding;

public class StartActivity extends BaseActivity<ActivityStartBinding> {

    @Override
    protected ActivityStartBinding inflateBinding(LayoutInflater inflater) {
        return ActivityStartBinding.inflate(inflater);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            boolean isWaiting = getIntent().getBooleanExtra("is_waiting", false);
            boolean isDeclined = getIntent().getBooleanExtra("is_declined", false);
            String inviteCode = getIntent().getStringExtra("invite_code");

            androidx.fragment.app.Fragment initialFragment;
            if (isDeclined) {
                initialFragment = OnboardingStatusFragment.newInstanceForDeclined();
            } else if (isWaiting) {
                initialFragment = OnboardingStatusFragment.newInstanceForWaiting();
            } else if (inviteCode != null) {
                initialFragment = OnboardingStatusFragment.newInstance(inviteCode);
            } else {
                initialFragment = new OnboardingWelcomeFragment();
            }

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.start_fragment_container, initialFragment)
                    .commit();
        }
    }
}
