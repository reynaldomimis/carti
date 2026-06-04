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
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.GridLayoutManager;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseBottomSheetFragment;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.data.local.BudgetManager;
import com.upreyvan.carti.databinding.BottomSheetAddCategoryBinding;
import com.upreyvan.carti.databinding.ItemIconPickerBinding;
import com.upreyvan.carti.util.ToastHelper;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AddCategoryBottomSheet extends BaseBottomSheetFragment<BottomSheetAddCategoryBinding> {

    private int selectedIcon = R.drawable.ic_chart;
    private Uri selectedImageUri = null;
    private GenericAdapter<Integer, ItemIconPickerBinding> adapter;
    private final List<Integer> icons = Arrays.asList(
            R.drawable.ic_chart, R.drawable.ic_calendar, R.drawable.ic_trophy,
            R.drawable.ic_person, R.drawable.ic_bell, R.drawable.ic_home,
            R.drawable.ic_lock, R.drawable.ic_send, R.drawable.ic_sync,
            R.drawable.ic_email, R.drawable.ic_image, R.drawable.ic_filter
    );

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
                        adapter.submitList(new ArrayList<>(icons));
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
        setupIconPicker();
        getBinding().btnClose.setOnClickListener(v -> dismiss());
        getBinding().btnCancel.setOnClickListener(v -> dismiss());
        getBinding().btnSave.setOnClickListener(v -> saveCategory());
        getBinding().btnUploadImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
    }

    private void startCrop(@NonNull Uri uri) {
        String destinationFileName = "cropped_category_" + System.currentTimeMillis() + ".jpg";
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

    private void setupIconPicker() {
        adapter = new GenericAdapter<>(
                new DiffUtil.ItemCallback<>() {
                    @Override public boolean areItemsTheSame(@NonNull Integer oldItem, @NonNull Integer newItem) { return oldItem.equals(newItem); }
                    @Override public boolean areContentsTheSame(@NonNull Integer oldItem, @NonNull Integer newItem) { return oldItem.equals(newItem); }
                },
                (inflater, parent) -> ItemIconPickerBinding.inflate(inflater, parent, false),
                (binding, item) -> {
                    binding.ivIcon.setImageResource(item);
                    boolean isSelected = (selectedImageUri == null && item == selectedIcon);
                    
                    if (isSelected) {
                        binding.cardIcon.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.carti_primary_green)));
                        binding.cardIcon.setStrokeWidth(6);
                        binding.cardIcon.setCardBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.mint_green_alpha)));
                        binding.ivIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.carti_primary_green)));
                    } else {
                        binding.cardIcon.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.border_subtle)));
                        binding.cardIcon.setStrokeWidth(2);
                        binding.cardIcon.setCardBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white)));
                        binding.ivIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.text_secondary)));
                    }

                    binding.getRoot().setOnClickListener(v -> {
                        selectedImageUri = null;
                        selectedIcon = item;
                        adapter.submitList(new ArrayList<>(icons));
                    });
                }
        );

        getBinding().rvIcons.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        getBinding().rvIcons.setAdapter(adapter);
        adapter.submitList(icons);
    }

    private void saveCategory() {
        if (getBinding().etCategoryName.getText() == null) return;

        String name = getBinding().etCategoryName.getText().toString().trim();
        if (name.isEmpty()) {
            showToast(R.string.msg_fill_all_fields, ToastHelper.Status.ERROR);
            return;
        }

        BudgetManager.getInstance(requireContext()).updateOrAddCategory(
                name,
                selectedIcon,
                R.color.carti_primary_green,
                R.color.mint_green_alpha,
                0,
                null
        );

        dismiss();
    }
}
