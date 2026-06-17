package com.upreyvan.carti.ui.track;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.managers.CategoryManager;
import com.upreyvan.carti.databinding.ActivityAddCategoryBinding;
import com.upreyvan.carti.models.Category;
import com.upreyvan.carti.models.ColorChoice;
import com.upreyvan.carti.models.IconChoice;
import com.upreyvan.carti.ui.common.IconPickerDialog;
import com.upreyvan.carti.utils.Utils;
import com.yalantis.ucrop.UCrop;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AddCategoryActivity extends BaseActivity<ActivityAddCategoryBinding> {

    private int selectedIconRes = R.drawable.ic_add;
    private int selectedIconColor = R.color.icon_others;
    private int selectedBgColor = R.color.log_others;
    private Uri selectedCustomIconUri = null;
    private String categoryId = null;
    private Category editingCategory = null;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> { if (uri != null) startCrop(uri); }
    );

    private final ActivityResultLauncher<Intent> cropImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedCustomIconUri = UCrop.getOutput(result.getData());
                    getBinding().ivCategoryIcon.setImageURI(selectedCustomIconUri);
                    getBinding().ivCategoryIcon.setImageTintList(null);
                }
            }
    );

    @Override
    protected ActivityAddCategoryBinding inflateBinding(LayoutInflater inflater) {
        return ActivityAddCategoryBinding.inflate(inflater);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        categoryId = getIntent().getStringExtra("category_id");
        if (categoryId != null) {
            editingCategory = CategoryManager.getInstance(this).getCategoryById(categoryId);
            if (editingCategory != null) {
                selectedIconRes = editingCategory.getIconRes();
                selectedIconColor = editingCategory.getIconColor();
                selectedBgColor = editingCategory.getBackgroundColor();
            }
        }

        setupDynamicPadding();
        setupToolbar();
        setupInitialData();
        setupParentDropdown();
        setupListeners();
    }

    private void setupParentDropdown() {
        List<Category> all = CategoryManager.getInstance(this).getCategories();
        List<String> mainCategories = new java.util.ArrayList<>();
        mainCategories.add("None (Main Category)");
        
        for (Category c : all) {
            if (c.getParentCategory() == null || c.getParentCategory().isEmpty()) {
                mainCategories.add(c.getName());
            }
        }

        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, mainCategories);
        getBinding().spinnerParentCategory.setAdapter(adapter);
        
        if (editingCategory != null && editingCategory.getParentCategory() != null) {
            getBinding().spinnerParentCategory.setText(editingCategory.getParentCategory(), false);
        } else {
            getBinding().spinnerParentCategory.setText(mainCategories.get(0), false);
        }
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(categoryId == null ? getString(R.string.title_add_category) : "Edit Category");
        getBinding().layoutToolbar.backButtonContainer.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.backButtonContainer.setOnClickListener(v -> finish());
    }

    private void setupInitialData() {
        if (editingCategory != null) {
            getBinding().etCategoryName.setText(editingCategory.getName());
            getBinding().ivCategoryIcon.setImageResource(selectedIconRes);
            getBinding().ivCategoryIcon.setImageTintList(android.content.res.ColorStateList.valueOf(getColor(selectedIconColor)));
            getBinding().cardIconContainer.setCardBackgroundColor(getColor(selectedBgColor));
            getBinding().btnSave.setText("Update Category");
            getBinding().btnDelete.setVisibility(View.VISIBLE);
        }
    }

    private void setupListeners() {
        getBinding().btnDelete.setOnClickListener(v -> {
            com.upreyvan.carti.utils.DialogHelper.showConfirmation(this, "Delete Category?", 
                "Are you sure you want to delete this category? Subcategories might also be affected.", "Delete", () -> {
                showLoading(true, "Deleting...");
                CategoryManager.getInstance(this).deleteCategoryRemote(categoryId);
                onOperationSuccess("Category Deleted!");
            });
        });

        getBinding().cardIconContainer.setOnClickListener(v -> {
            IconPickerDialog dialog = new IconPickerDialog();
            dialog.setListener(new IconPickerDialog.OnIconSelectedListener() {
                @Override
                public void onIconSelected(IconChoice icon, ColorChoice color) {
                    selectedIconRes = icon.getIconRes();
                    selectedIconColor = color.getColorRes();
                    selectedBgColor = color.getBgColorRes();
                    selectedCustomIconUri = null;
                    getBinding().ivCategoryIcon.setImageResource(selectedIconRes);
                    getBinding().ivCategoryIcon.setImageTintList(android.content.res.ColorStateList.valueOf(getColor(selectedIconColor)));
                    getBinding().cardIconContainer.setCardBackgroundColor(getColor(selectedBgColor));
                }
                @Override public void onUploadCustom() { pickImageLauncher.launch("image/*"); }
            });
            dialog.show(getSupportFragmentManager(), "ICON_PICKER");
        });

        getBinding().btnSave.setOnClickListener(v -> {
            String name = getBinding().etCategoryName.getText().toString().trim();
            if (name.isEmpty()) {
                showToast("Please enter a category name", com.upreyvan.carti.utils.UiHelper.Status.WARNING);
                return;
            }

            if (!checkNetwork()) return;

            showLoading(true, categoryId == null ? "Saving category..." : "Updating category...");
            
            String parentInput = getBinding().spinnerParentCategory.getText().toString();
            String parent = (parentInput.contains("None") || parentInput.isEmpty()) ? null : parentInput;

            Category category = new Category(
                    categoryId == null ? UUID.randomUUID().toString() : categoryId,
                    name, 
                    selectedIconRes, 
                    selectedIconColor, 
                    selectedBgColor, 
                    false,
                    parent
            );
            
            com.upreyvan.carti.datasource.AppwriteManager.AppwriteCallback<java.util.Map<java.lang.String, Object>> callback = new com.upreyvan.carti.datasource.AppwriteManager.AppwriteCallback<>() {
                @Override
                public void onSuccess(java.util.Map<String, Object> result) {
                    onOperationSuccess(categoryId == null ? "Category Saved!" : "Category Updated!");
                }

                @Override
                public void onError(Throwable error) {
                    onOperationError(error);
                }
            };

            if (categoryId == null) {
                CategoryManager.getInstance(this).addCategoryRemote(category, callback);
            } else {
                CategoryManager.getInstance(this).updateCategoryRemote(categoryId, category, callback);
            }
        });
    }

    private void onOperationSuccess(String message) {
        showLoading(false);
        CategoryManager.getInstance(this).refreshRemoteCategories(com.upreyvan.carti.managers.PreferenceManager.getInstance(this).getFamilyId());
        showToast(message, com.upreyvan.carti.utils.UiHelper.Status.SUCCESS);
        finish();
    }

    private void onOperationError(Throwable error) {
        showLoading(false);
        showToast("Operation failed: " + error.getMessage(), com.upreyvan.carti.utils.UiHelper.Status.ERROR);
    }

    private void startCrop(Uri uri) {
        String dest = "cropped_category_" + UUID.randomUUID() + ".jpg";
        UCrop uCrop = UCrop.of(uri, Uri.fromFile(new File(getCacheDir(), dest)));
        uCrop.withAspectRatio(1, 1);
        uCrop.withMaxResultSize(200, 200);
        cropImageLauncher.launch(uCrop.getIntent(this));
    }

    private void setupDynamicPadding() {
        Utils.applySystemBarInsets(getBinding().layoutToolbar.getRoot(), getBinding().btnSave, 1f, 0);
        getBinding().scrollView.setPadding(0, 0, 0, getResources().getDimensionPixelSize(R.dimen.scroll_bottom_padding));
    }
}
