package com.upreyvan.carti.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.upreyvan.carti.MainActivity;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.databinding.FragmentAuthBinding;
import com.upreyvan.carti.ui.onboarding.StartActivity;

import java.util.HashMap;
import java.util.Map;

/**
 * Senior Developer Refactored: AuthFragment handles Login and Registration.
 * It ensures the local User Session matches the Gateway's User Context.
 */
import com.upreyvan.carti.util.Validator;

public class AuthFragment extends BaseFragment<FragmentAuthBinding> {

    private boolean isLoginMode = true;

    @Override
    protected FragmentAuthBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentAuthBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupUI();
        setupRoleDropdown();
        setupErrorClearing();
    }

    private void setupErrorClearing() {
        android.text.TextWatcher clearErrorWatcher = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                getBinding().tilUsername.setError(null);
                getBinding().tilEmail.setError(null);
                getBinding().tilPassword.setError(null);
                getBinding().tilRole.setError(null);
            }
        };

        getBinding().etUsername.addTextChangedListener(clearErrorWatcher);
        getBinding().etEmail.addTextChangedListener(clearErrorWatcher);
        getBinding().etPassword.addTextChangedListener(clearErrorWatcher);
        getBinding().actvRole.addTextChangedListener(clearErrorWatcher);
    }

    private void setupRoleDropdown() {
        String[] roles = {
                getString(R.string.role_father),
                getString(R.string.role_mother),
                getString(R.string.role_brother),
                getString(R.string.role_sister),
                getString(R.string.role_child)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, roles);
        getBinding().actvRole.setAdapter(adapter);
    }

    private void setupUI() {
        updateModeUI();

        getBinding().btnSubmit.setOnClickListener(v -> {
            if (validate()) {
                if (isLoginMode) {
                    performLogin();
                } else {
                    performRegister();
                }
            }
        });

        getBinding().tvSwitchPrompt.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            updateModeUI();
        });

        getBinding().tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    private void showForgotPasswordDialog() {
        String email = getBinding().etEmail.getText().toString().trim();
        if (Validator.isEmpty(email) || !Validator.isValidEmail(email)) {
            getBinding().tilEmail.setError(getString(R.string.err_invalid_email));
            return;
        }

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Reset Password")
                .setMessage("Send password reset link to " + email + "?")
                .setPositiveButton("Send", (dialog, which) -> {
                    setLoading(true);
                    AppwriteManager.getInstance(requireContext()).createPasswordRecovery(
                            email,
                            new AppwriteManager.AppwriteCallback<>() {
                                @Override
                                public void onSuccess(Object result) {
                                    setLoading(false);
                                    showToast(R.string.msg_reset_link_sent, com.upreyvan.carti.util.ToastHelper.Status.SUCCESS);
                                }

                                @Override
                                public void onError(Throwable error) {
                                    setLoading(false);
                                    showToast("Error: " + error.getMessage(), com.upreyvan.carti.util.ToastHelper.Status.ERROR);
                                }
                            }
                    );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateModeUI() {
        if (isLoginMode) {
            getBinding().tvTitle.setText(R.string.login_title);
            getBinding().tvSubtitle.setText(R.string.login_subtitle);
            getBinding().tilUsername.setVisibility(View.GONE);
            getBinding().tilRole.setVisibility(View.GONE);
            getBinding().cbIsEmployee.setVisibility(View.GONE);
            getBinding().btnSubmit.setText(R.string.btn_login);
            getBinding().tvForgotPassword.setVisibility(View.VISIBLE);
            getBinding().tvSwitchPrompt.setText(Html.fromHtml(getString(R.string.prompt_no_account), Html.FROM_HTML_MODE_LEGACY));
        } else {
            getBinding().tvTitle.setText(R.string.register_title);
            getBinding().tvSubtitle.setText(R.string.register_subtitle);
            getBinding().tilUsername.setVisibility(View.VISIBLE);
            getBinding().tilRole.setVisibility(View.VISIBLE);
            getBinding().cbIsEmployee.setVisibility(View.VISIBLE);
            getBinding().btnSubmit.setText(R.string.btn_register);
            getBinding().tvForgotPassword.setVisibility(View.GONE);
            getBinding().tvSwitchPrompt.setText(Html.fromHtml(getString(R.string.prompt_has_account), Html.FROM_HTML_MODE_LEGACY));
        }
        getBinding().tilUsername.setError(null);
        getBinding().tilEmail.setError(null);
        getBinding().tilPassword.setError(null);
        getBinding().tilRole.setError(null);
    }

    private boolean validate() {
        boolean isValid = true;
        String email = getBinding().etEmail.getText().toString().trim();
        String password = getBinding().etPassword.getText().toString().trim();

        if (!isLoginMode) {
            if (Validator.isEmpty(getBinding().etUsername)) {
                getBinding().tilUsername.setError(getString(R.string.err_required));
                isValid = false;
            }
            if (Validator.isEmpty(getBinding().actvRole)) {
                getBinding().tilRole.setError(getString(R.string.err_required));
                isValid = false;
            }
        }

        if (Validator.isEmpty(email)) {
            getBinding().tilEmail.setError(getString(R.string.err_required));
            isValid = false;
        } else if (!Validator.isValidEmail(email)) {
            getBinding().tilEmail.setError(getString(R.string.err_invalid_email));
            isValid = false;
        }

        if (Validator.isEmpty(password)) {
            getBinding().tilPassword.setError(getString(R.string.err_required));
            isValid = false;
        } else if (!Validator.isValidPassword(password)) {
            getBinding().tilPassword.setError(getString(R.string.msg_password_short));
            isValid = false;
        }

        return isValid;
    }

    private void performLogin() {
        String email = getBinding().etEmail.getText().toString().trim();
        String password = getBinding().etPassword.getText().toString().trim();

        setLoading(true);

        AppwriteManager.getInstance(requireContext()).login(email, password, new AppwriteManager.AppwriteCallback<io.appwrite.models.Session>() {
            @Override
            public void onSuccess(io.appwrite.models.Session result) {
                fetchUserContextAndNavigate();
            }

            @Override
            public void onError(Throwable error) {
                setLoading(false);
                showToast("Login failed: " + error.getMessage(), com.upreyvan.carti.util.ToastHelper.Status.ERROR);
            }
        });
    }

    private void performRegister() {
        String username = getBinding().etUsername.getText().toString().trim();
        String email = getBinding().etEmail.getText().toString().trim();
        String password = getBinding().etPassword.getText().toString().trim();
        String role = getBinding().actvRole.getText().toString();
        boolean isEmployed = getBinding().cbIsEmployee.isChecked();

        setLoading(true);

        new ApiHelper(requireContext()).register(email, password, username, isEmployed, role, new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                AppwriteManager.getInstance(requireContext()).login(email, password, new AppwriteManager.AppwriteCallback<io.appwrite.models.Session>() {
                    @Override
                    public void onSuccess(io.appwrite.models.Session session) {
                        fetchUserContextAndNavigate();
                    }

                    @Override
                    public void onError(Throwable error) {
                        setLoading(false);
                        showToast("Registered but login failed: " + error.getMessage(), com.upreyvan.carti.util.ToastHelper.Status.ERROR);
                    }
                });
            }

            @Override
            public void onError(Throwable error) {
                setLoading(false);
                showToast("Registration failed: " + error.getMessage(), com.upreyvan.carti.util.ToastHelper.Status.ERROR);
            }
        });
    }

    private void fetchUserContextAndNavigate() {
        new ApiHelper(requireContext()).getUser(new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> userDoc) {
                PreferenceManager pref = new PreferenceManager(requireContext());

                String name = String.valueOf(userDoc.getOrDefault("username", "User"));
                String email = String.valueOf(userDoc.getOrDefault("email", ""));
                String role = String.valueOf(userDoc.getOrDefault("role", "Member"));
                String familyId = (userDoc.get("familyId") != null && !"null".equals(String.valueOf(userDoc.get("familyId")))) ? String.valueOf(userDoc.get("familyId")) : "";
                String inviteCode = (userDoc.get("inviteCode") != null && !"null".equals(String.valueOf(userDoc.get("inviteCode")))) ? String.valueOf(userDoc.get("inviteCode")) : "";
                String userId = String.valueOf(userDoc.getOrDefault("$id", ""));

                boolean isEmployed = false;
                Object emp = userDoc.get("isEmployed");
                if (emp instanceof Boolean) isEmployed = (Boolean) emp;
                else if (emp != null) isEmployed = Boolean.parseBoolean(String.valueOf(emp));

                pref.setUserData(name, email, role, isEmployed, familyId, inviteCode, userId);

                setLoading(false);
                navigateToNextScreen(familyId);
            }

            @Override
            public void onError(Throwable error) {
                setLoading(false);
                showToast("Failed to fetch user data: " + error.getMessage(), com.upreyvan.carti.util.ToastHelper.Status.ERROR);
            }
        });
    }

    private void setLoading(boolean isLoading) {
        getBinding().btnSubmit.setEnabled(!isLoading);
        if (isLoading) {
            getBinding().btnSubmit.setText(isLoginMode ? R.string.btn_logging_in : R.string.btn_registering);
        } else {
            getBinding().btnSubmit.setText(isLoginMode ? R.string.btn_login : R.string.btn_register);
        }
    }

    private void navigateToNextScreen(String familyId) {
        boolean needsOnboarding = familyId == null || familyId.isEmpty();

        Intent intent = new Intent(requireActivity(), needsOnboarding ? StartActivity.class : MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
