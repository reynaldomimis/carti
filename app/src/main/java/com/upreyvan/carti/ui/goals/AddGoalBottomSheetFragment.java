package com.upreyvan.carti.ui.goals;

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

import com.google.android.material.datepicker.MaterialDatePicker;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseBottomSheetFragment;
import com.upreyvan.carti.databinding.LayoutBottomSheetAddGoalBinding;
import com.upreyvan.carti.models.IconChoice;
import com.upreyvan.carti.models.TransactionWithUser;
import com.upreyvan.carti.ui.common.IconPickerDialog;
import com.upreyvan.carti.utils.UiHelper;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class AddGoalBottomSheetFragment extends BaseBottomSheetFragment<LayoutBottomSheetAddGoalBinding> {

    private int selectedIcon = R.drawable.ic_chart;
    private Uri selectedImageUri = null;
    private GoalViewModel viewModel;

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
                        getBinding().ivGoalIcon.setImageURI(selectedImageUri);
                        getBinding().ivGoalIcon.setImageTintList(null);
                    }
                }
            }
    );

    public static AddGoalBottomSheetFragment newInstance() {
        return new AddGoalBottomSheetFragment();
    }

    @Override
    protected LayoutBottomSheetAddGoalBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return LayoutBottomSheetAddGoalBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(GoalViewModel.class);
        setupDatePicker();
        setupListeners();
        setupInputValidation();
        observeViewModel();
        validateForm();
    }

    private void setupInputValidation() {
        TextWatcher validationWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { validateForm(); }
            @Override public void afterTextChanged(Editable s) {}
        };

        getBinding().etGoalName.addTextChangedListener(validationWatcher);
        getBinding().etTargetAmount.addTextChangedListener(new com.upreyvan.carti.utils.AmountTextWatcher(getBinding().etTargetAmount));
        getBinding().etTargetAmount.addTextChangedListener(validationWatcher);
        getBinding().etSavedAmount.addTextChangedListener(new com.upreyvan.carti.utils.AmountTextWatcher(getBinding().etSavedAmount));
        getBinding().etTargetDate.addTextChangedListener(validationWatcher);
    }

    private void validateForm() {
        String name = getBinding().etGoalName.getText().toString().trim();
        String targetStr = getBinding().etTargetAmount.getText().toString().trim();
        String date = getBinding().etTargetDate.getText().toString().trim();

        double target = com.upreyvan.carti.utils.StringHelper.parseDouble(targetStr);
        
        boolean isValid = !name.isEmpty() && target > 0 && !date.isEmpty();
        boolean isLoading = viewModel.getIsLoading().getValue() != null && viewModel.getIsLoading().getValue();

        getBinding().btnSave.setEnabled(isValid && !isLoading);
    }

    private void observeViewModel() {
        viewModel.getSaveSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success) {
                showToast(R.string.msg_goal_saved_success, UiHelper.Status.SUCCESS);
                dismiss();
            }
        });
        
        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) showToast(err, UiHelper.Status.ERROR);
        });
        
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            showLoading(loading, "Saving goal...");
            validateForm();
        });
    }

    private void setupDatePicker() {
        View.OnClickListener show = v -> showDatePicker();
        getBinding().etTargetDate.setOnClickListener(show);
        getBinding().layoutTargetDate.setEndIconOnClickListener(show);
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> dp = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.label_target_date)
                .setTheme(R.style.CartiDatePicker)
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();
        dp.addOnPositiveButtonClickListener(s -> {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(s);
            getBinding().etTargetDate.setText(new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(c.getTime()));
        });
        dp.show(getChildFragmentManager(), "DATE_PICKER");
    }

    private void setupListeners() {
        getBinding().btnClose.setOnClickListener(v -> dismiss());
        getBinding().btnCancel.setOnClickListener(v -> dismiss());
        getBinding().btnSave.setOnClickListener(v -> onSaveClicked());
        getBinding().cardIconContainer.setOnClickListener(v -> showIconPicker());
    }

    private void showIconPicker() {
        IconPickerDialog dialog = new IconPickerDialog();
        dialog.setListener(new IconPickerDialog.OnIconSelectedListener() {
            @Override
            public void onIconSelected(IconChoice icon) {
                selectedIcon = icon.getIconRes();
                selectedImageUri = null;
                getBinding().ivGoalIcon.setImageResource(selectedIcon);
                getBinding().ivGoalIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.carti_primary_green)));
            }

            @Override
            public void onUploadCustom() {
                pickImageLauncher.launch("image/*");
            }
        });
        dialog.show(getChildFragmentManager(), "ICON_PICKER");
    }

    private void startCrop(@NonNull Uri uri) {
        String destinationFileName = "cropped_goal_" + UUID.randomUUID() + ".jpg";
        UCrop.Options options = new UCrop.Options();
        options.setCompressionFormat(android.graphics.Bitmap.CompressFormat.JPEG);
        options.setCompressionQuality(90);
        options.setCircleDimmedLayer(true);
        options.setShowCropGrid(false);
        options.setHideBottomControls(false);
        options.setFreeStyleCropEnabled(true);

        UCrop uCrop = UCrop.of(uri, Uri.fromFile(new File(requireContext().getCacheDir(), destinationFileName)))
                .withAspectRatio(1, 1)
                .withMaxResultSize(400, 400)
                .withOptions(options);

        cropImageLauncher.launch(uCrop.getIntent(requireContext()));
    }

    private void onSaveClicked() {
        if (!checkNetwork()) return;

        String name = getBinding().etGoalName.getText().toString().trim();

        List<TransactionWithUser> goalsData = viewModel.getGoals().getValue();
        if (goalsData != null) {
            for (TransactionWithUser item : goalsData) {
                if (item.getTransaction().getTitle().equalsIgnoreCase(name)) {
                    UiHelper.showSnackbar(getBinding().getRoot(), "This goal already exists", UiHelper.Status.WARNING);
                    return;
                }
            }
        }

        double targetAmount = com.upreyvan.carti.utils.StringHelper.parseDouble(getBinding().etTargetAmount.getText().toString().trim());
        String savedAmountStr = getBinding().etSavedAmount.getText().toString().trim();
        double savedAmount = savedAmountStr.isEmpty() ? 0 : com.upreyvan.carti.utils.StringHelper.parseDouble(savedAmountStr);
        String date = getBinding().etTargetDate.getText().toString().trim();

        viewModel.saveGoal(name, targetAmount, savedAmount, date, selectedIcon, selectedImageUri);
    }
}
