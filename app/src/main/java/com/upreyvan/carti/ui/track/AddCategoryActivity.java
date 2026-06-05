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
import com.upreyvan.carti.data.local.CategoryManager;
import com.upreyvan.carti.databinding.ActivityAddCategoryBinding;
import com.upreyvan.carti.model.Category;
import com.upreyvan.carti.model.IconChoice;
import com.upreyvan.carti.ui.common.IconPickerDialog;
import com.upreyvan.carti.util.Utils;
import com.yalantis.ucrop.UCrop;
import java.io.File;
import java.util.UUID;

public class AddCategoryActivity extends BaseActivity<ActivityAddCategoryBinding> {

    private int selectedIconRes = R.drawable.ic_add;
    private Uri selectedCustomIconUri = null;

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
        setupDynamicPadding();
        setupToolbar();
        setupListeners();
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText(R.string.title_add_category);
        getBinding().layoutToolbar.backButtonContainer.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> finish());
    }

    private void setupListeners() {
        getBinding().cardIconContainer.setOnClickListener(v -> {
            IconPickerDialog dialog = new IconPickerDialog();
            dialog.setListener(new IconPickerDialog.OnIconSelectedListener() {
                @Override
                public void onIconSelected(IconChoice icon) {
                    selectedIconRes = icon.getIconRes();
                    selectedCustomIconUri = null;
                    getBinding().ivCategoryIcon.setImageResource(selectedIconRes);
                    getBinding().ivCategoryIcon.setImageTintList(null);
                }
                @Override public void onUploadCustom() { pickImageLauncher.launch("image/*"); }
            });
            dialog.show(getSupportFragmentManager(), "ICON_PICKER");
        });

        getBinding().btnSave.setOnClickListener(v -> {
            String name = getBinding().etCategoryName.getText().toString().trim();
            if (name.isEmpty()) {
                showToast("Please enter a category name", com.upreyvan.carti.util.UiHelper.Status.WARNING);
                return;
            }
            Category newCategory = new Category(UUID.randomUUID().toString(), name, selectedIconRes, R.color.icon_others, R.color.log_others, false);
            CategoryManager.getInstance(this).addCategory(newCategory);
            showToast("Category Saved!", com.upreyvan.carti.util.UiHelper.Status.SUCCESS);
            finish();
        });
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
