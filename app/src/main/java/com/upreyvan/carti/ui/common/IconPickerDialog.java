package com.upreyvan.carti.ui.common;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.managers.IconManager;
import com.upreyvan.carti.databinding.DialogIconPickerBinding;
import com.upreyvan.carti.databinding.ItemColorChoiceBinding;
import com.upreyvan.carti.databinding.ItemIconChoiceBinding;
import com.upreyvan.carti.models.ColorChoice;
import com.upreyvan.carti.models.IconChoice;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class IconPickerDialog extends BottomSheetDialogFragment {

    public interface OnIconSelectedListener {
        void onIconSelected(IconChoice icon, ColorChoice color);
        void onUploadCustom();
        default boolean isUploadItem(IconChoice icon) {
            return "UPLOAD_CUSTOM_ACTION".equals(icon.getName());
        }
    }

    private DialogIconPickerBinding binding;
    private OnIconSelectedListener listener;
    private List<IconChoice> allIcons;
    private GenericAdapter<IconChoice, ItemIconChoiceBinding> adapter;
    private GenericAdapter<ColorChoice, ItemColorChoiceBinding> colorAdapter;
    private ColorChoice selectedColor;
    private List<ColorChoice> availableColors;
    private int initialColorRes = 0;

    public void setListener(OnIconSelectedListener listener) {
        this.listener = listener;
    }

    public void setInitialColor(int colorRes) {
        this.initialColorRes = colorRes;
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
        binding = DialogIconPickerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupColors();
        setupRecyclerView();
        setupSearch();
        setupListeners();
        binding.btnClose.setOnClickListener(v -> dismiss());
    }

    private void setupColors() {
        availableColors = new ArrayList<>();
        availableColors.add(new ColorChoice(R.color.carti_primary_green, R.color.mint_green_alpha));
        availableColors.add(new ColorChoice(R.color.icon_food, R.color.log_food));
        availableColors.add(new ColorChoice(R.color.icon_fare, R.color.log_fare));
        availableColors.add(new ColorChoice(R.color.icon_load, R.color.log_load));
        availableColors.add(new ColorChoice(R.color.icon_store, R.color.log_store));
        availableColors.add(new ColorChoice(R.color.icon_electricity, R.color.log_electricity));
        availableColors.add(new ColorChoice(R.color.icon_water, R.color.log_water));
        availableColors.add(new ColorChoice(R.color.icon_debt, R.color.log_debt));
        availableColors.add(new ColorChoice(R.color.status_red, R.color.status_red_tonal));
        availableColors.add(new ColorChoice(R.color.purple, R.color.tonal_button_bg));
        availableColors.add(new ColorChoice(R.color.carti_primary_blue, R.color.white_10));
        availableColors.add(new ColorChoice(R.color.dash_orange, R.color.dash_orange_alpha));
        availableColors.add(new ColorChoice(R.color.gray, R.color.surface_variant));
        availableColors.add(new ColorChoice(R.color.black, R.color.white_10));
        availableColors.add(new ColorChoice(R.color.gradient_start, R.color.white_10));
        availableColors.add(new ColorChoice(R.color.gradient_end, R.color.white_10));
        availableColors.add(new ColorChoice(R.color.green_darker, R.color.mint_green_alpha));
        availableColors.add(new ColorChoice(R.color.green_budget_status, R.color.mint_green_alpha));
        availableColors.add(new ColorChoice(R.color.chat_progress_primary, R.color.chat_progress_track));
        availableColors.add(new ColorChoice(R.color.dash_red, R.color.dash_red_alpha));

        selectedColor = availableColors.get(0);
        if (initialColorRes != 0) {
            for (ColorChoice choice : availableColors) {
                if (choice.getColorRes() == initialColorRes) {
                    selectedColor = choice;
                    break;
                }
            }
        }

        colorAdapter = new GenericAdapter<>(
                ColorChoice.DIFF_CALLBACK,
                (inflater, parent) -> ItemColorChoiceBinding.inflate(inflater, parent, false),
                (binding, color) -> {
                    binding.viewColor.setBackgroundColor(requireContext().getColor(color.getColorRes()));
                    binding.getRoot().setCardBackgroundColor(requireContext().getColor(color.getBgColorRes()));
                    if (color.equals(selectedColor)) {
                        binding.getRoot().setStrokeColor(requireContext().getColor(R.color.white));
                        binding.getRoot().setStrokeWidth(com.upreyvan.carti.utils.Utils.dpToPx(requireContext(), 2));
                    } else {
                        binding.getRoot().setStrokeWidth(0);
                    }
                }
        );

        colorAdapter.setOnItemClickListener(color -> {
            selectedColor = color;
            colorAdapter.notifyDataSetChanged();
            adapter.notifyDataSetChanged();
        });

        binding.rvColors.setAdapter(colorAdapter);
        colorAdapter.submitList(availableColors);
    }

    private void setupRecyclerView() {
        allIcons = new ArrayList<>(IconManager.getSystemIcons());
        allIcons.add(new IconChoice("UPLOAD_CUSTOM_ACTION", R.drawable.ic_upload));
        
        adapter = new GenericAdapter<>(
                IconChoice.DIFF_CALLBACK,
                (inflater, parent) -> ItemIconChoiceBinding.inflate(inflater, parent, false),
                (binding, icon) -> {
                    binding.ivIcon.setImageResource(icon.getIconRes());
                    if ("UPLOAD_CUSTOM_ACTION".equals(icon.getName())) {
                        binding.ivIcon.setColorFilter(requireContext().getColor(R.color.carti_primary_green));
                        binding.getRoot().setStrokeColor(requireContext().getColor(R.color.carti_primary_green));
                        binding.getRoot().setCardBackgroundColor(requireContext().getColor(R.color.mint_green_alpha));
                    } else {
                        binding.ivIcon.setColorFilter(requireContext().getColor(selectedColor.getColorRes()));
                        binding.getRoot().setCardBackgroundColor(requireContext().getColor(selectedColor.getBgColorRes()));
                        binding.getRoot().setStrokeColor(requireContext().getColor(R.color.card_border));
                    }
                }
        );
        adapter.setOnItemClickListener(icon -> {
            if (listener != null) {
                if ("UPLOAD_CUSTOM_ACTION".equals(icon.getName())) {
                    listener.onUploadCustom();
                } else {
                    listener.onIconSelected(icon, selectedColor);
                }
            }
            dismiss();
        });
        adapter.submitList(new ArrayList<>(allIcons));
        binding.rvIcons.setLayoutManager(new GridLayoutManager(requireContext(), 5));
        binding.rvIcons.setAdapter(adapter);
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterIcons(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterIcons(String query) {
        if (query.isEmpty()) {
            adapter.submitList(new ArrayList<>(allIcons));
            return;
        }

        List<IconChoice> filtered = allIcons.stream()
                .filter(icon -> "UPLOAD_CUSTOM_ACTION".equals(icon.getName()) || 
                                icon.getName().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
        adapter.submitList(filtered);
    }

    private void setupListeners() {
        // Button removed from UI, action moved to list item
    }
}
