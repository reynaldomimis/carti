package com.upreyvan.carti.ui.track;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import androidx.lifecycle.ViewModelProvider;
import com.upreyvan.carti.MainActivity;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.data.local.CategoryManager;
import com.upreyvan.carti.databinding.ActivityAddTrackBinding;
import com.upreyvan.carti.model.Category;
import com.upreyvan.carti.util.StringHelper;
import com.upreyvan.carti.util.UiHelper;
import com.upreyvan.carti.util.Utils;
import com.upreyvan.carti.util.Validator;
import java.util.List;

public class AddTrackActivity extends BaseActivity<ActivityAddTrackBinding> {
    private AddTrackViewModel viewModel;

    @Override protected ActivityAddTrackBinding inflateBinding(LayoutInflater inflater) { return ActivityAddTrackBinding.inflate(inflater); }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AddTrackViewModel.class);
        setupDynamicPadding(); setupToolbar(); setupDropdowns(); setupClickListeners();
        observeViewModel();
        getBinding().layoutForm.cvBalanceInfo.setVisibility(View.GONE);
    }

    private void observeViewModel() {
        viewModel.getRemainingBalance().observe(this, balance -> {
            getBinding().layoutForm.tvAllocatedBalance.setText(StringHelper.formatCurrency(balance));
        });
        
        viewModel.getSaveSuccess().observe(this, success -> {
            if (success) {
                startActivity(new Intent(AddTrackActivity.this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                finish();
            }
        });
        
        viewModel.getError().observe(this, error -> {
            if (error != null) showToast(getString(R.string.err_failed_save, error), UiHelper.Status.ERROR);
        });
        
        viewModel.getIsLoading().observe(this, loading -> showLoading(loading, getString(R.string.msg_saving_expense)));
    }

    private void setupDropdowns() {
        List<Category> cats = CategoryManager.getInstance(this).getCategories();
        String[] names = cats.stream().map(Category::getName).toArray(String[]::new);
        getBinding().actvCategory.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, names));
        if (names.length > 0) { 
            getBinding().actvCategory.setText(names[0], false); 
            viewModel.setCategory(names[0]);
            getBinding().layoutForm.cvBalanceInfo.setVisibility(View.VISIBLE);
        }
        getBinding().actvCategory.setOnItemClickListener((p, v, pos, id) -> {
            String cat = (String) p.getItemAtPosition(pos);
            viewModel.setCategory(cat);
            getBinding().layoutForm.cvBalanceInfo.setVisibility(View.VISIBLE);
        });

        String[] src = {"Cash", "GCash", "Maya", "Bank Transfer", "Credit Card"};
        getBinding().layoutForm.actvSource.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, src));
        getBinding().layoutForm.actvSource.setText(src[0], false);
    }

    private void setupDynamicPadding() {
        Utils.applySystemBarInsets(getBinding().layoutToolbar.getRoot(), getBinding().btnSave, 1f, 0);
        getBinding().scrollView.setPadding(0, 0, 0, getResources().getDimensionPixelSize(R.dimen.scroll_bottom_padding));
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.add_expense_title);
        getBinding().layoutToolbar.backButtonContainer.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        getBinding().btnSave.setOnClickListener(v -> {
            if (!checkNetwork()) return;
            if (Validator.isEmpty(getBinding().layoutForm.etAmount) || Validator.isEmpty(getBinding().actvCategory) || Validator.isEmpty(getBinding().layoutForm.etDescription)) {
                showToast(R.string.msg_fill_all_fields, UiHelper.Status.WARNING); return;
            }
            double val = StringHelper.parseDouble(getBinding().layoutForm.etAmount.getText().toString());
            String cat = getBinding().actvCategory.getText().toString();
            String desc = getBinding().layoutForm.etDescription.getText().toString();
            String source = getBinding().layoutForm.actvSource.getText().toString();
            
            viewModel.saveTrack(val, cat, desc, source);
        });
    }
}
