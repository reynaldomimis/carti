package com.upreyvan.carti.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.databinding.FragmentAuthBinding;
import com.upreyvan.carti.ui.onboarding.StartActivity;
import com.upreyvan.carti.util.UiHelper;
import com.upreyvan.carti.util.Validator;

public class AuthFragment extends BaseFragment<FragmentAuthBinding> {
    private boolean isLoginMode = true;
    private AuthViewModel viewModel;

    @Override
    protected FragmentAuthBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentAuthBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        setupUI();
        setupRoleDropdown();
        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getLoginSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success) {
                viewModel.consumeLoginSuccess();
                startActivity(new Intent(requireActivity(), SplashActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                requireActivity().finish();
            }
        });
        viewModel.getRegisterSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success) showToast("Registration successful!", UiHelper.Status.SUCCESS);
        });
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) showToast(error, UiHelper.Status.ERROR);
        });
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), this::setLoading);
    }

    private void setupRoleDropdown() {
        String[] roles = { getString(R.string.role_father), getString(R.string.role_mother), getString(R.string.role_brother), getString(R.string.role_sister), getString(R.string.role_child) };
        getBinding().actvRole.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, roles));
    }

    private void setupUI() {
        updateModeUI();
        getBinding().btnSubmit.setOnClickListener(v -> {
            if (validate()) {
                String email = getBinding().etEmail.getText().toString().trim();
                String password = getBinding().etPassword.getText().toString().trim();
                if (isLoginMode) {
                    viewModel.login(email, password);
                } else {
                    String username = getBinding().etUsername.getText().toString().trim();
                    String role = getBinding().actvRole.getText().toString();
                    viewModel.register(email, password, username, getBinding().cbIsEmployee.isChecked(), role);
                }
            }
        });
        
        getBinding().tvSwitchPrompt.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            updateModeUI();
        });
    }

    private void updateModeUI() {
        getBinding().tilUsername.setVisibility(isLoginMode ? View.GONE : View.VISIBLE);
        getBinding().tilRole.setVisibility(isLoginMode ? View.GONE : View.VISIBLE);
        getBinding().cbIsEmployee.setVisibility(isLoginMode ? View.GONE : View.VISIBLE);
        getBinding().tvTitle.setText(isLoginMode ? R.string.login_title : R.string.register_title);
        getBinding().btnSubmit.setText(isLoginMode ? R.string.btn_login : R.string.btn_register);
        getBinding().tvSwitchPrompt.setText(Html.fromHtml(isLoginMode ? getString(R.string.prompt_no_account) : getString(R.string.prompt_has_account), Html.FROM_HTML_MODE_LEGACY));
    }

    private boolean validate() {
        android.text.Editable emailText = getBinding().etEmail.getText();
        android.text.Editable passwordText = getBinding().etPassword.getText();
        String email = (emailText != null) ? emailText.toString().trim() : "";
        String password = (passwordText != null) ? passwordText.toString().trim() : "";
        
        if (!Validator.isValidEmail(email)) {
            showToast(getString(R.string.err_invalid_email), UiHelper.Status.WARNING);
            return false;
        }
        if (!Validator.isValidPassword(password)) {
            showToast("Password must be at least 8 characters", UiHelper.Status.WARNING);
            return false;
        }
        
        if (!isLoginMode) {
            if (Validator.isEmpty(getBinding().etUsername)) {
                showToast("Username is required", UiHelper.Status.WARNING);
                return false;
            }
            if (Validator.isEmpty(getBinding().actvRole)) {
                showToast("Please select your role", UiHelper.Status.WARNING);
                return false;
            }
        }
        
        return true;
    }

    private void setLoading(boolean l) {
        getBinding().btnSubmit.setEnabled(!l);
        getBinding().btnSubmit.setText(l ? (isLoginMode ? getString(R.string.btn_logging_in) : getString(R.string.btn_registering)) : (isLoginMode ? getString(R.string.btn_login) : getString(R.string.btn_register)));
    }
}
