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
            }
            String role = getBinding().actvRole.getText().toString();
            if (role.isEmpty()) {
                getBinding().tilRole.setError(getString(R.string.err_required));
                isValid = false;
            }
        }

        if (email.isEmpty()) {
            getBinding().tilEmail.setError(getString(R.string.err_required));
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            getBinding().tilEmail.setError(getString(R.string.err_invalid_email));
            isValid = false;
        }

        if (password.isEmpty()) {
            getBinding().tilPassword.setError(getString(R.string.err_required));
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
                // STEP 2: Fetch full User Context from Gateway immediately after session creation
                fetchUserContextAndNavigate();
            }

            @Override
            public void onError(Throwable error) {
                setLoading(false);
                Toast.makeText(requireContext(), "Login failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
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
                // Registration successful on gateway, now create session
                AppwriteManager.getInstance(requireContext()).login(email, password, new AppwriteManager.AppwriteCallback<io.appwrite.models.Session>() {
                    @Override
                    public void onSuccess(io.appwrite.models.Session session) {
                        fetchUserContextAndNavigate();
                    }

                    @Override
                    public void onError(Throwable error) {
                        setLoading(false);
                        Toast.makeText(requireContext(), "Registered but login failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(Throwable error) {
                setLoading(false);
                Toast.makeText(requireContext(), "Registration failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
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
                String familyId = (userDoc.get("familyId") != null && !"null".equals(String.valueOf(userDoc.get("familyId")))) 
                                  ? String.valueOf(userDoc.get("familyId")) : "";
                String inviteCode = (userDoc.get("inviteCode") != null && !"null".equals(String.valueOf(userDoc.get("inviteCode")))) 
                                  ? String.valueOf(userDoc.get("inviteCode")) : "";
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
                Toast.makeText(requireContext(), "Failed to fetch user data: " + error.getMessage(), Toast.LENGTH_LONG).show();
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
        // If familyId exists, the user is already part of a family group, go to Main.
        // Otherwise, they need to Create or Join a family in Onboarding.
        boolean needsOnboarding = familyId == null || familyId.isEmpty();
        
        Intent intent = new Intent(requireActivity(), needsOnboarding ? StartActivity.class : MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
