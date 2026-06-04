package com.upreyvan.carti.ui.goals;

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

import com.google.android.material.datepicker.MaterialDatePicker;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseBottomSheetFragment;
import com.upreyvan.carti.data.remote.AppwriteManager;
import com.upreyvan.carti.data.repository.TransactionRepository;
import com.upreyvan.carti.databinding.LayoutBottomSheetAddGoalBinding;
import com.upreyvan.carti.model.IconChoice;
import com.upreyvan.carti.model.Transaction;
import com.upreyvan.carti.ui.common.IconPickerDialog;
import com.upreyvan.carti.util.Constants;
import com.upreyvan.carti.util.ToastHelper;
import com.upreyvan.carti.util.Validator;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

public class AddGoalBottomSheetFragment extends BaseBottomSheetFragment<LayoutBottomSheetAddGoalBinding> {

    private int selectedIcon = R.drawable.ic_chart;
    private Uri selectedImageUri = null;

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
        setupDatePicker();
        setupListeners();
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
        if (Validator.isEmpty(getBinding().etGoalName) || Validator.isEmpty(getBinding().etTargetAmount)) {
            showToast(R.string.msg_fill_all_fields, ToastHelper.Status.WARNING);
            return;
        }

        String name = getBinding().etGoalName.getText().toString().trim();
        double targetAmount = Double.parseDouble(getBinding().etTargetAmount.getText().toString().trim());
        String savedAmountStr = getBinding().etSavedAmount.getText().toString().trim();
        double savedAmount = savedAmountStr.isEmpty() ? 0 : Double.parseDouble(savedAmountStr);
        String date = getBinding().etTargetDate.getText().toString().trim();

        showLoading(true, "Saving goal...");

        if (selectedImageUri != null) {
            uploadImageAndSave(name, targetAmount, savedAmount, date);
        } else {
            saveGoal(name, targetAmount, savedAmount, date, null);
        }
    }

    private void uploadImageAndSave(String name, double targetAmount, double savedAmount, String date) {
        File file = new File(selectedImageUri.getPath());
        io.appwrite.models.InputFile inputFile = io.appwrite.models.InputFile.Companion.fromFile(file);
        
        AppwriteManager.getInstance(requireContext()).uploadFile(
                Constants.Appwrite.BUCKET_ICONS,
                io.appwrite.ID.Companion.unique(0),
                inputFile,
                null,
                new AppwriteManager.AppwriteCallback<>() {
                    @Override
                    public void onSuccess(io.appwrite.models.File result) {
                        String fileUrl = String.format("%s/storage/buckets/%s/files/%s/view?project=%s",
                                com.upreyvan.carti.BuildConfig.APPWRITE_ENDPOINT,
                                Constants.Appwrite.BUCKET_ICONS,
                                result.getId(),
                                com.upreyvan.carti.BuildConfig.APPWRITE_PROJECT_ID);
                        saveGoal(name, targetAmount, savedAmount, date, fileUrl);
                    }

                    @Override
                    public void onError(Throwable error) {
                        showLoading(false);
                        showToast("Failed to upload image: " + error.getMessage(), ToastHelper.Status.ERROR);
                    }
                }
        );
    }

    private void saveGoal(String name, double targetAmount, double savedAmount, String date, String iconUrl) {
        Transaction t = new Transaction();
        t.setTitle(name);
        t.setTargetAmount(targetAmount);
        t.setAmount(savedAmount);
        t.setType("GOAL");
        t.setTargetDate(date);
        t.setIconRes(selectedIcon);
        t.setIconUrl(iconUrl);

        TransactionRepository.getInstance(requireContext()).addTransaction(t, new AppwriteManager.AppwriteCallback<>() {
            @Override
            public void onSuccess(java.util.Map<String, Object> result) {
                if (isAdded()) {
                    showLoading(false);
                    showToast(R.string.msg_goal_saved_success, ToastHelper.Status.SUCCESS);
                    dismiss();
                }
            }

            @Override
            public void onError(Throwable error) {
                if (isAdded()) {
                    showLoading(false);
                    showToast(getString(R.string.err_generic, error.getMessage()), ToastHelper.Status.ERROR);
                }
            }
        });
    }
}
