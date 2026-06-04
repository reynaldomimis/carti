package com.upreyvan.carti.ui.allocate;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseBottomSheetFragment;
import com.upreyvan.carti.data.local.BudgetManager;
import com.upreyvan.carti.databinding.BottomSheetAddCategoryBinding;
import com.upreyvan.carti.model.IconChoice;
import com.upreyvan.carti.ui.common.IconPickerDialog;
import com.upreyvan.carti.util.ToastHelper;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.UUID;

public class AddCategoryBottomSheet extends BaseBottomSheetFragment<BottomSheetAddCategoryBinding> {

    private int selectedIcon = R.drawable.ic_chart;
    private Uri selectedImageUri = null;
    private OnCategoryAddedListener listener;
    public interface OnCategoryAddedListener {
        void onCategoryAdded(String name, double amount, boolean isRecurring);
    }

    public void setListener(OnCategoryAddedListener listener) {
        this.listener = listener;
    }

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    startCrop(uri);
                }
            }
    );

    private final ActivityResultLauncher<Intent> cropImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = UCrop.getOutput(result.getData());
                    if (selectedImageUri != null) {
                        selectedIcon = 0;
                        getBinding().ivCategoryIcon.setImageURI(selectedImageUri);
                        getBinding().ivCategoryIcon.setImageTintList(null);
                    }
                }
            }
    );

    public static AddCategoryBottomSheet newInstance() {
        return new AddCategoryBottomSheet();
    }

    @Override
    protected BottomSheetAddCategoryBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return BottomSheetAddCategoryBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getBinding().btnClose.setOnClickListener(v -> dismiss());
        getBinding().btnCancel.setOnClickListener(v -> dismiss());
        getBinding().btnSave.setOnClickListener(v -> saveCategory());
        getBinding().cardIconContainer.setOnClickListener(v -> showIconPicker());
    }

    private void showIconPicker() {
        IconPickerDialog dialog = new IconPickerDialog();
        dialog.setListener(new IconPickerDialog.OnIconSelectedListener() {
            @Override
            public void onIconSelected(IconChoice icon) {
                selectedIcon = icon.getIconRes();
                selectedImageUri = null;
                getBinding().ivCategoryIcon.setImageResource(selectedIcon);
                getBinding().ivCategoryIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.carti_primary_green)));
            }

            @Override
            public void onUploadCustom() {
                pickImageLauncher.launch("image/*");
            }
        });
        dialog.show(getChildFragmentManager(), "ICON_PICKER");
    }

    private void startCrop(@NonNull Uri uri) {
        String destinationFileName = "cropped_category_" + UUID.randomUUID() + ".jpg";
        UCrop.Options options = new UCrop.Options();
        options.setCompressionFormat(android.graphics.Bitmap.CompressFormat.JPEG);
        options.setCompressionQuality(90);
        options.setCircleDimmedLayer(true);
        options.setShowCropGrid(false);
        options.setHideBottomControls(false);
        options.setFreeStyleCropEnabled(true);

        UCrop uCrop = UCrop.of(uri, Uri.fromFile(new File(requireContext().getCacheDir(), destinationFileName)))
                .withAspectRatio(1, 1)
                .withMaxResultSize(200, 200)
                .withOptions(options);

        cropImageLauncher.launch(uCrop.getIntent(requireContext()));
    }

    private void saveCategory() {
        if (getBinding().etCategoryName.getText() == null) return;

        String name = getBinding().etCategoryName.getText().toString().trim();
        String amountStr = getBinding().etAmount.getText().toString().trim();
        boolean isRecurring = getBinding().switchRecurring.isChecked();
        
        if (name.isEmpty()) {
            ToastHelper.show(requireContext(), R.string.msg_fill_all_fields, ToastHelper.Status.ERROR);
            return;
        }

        double amount = 0;
        try {
            if (!amountStr.isEmpty()) amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException ignored) {}

        BudgetManager.getInstance(requireContext()).updateOrAddCategory(
                name,
                selectedIcon,
                R.color.carti_primary_green,
                R.color.mint_green_alpha,
                amount,
                null,
                isRecurring
        );

        if (listener != null) {
            listener.onCategoryAdded(name, amount, isRecurring);
        }

        dismiss();
    }
}
