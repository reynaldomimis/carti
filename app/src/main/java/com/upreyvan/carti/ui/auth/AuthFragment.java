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

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.databinding.FragmentAuthBinding;
import java.util.HashMap;
import java.util.Map;
import com.upreyvan.carti.MainActivity;
import com.upreyvan.carti.ui.onboarding.StartActivity;

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
    }

    private void updateModeUI() {
        if (isLoginMode) {
            getBinding().tvTitle.setText(R.string.login_title);
            getBinding().tvSubtitle.setText(R.string.login_subtitle);
            getBinding().tilUsername.setVisibility(View.GONE);
            getBinding().tilRole.setVisibility(View.GONE);
            getBinding().cbIsEmployee.setVisibility(View.GONE);
            getBinding().btnSubmit.setText(R.string.btn_login);
            getBinding().tvSwitchPrompt.setText(Html.fromHtml(getString(R.string.prompt_no_account), Html.FROM_HTML_MODE_LEGACY));
        } else {
            getBinding().tvTitle.setText(R.string.register_title);
            getBinding().tvSubtitle.setText(R.string.register_subtitle);
            getBinding().tilUsername.setVisibility(View.VISIBLE);
            getBinding().tilRole.setVisibility(View.VISIBLE);
            getBinding().cbIsEmployee.setVisibility(View.VISIBLE);
            getBinding().btnSubmit.setText(R.string.btn_register);
            getBinding().tvSwitchPrompt.setText(Html.fromHtml(getString(R.string.prompt_has_account), Html.FROM_HTML_MODE_LEGACY));
        }
        // Clear errors when switching
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
            String username = getBinding().etUsername.getText().toString().trim();
            if (username.isEmpty()) {
                getBinding().tilUsername.setError(getString(R.string.err_required));
                isValid = false;
            } else if (username.length() > 10) {
                getBinding().tilUsername.setError(getString(R.string.err_username_long));
                isValid = false;
            } else {
                getBinding().tilUsername.setError(null);
            }

            String role = getBinding().actvRole.getText().toString();
            if (role.isEmpty()) {
                getBinding().tilRole.setError(getString(R.string.err_required));
                isValid = false;
            } else {
                getBinding().tilRole.setError(null);
            }
        }

        if (email.isEmpty()) {
            getBinding().tilEmail.setError(getString(R.string.err_required));
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            getBinding().tilEmail.setError(getString(R.string.err_invalid_email));
            isValid = false;
        } else {
            getBinding().tilEmail.setError(null);
        }

        if (password.isEmpty()) {
            getBinding().tilPassword.setError(getString(R.string.err_required));
            isValid = false;
        } else if (!isLoginMode) {
            if (password.length() < 8) {
                getBinding().tilPassword.setError(getString(R.string.err_password_short));
                isValid = false;
            } else if (!password.matches(".*[A-Z].*") || !password.matches(".*[a-z].*") || !password.matches(".*[0-9].*")) {
                getBinding().tilPassword.setError(getString(R.string.err_password_weak));
                isValid = false;
            } else {
                getBinding().tilPassword.setError(null);
            }
        } else {
            getBinding().tilPassword.setError(null);
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
                fetchAndSaveUser();
            }

            @Override
            public void onError(Throwable error) {
                setLoading(false);
                Toast.makeText(requireContext(), getString(R.string.err_register_failed, error.getMessage()), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void fetchAndSaveUser() {
        ApiHelper apiHelper = new ApiHelper(requireContext());
        apiHelper.getUser(new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> userDoc) {
                PreferenceManager pref = new PreferenceManager(requireContext());
                
                String name = String.valueOf(userDoc.get("username"));
                if (name == null || "null".equals(name)) name = "User";
                
                String email = String.valueOf(userDoc.get("email"));
                String role = String.valueOf(userDoc.get("role"));
                String familyId = String.valueOf(userDoc.get("familyId"));
                
                boolean isEmployed = false;
                Object emp = userDoc.get("isEmployed");
                if (emp != null) {
                    if (emp instanceof Boolean) isEmployed = (Boolean) emp;
                    else isEmployed = Boolean.parseBoolean(String.valueOf(emp));
                }

                pref.setUserData(name, email, role, isEmployed, familyId);
                setLoading(false);
                navigateToNextScreen(isEmployed, familyId);
            }

            @Override
            public void onError(Throwable error) {
                // Fallback to basic account info if get_user action fails
                fetchBasicAccountInfo();
            }
        });
    }

    private void fetchBasicAccountInfo() {
        AppwriteManager.getInstance(requireContext()).getUser(new AppwriteManager.AppwriteCallback<>() {
            @Override
            public void onSuccess(io.appwrite.models.User<java.util.Map<String, Object>> result) {
                PreferenceManager pref = new PreferenceManager(requireContext());
                boolean isEmployed = false;
                String role = "Member";
                String familyId = "";
                if (result.getPrefs() != null && result.getPrefs().getData() != null) {
                    Object emp = result.getPrefs().getData().get("isEmployed");
                    if (emp != null) {
                        if (emp instanceof Boolean) isEmployed = (Boolean) emp;
                        else isEmployed = Boolean.parseBoolean(String.valueOf(emp));
                    }
                    
                    Object r = result.getPrefs().getData().get("role");
                    if (r != null) role = String.valueOf(r);

                    Object fid = result.getPrefs().getData().get("familyId");
                    if (fid != null) familyId = String.valueOf(fid);
                }
                pref.setUserData(result.getName(), result.getEmail(), role, isEmployed, familyId);
                setLoading(false);
                navigateToNextScreen(isEmployed, familyId);
            }

            @Override
            public void onError(Throwable error) {
                setLoading(false);
                PreferenceManager pref = new PreferenceManager(requireContext());
                String emailInput = getBinding().etEmail.getText().toString().trim();
                pref.setUserData("User", emailInput, "Member", false, "");
                navigateToNextScreen(false, "");
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

        new ApiHelper(requireContext()).register(email, password, username, isEmployed, role, new AppwriteManager.AppwriteCallback<java.util.Map<String, Object>>() {
            @Override
            public void onSuccess(java.util.Map<String, Object> result) {
                PreferenceManager pref = new PreferenceManager(requireContext());
                pref.setUserData(username, email, role, isEmployed, "");

                // Update preferences in Appwrite as well so it's persistent on the server
                java.util.Map<String, Object> userPrefs = new java.util.HashMap<>();
                userPrefs.put("isEmployed", isEmployed);
                userPrefs.put("role", role);
                userPrefs.put("familyId", "");
                
                AppwriteManager.getInstance(requireContext()).updatePrefs(userPrefs, new AppwriteManager.AppwriteCallback<>() {
                    @Override
                    public void onSuccess(io.appwrite.models.User<java.util.Map<String, Object>> result) {
                        doLogin(email, password, isEmployed);
                    }

                    @Override
                    public void onError(Throwable error) {
                        // Even if prefs update fail, try to login
                        doLogin(email, password, isEmployed);
                    }
                });
            }

            @Override
            public void onError(Throwable error) {
                setLoading(false);
                Toast.makeText(requireContext(), getString(R.string.err_register_failed, error.getMessage()), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void doLogin(String email, String password, boolean isEmployed) {
        AppwriteManager.getInstance(requireContext()).login(email, password, new AppwriteManager.AppwriteCallback<io.appwrite.models.Session>() {
            @Override
            public void onSuccess(io.appwrite.models.Session result) {
                setLoading(false);
                navigateToNextScreen(isEmployed, "");
            }

            @Override
            public void onError(Throwable error) {
                setLoading(false);
                Toast.makeText(requireContext(), getString(R.string.msg_registered_login_failed, error.getMessage()), Toast.LENGTH_LONG).show();
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

    private void navigateToNextScreen(boolean isEmployed, String familyId) {
        // SOURCE OF TRUTH: If familyId exists, go to Home. Otherwise, go to Onboarding.
        boolean needsOnboarding = familyId == null || familyId.isEmpty();
        
        Intent intent = new Intent(requireActivity(), needsOnboarding ? StartActivity.class : MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}