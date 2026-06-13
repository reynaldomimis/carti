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
import com.upreyvan.carti.managers.CategoryManager;
import com.upreyvan.carti.databinding.ActivityAddTrackBinding;
import com.upreyvan.carti.models.Category;
import com.upreyvan.carti.utils.StringHelper;
import com.upreyvan.carti.utils.UiHelper;
import com.upreyvan.carti.utils.Utils;
import com.upreyvan.carti.utils.Validator;
import java.util.ArrayList;
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
        List<String> names = new ArrayList<>();
        for (Category item : cats) {
            if (item.getParentCategory() == null || item.getParentCategory().isEmpty()) {
                names.add(item.getName());
            }
        }
        
        names.sort((a, b) -> {
            if (a.equalsIgnoreCase("Others")) return 1;
            if (b.equalsIgnoreCase("Others")) return -1;
            return a.compareToIgnoreCase(b);
        });
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, names);
        getBinding().layoutForm.etCategory.setAdapter(adapter);
        
        getBinding().layoutForm.etCategory.setOnItemClickListener((p, v, pos, id) -> {
            String cat = (String) p.getItemAtPosition(pos);
            viewModel.setCategory(cat);
            updateSubCategoryDropdown(cat);
            getBinding().layoutForm.cvBalanceInfo.setVisibility(View.VISIBLE);
        });

        if (!names.isEmpty()) { 
            getBinding().layoutForm.etCategory.setText(names.get(0), false); 
            viewModel.setCategory(names.get(0));
            updateSubCategoryDropdown(names.get(0));
            getBinding().layoutForm.cvBalanceInfo.setVisibility(View.VISIBLE);
        }
    }

    private void updateSubCategoryDropdown(String parentCategoryName) {
        List<com.upreyvan.carti.models.Category> allCategories = com.upreyvan.carti.managers.CategoryManager.getInstance(this).getCategories();
        List<String> subCategoryNames = new ArrayList<>();
        for (com.upreyvan.carti.models.Category item : allCategories) {
            if (parentCategoryName.equalsIgnoreCase(item.getParentCategory())) {
                subCategoryNames.add(item.getName());
            }
        }

        if (!subCategoryNames.isEmpty()) {
            getBinding().layoutForm.labelSubCategory.setVisibility(View.VISIBLE);
            getBinding().layoutForm.layoutSubCategory.setVisibility(View.VISIBLE);
            
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, subCategoryNames);
            getBinding().layoutForm.etSubCategory.setAdapter(adapter);
            getBinding().layoutForm.etSubCategory.setText(""); 
        } else {
            getBinding().layoutForm.labelSubCategory.setVisibility(View.GONE);
            getBinding().layoutForm.layoutSubCategory.setVisibility(View.GONE);
            getBinding().layoutForm.etSubCategory.setText("");
        }
    }

    private void setupDynamicPadding() {
        Utils.applySystemBarInsets(getBinding().layoutToolbar.getRoot(), getBinding().btnSave, 1f, 0);
        getBinding().scrollView.setPadding(0, 0, 0, getResources().getDimensionPixelSize(R.dimen.scroll_bottom_padding));
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.add_expense_title);
        getBinding().layoutToolbar.backButtonContainer.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.backButtonContainer.setOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        getBinding().btnSave.setOnClickListener(v -> {
            if (!checkNetwork()) return;
            String cat = getBinding().layoutForm.etCategory.getText().toString().trim();
            String sub = getBinding().layoutForm.etSubCategory.getText().toString().trim();
            
            if (Validator.isEmpty(getBinding().layoutForm.etAmount) || cat.isEmpty() || Validator.isEmpty(getBinding().layoutForm.etDescription)) {
                showToast(R.string.msg_fill_all_fields, UiHelper.Status.WARNING); return;
            }
            
            double val = StringHelper.parseDouble(getBinding().layoutForm.etAmount.getText().toString());
            String desc = getBinding().layoutForm.etDescription.getText().toString();
            String finalCat = sub.isEmpty() ? cat : sub;
            
            viewModel.saveTrack(val, finalCat, desc, "Cash");
        });
    }
}
