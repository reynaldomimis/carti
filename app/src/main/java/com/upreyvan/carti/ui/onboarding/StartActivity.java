package com.upreyvan.carti.ui.onboarding;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import android.view.LayoutInflater;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.databinding.ActivityStartBinding;
import com.upreyvan.carti.ui.onboarding.OnboardingWelcomeFragment;

public class StartActivity extends BaseActivity<ActivityStartBinding> {

    @Override
    protected ActivityStartBinding inflateBinding(LayoutInflater inflater) {
        return ActivityStartBinding.inflate(inflater);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.start_fragment_container, new OnboardingWelcomeFragment())
                    .commit();
        }
    }
}
