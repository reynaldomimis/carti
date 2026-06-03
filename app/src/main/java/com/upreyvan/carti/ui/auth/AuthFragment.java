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
import com.upreyvan.carti.MainActivity;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.data.local.PreferenceManager;
import com.upreyvan.carti.data.remote.ApiHelper;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.databinding.FragmentAuthBinding;
import com.upreyvan.carti.ui.onboarding.StartActivity;
import com.upreyvan.carti.util.ToastHelper;
import com.upreyvan.carti.util.Validator;
import java.util.Map;

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
        String[] roles = { getString(R.string.role_father), getString(R.string.role_mother), getString(R.string.role_brother), getString(R.string.role_sister), getString(R.string.role_child) };
        getBinding().actvRole.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, roles));
    }

    private void setupUI() {
        updateModeUI();
        getBinding().btnSubmit.setOnClickListener(v -> {
            if (validate()) {
                if (isLoginMode) performLogin();
                else performRegister();
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
        String email = getBinding().etEmail.getText().toString().trim();
        String password = getBinding().etPassword.getText().toString().trim();
        if (Validator.isEmpty(email) || !Validator.isValidEmail(email)) return false;
        if (Validator.isEmpty(password) || !Validator.isValidPassword(password)) return false;
        return true;
    }

    private void performLogin() {
        setLoading(true);
        AppwriteManager.getInstance(requireContext()).login(getBinding().etEmail.getText().toString().trim(), getBinding().etPassword.getText().toString().trim(), new AppwriteManager.AppwriteCallback<io.appwrite.models.Session>() {
            @Override public void onSuccess(io.appwrite.models.Session result) { fetchContext(); }
            @Override public void onError(Throwable e) { setLoading(false); showToast(e.getMessage(), ToastHelper.Status.ERROR); }
        });
    }

    private void performRegister() {
        setLoading(true);
        new ApiHelper(requireContext()).register(getBinding().etEmail.getText().toString().trim(), getBinding().etPassword.getText().toString().trim(), getBinding().etUsername.getText().toString().trim(), getBinding().cbIsEmployee.isChecked(), getBinding().actvRole.getText().toString(), new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override public void onSuccess(Map<String, Object> r) { performLogin(); }
            @Override public void onError(Throwable e) { setLoading(false); showToast(e.getMessage(), ToastHelper.Status.ERROR); }
        });
    }

    private void fetchContext() {
        new ApiHelper(requireContext()).getUser(new AppwriteManager.AppwriteCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> user) {
                PreferenceManager pref = PreferenceManager.getInstance(requireContext());
                String familyId = (user.get("familyId") != null && !"null".equals(String.valueOf(user.get("familyId")))) ? String.valueOf(user.get("familyId")) : "";
                String userId = String.valueOf(user.getOrDefault("$id", user.getOrDefault("userId", "")));
                if (userId.isEmpty() || "null".equals(userId)) userId = "";
                
                pref.setUserData(String.valueOf(user.getOrDefault("username", "User")), String.valueOf(user.getOrDefault("email", "")), String.valueOf(user.getOrDefault("role", "Member")), Boolean.parseBoolean(String.valueOf(user.getOrDefault("isEmployed", false))), familyId, String.valueOf(user.getOrDefault("inviteCode", "")), userId);
                setLoading(false);
                if (userId.isEmpty()) { showToast("Login failed: Session error.", ToastHelper.Status.ERROR); return; }
                startActivity(new Intent(requireActivity(), familyId.isEmpty() ? StartActivity.class : MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                requireActivity().finish();
            }
            @Override public void onError(Throwable e) { setLoading(false); showToast(e.getMessage(), ToastHelper.Status.ERROR); }
        });
    }

    private void setLoading(boolean l) {
        getBinding().btnSubmit.setEnabled(!l);
        getBinding().btnSubmit.setText(l ? (isLoginMode ? getString(R.string.btn_logging_in) : getString(R.string.btn_registering)) : (isLoginMode ? getString(R.string.btn_login) : getString(R.string.btn_register)));
    }
}
