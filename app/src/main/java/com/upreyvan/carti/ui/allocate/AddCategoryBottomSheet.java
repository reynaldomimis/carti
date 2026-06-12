package com.upreyvan.carti.ui.allocate;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseBottomSheetFragment;
import com.upreyvan.carti.repository.TransactionRepository;
import com.upreyvan.carti.databinding.BottomSheetAddCategoryBinding;
import com.upreyvan.carti.models.BudgetCategoryItem;
import com.upreyvan.carti.models.IconChoice;
import com.upreyvan.carti.ui.common.IconPickerDialog;
import com.upreyvan.carti.utils.UiHelper;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class AddCategoryBottomSheet extends BaseBottomSheetFragment<BottomSheetAddCategoryBinding> {

    private int selectedIcon = R.drawable.ic_chart;
    private Uri selectedImageUri = null;
    private String parentCategory = null;
    private BudgetCategoryItem editingItem = null;
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

    public static AddCategoryBottomSheet newInstance(String parentCategory) {
        AddCategoryBottomSheet fragment = new AddCategoryBottomSheet();
        Bundle args = new Bundle();
        args.putString("parent_category", parentCategory);
        fragment.setArguments(args);
        return fragment;
    }

    public static AddCategoryBottomSheet newInstance(BudgetCategoryItem item) {
        AddCategoryBottomSheet fragment = new AddCategoryBottomSheet();
        fragment.editingItem = item;
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            parentCategory = getArguments().getString("parent_category");
        }
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

        setupInputValidation();

        // Always hide amount and recurring as per requirements: only Name and Icon
        getBinding().labelAmount.setVisibility(View.GONE);
        getBinding().layoutAmount.setVisibility(View.GONE);
        getBinding().layoutRecurring.setVisibility(View.GONE);

        if (parentCategory != null) {
            getBinding().tvTitle.setText(R.string.title_add_sub_category);
            getBinding().tvSubtitle.setText(getString(R.string.desc_adding_sub_category, parentCategory));
            
            // Default sub-category icon to parent's icon
            com.upreyvan.carti.models.Category parent = com.upreyvan.carti.managers.CategoryManager.getInstance(requireContext()).getCategoryByName(parentCategory);
            if (parent != null && parent.getIconRes() != 0) {
                selectedIcon = parent.getIconRes();
                getBinding().ivCategoryIcon.setImageResource(selectedIcon);
                getBinding().ivCategoryIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.carti_primary_green)));
            }
        }

        if (editingItem != null) {
            getBinding().tvTitle.setText(R.string.btn_edit);
            getBinding().etCategoryName.setText(editingItem.getCategoryName());
            selectedIcon = editingItem.getIconRes();
            if (selectedIcon != 0) {
                getBinding().ivCategoryIcon.setImageResource(selectedIcon);
                getBinding().ivCategoryIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.carti_primary_green)));
            }
            parentCategory = editingItem.getParentCategory();
        }
        
        validateForm();
    }

    private void setupInputValidation() {
        TextWatcher validationWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { 
                validateForm(); 
                if (editingItem == null && selectedIcon == R.drawable.ic_chart) {
                    suggestIcon(s.toString());
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        };

        getBinding().etCategoryName.addTextChangedListener(validationWatcher);
        getBinding().etAmount.addTextChangedListener(new com.upreyvan.carti.utils.AmountTextWatcher(getBinding().etAmount));
        getBinding().etAmount.addTextChangedListener(validationWatcher);
    }

    private void suggestIcon(String name) {
        if (name == null || name.isEmpty()) return;
        String clean = name.toLowerCase().trim();
        
        int suggested = R.drawable.ic_chart;
        
        // Comprehensive Keyword Matching using Android built-in icons
        if (clean.contains("food") || clean.contains("kain") || clean.contains("meal") || clean.contains("eat") || clean.contains("restau")) {
            suggested = android.R.drawable.ic_menu_gallery; 
        } else if (clean.contains("water") || clean.contains("tubig") || clean.contains("drink")) {
            suggested = android.R.drawable.ic_menu_compass;
        } else if (clean.contains("elect") || clean.contains("kuryente") || clean.contains("meralco") || clean.contains("light") || clean.contains("power")) {
            suggested = R.drawable.ic_calendar;
        } else if (clean.contains("fare") || clean.contains("trans") || clean.contains("gas") || clean.contains("fuel") || clean.contains("car") || clean.contains("drive") || clean.contains("taxi")) {
            suggested = android.R.drawable.ic_dialog_map;
        } else if (clean.contains("load") || clean.contains("data") || clean.contains("internet") || clean.contains("wifi") || clean.contains("phone") || clean.contains("call")) {
            suggested = android.R.drawable.ic_menu_send;
        } else if (clean.contains("bill") || clean.contains("rent") || clean.contains("calendar") || clean.contains("due") || clean.contains("tax")) {
            suggested = R.drawable.ic_calendar;
        } else if (clean.contains("store") || clean.contains("grocery") || clean.contains("shop") || clean.contains("mall") || clean.contains("buy")) {
            suggested = android.R.drawable.ic_input_add;
        } else if (clean.contains("debt") || clean.contains("utang") || clean.contains("loan") || clean.contains("credit") || clean.contains("pay")) {
            suggested = android.R.drawable.ic_lock_lock;
        } else if (clean.contains("health") || clean.contains("med") || clean.contains("hospital") || clean.contains("clinic") || clean.contains("doctor")) {
            suggested = android.R.drawable.ic_menu_compass;
        } else if (clean.contains("school") || clean.contains("educ") || clean.contains("study") || clean.contains("book") || clean.contains("class")) {
            suggested = android.R.drawable.ic_menu_edit;
        } else if (clean.contains("repair") || clean.contains("fix") || clean.contains("home") || clean.contains("house") || clean.contains("maintain")) {
            suggested = android.R.drawable.ic_menu_manage;
        } else if (clean.contains("pet") || clean.contains("dog") || clean.contains("cat") || clean.contains("animal")) {
            suggested = android.R.drawable.ic_menu_view;
        } else if (clean.contains("save") || clean.contains("bank") || clean.contains("money") || clean.contains("cash") || clean.contains("wallet")) {
            suggested = android.R.drawable.ic_menu_save;
        } else if (clean.contains("entertain") || clean.contains("movie") || clean.contains("music") || clean.contains("game") || clean.contains("fun")) {
            suggested = android.R.drawable.ic_menu_slideshow;
        } else if (clean.contains("personal") || clean.contains("care") || clean.contains("self") || clean.contains("beauty")) {
            suggested = android.R.drawable.ic_menu_myplaces;
        } else if (clean.contains("work") || clean.contains("job") || clean.contains("office") || clean.contains("biz")) {
            suggested = android.R.drawable.ic_menu_agenda;
        }

        if (suggested != R.drawable.ic_chart) {
            selectedIcon = suggested;
            getBinding().ivCategoryIcon.setImageResource(selectedIcon);
            getBinding().ivCategoryIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.carti_primary_green)));
        }
    }

    private void validateForm() {
        String name = getBinding().etCategoryName.getText().toString().trim();
        boolean isNameValid = !name.isEmpty();
        
        getBinding().btnSave.setEnabled(isNameValid);
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
        android.text.Editable nameText = getBinding().etCategoryName.getText();
        
        if (nameText == null) return;

        String name = nameText.toString().trim();
        boolean isRecurring = getBinding().switchRecurring.isChecked();
        
        if (name.isEmpty()) {
            UiHelper.showSnackbar(getBinding().getRoot(), R.string.msg_fill_all_fields, UiHelper.Status.ERROR);
            return;
        }

        com.upreyvan.carti.managers.CategoryManager manager = com.upreyvan.carti.managers.CategoryManager.getInstance(requireContext());
        List<com.upreyvan.carti.models.Category> existing = manager.getCategories();
        
        for (com.upreyvan.carti.models.Category item : existing) {
            if (item.getName().equalsIgnoreCase(name) &&
                    Objects.equals(item.getParentCategory(), parentCategory)) {
                if (editingItem == null || !editingItem.getCategoryName().equalsIgnoreCase(name)) {
                    UiHelper.showSnackbar(getBinding().getRoot(), "This category already exists", UiHelper.Status.WARNING);
                    return;
                }
            }
        }
        
        com.upreyvan.carti.models.Category cat = new com.upreyvan.carti.models.Category(
                editingItem != null ? editingItem.getCategoryName() : UUID.randomUUID().toString(),
                name,
                selectedIcon,
                R.color.carti_primary_green,
                R.color.mint_green_alpha,
                false,
                parentCategory
        );

        manager.updateCategory(editingItem != null ? editingItem.getCategoryName() : name, cat);

        // If user also set an amount, we save it as a budget (ALLOCATION) in the repo
        String amountStr = getBinding().etAmount.getText() != null ? getBinding().etAmount.getText().toString() : "0";
        if (!amountStr.isEmpty()) {
            try { 
                double amount = com.upreyvan.carti.utils.StringHelper.parseDouble(amountStr);
                if (amount > 0) {
                    TransactionRepository.getInstance(requireContext()).updateOrAddCategory(
                            editingItem != null ? editingItem.getCategoryName() : null,
                            name,
                            selectedIcon,
                            R.color.carti_primary_green,
                            R.color.mint_green_alpha,
                            amount,
                            parentCategory,
                            isRecurring
                    );
                }
            } catch (Exception ignored) {}
        }

        PlanViewModel viewModel = new ViewModelProvider(requireActivity()).get(PlanViewModel.class);
        viewModel.loadData();
        
        if (listener != null) listener.onCategoryAdded(name, 0, isRecurring);
        dismiss();
    }
}
