package com.upreyvan.carti.ui.common;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.databinding.DialogColorPickerBinding;
import com.upreyvan.carti.databinding.ItemColorChoiceBinding;
import com.upreyvan.carti.models.ColorChoice;

import java.util.ArrayList;
import java.util.List;

public class ColorPickerDialog extends BottomSheetDialogFragment {

    public interface OnColorSelectedListener {
        void onColorSelected(ColorChoice color);
    }

    private DialogColorPickerBinding binding;
    private OnColorSelectedListener listener;

    public void setListener(OnColorSelectedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setNavigationBarColor(requireContext().getColor(R.color.white));
            View decorView = dialog.getWindow().getDecorView();
            int flags = decorView.getSystemUiVisibility();
            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            decorView.setSystemUiVisibility(flags);
            dialog.getWindow().setDimAmount(0.4f);
        }
        return dialog;
    }

    @Override
    public int getTheme() {
        return R.style.CustomBottomSheetDialogTheme;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogColorPickerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerView();
        binding.btnClose.setOnClickListener(v -> dismiss());
    }

    private void setupRecyclerView() {
        List<ColorChoice> colors = new ArrayList<>();
        colors.add(new ColorChoice(R.color.icon_food, R.color.log_food));
        colors.add(new ColorChoice(R.color.icon_fare, R.color.log_fare));
        colors.add(new ColorChoice(R.color.icon_load, R.color.log_load));
        colors.add(new ColorChoice(R.color.icon_store, R.color.log_store));
        colors.add(new ColorChoice(R.color.icon_electricity, R.color.log_electricity));
        colors.add(new ColorChoice(R.color.icon_water, R.color.log_water));
        colors.add(new ColorChoice(R.color.icon_debt, R.color.log_debt));
        colors.add(new ColorChoice(R.color.icon_others, R.color.log_others));
        colors.add(new ColorChoice(R.color.status_red, R.color.status_red_tonal));
        colors.add(new ColorChoice(R.color.carti_primary_green, R.color.mint_green_alpha));
        colors.add(new ColorChoice(R.color.purple, R.color.tonal_button_bg));

        GenericAdapter<ColorChoice, ItemColorChoiceBinding> adapter = new GenericAdapter<>(
                ColorChoice.DIFF_CALLBACK,
                (inflater, parent) -> ItemColorChoiceBinding.inflate(inflater, parent, false),
                (binding, color) -> {
                    binding.viewColor.setBackgroundColor(ContextCompat.getColor(requireContext(), color.getColorRes()));
                    binding.getRoot().setCardBackgroundColor(ContextCompat.getColor(requireContext(), color.getBgColorRes()));
                    binding.getRoot().setStrokeColor(ContextCompat.getColor(requireContext(), color.getColorRes()));
                }
        );

        adapter.setOnItemClickListener(color -> {
            if (listener != null) {
                listener.onColorSelected(color);
            }
            dismiss();
        });

        binding.rvColors.setLayoutManager(new GridLayoutManager(requireContext(), 4));
        binding.rvColors.setAdapter(adapter);
        adapter.submitList(colors);
    }
}
