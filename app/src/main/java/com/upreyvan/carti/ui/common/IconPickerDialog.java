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
import com.upreyvan.carti.data.local.IconManager;
import com.upreyvan.carti.databinding.DialogIconPickerBinding;
import com.upreyvan.carti.databinding.ItemIconChoiceBinding;
import com.upreyvan.carti.model.IconChoice;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class IconPickerDialog extends BottomSheetDialogFragment {

    public interface OnIconSelectedListener {
        void onIconSelected(IconChoice icon);
        void onUploadCustom();
        default boolean isUploadItem(IconChoice icon) {
            return "UPLOAD_CUSTOM_ACTION".equals(icon.getName());
        }
    }

    private DialogIconPickerBinding binding;
    private OnIconSelectedListener listener;
    private List<IconChoice> allIcons;
    private GenericAdapter<IconChoice, ItemIconChoiceBinding> adapter;

    public void setListener(OnIconSelectedListener listener) {
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
        binding = DialogIconPickerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerView();
        setupSearch();
        setupListeners();
        binding.btnClose.setOnClickListener(v -> dismiss());
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
                    } else {
                        binding.ivIcon.setColorFilter(requireContext().getColor(R.color.text_secondary));
                        binding.getRoot().setStrokeColor(requireContext().getColor(R.color.card_border));
                    }
                }
        );
        adapter.setOnItemClickListener(icon -> {
            if (listener != null) {
                if ("UPLOAD_CUSTOM_ACTION".equals(icon.getName())) {
                    listener.onUploadCustom();
                } else {
                    listener.onIconSelected(icon);
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
