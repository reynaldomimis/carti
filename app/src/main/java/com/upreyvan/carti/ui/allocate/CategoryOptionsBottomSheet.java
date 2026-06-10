package com.upreyvan.carti.ui.allocate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseBottomSheetFragment;
import com.upreyvan.carti.databinding.BottomSheetCategoryOptionsBinding;
import com.upreyvan.carti.models.BudgetCategoryItem;

public class CategoryOptionsBottomSheet extends BaseBottomSheetFragment<BottomSheetCategoryOptionsBinding> {
    private BudgetCategoryItem category;
    private OnCategoryOptionListener listener;

    public interface OnCategoryOptionListener {
        void onShowSubCategories(BudgetCategoryItem category);
        void onEditCategory(BudgetCategoryItem category);
        void onDeleteCategory(BudgetCategoryItem category);
    }

    public static CategoryOptionsBottomSheet newInstance(BudgetCategoryItem category) {
        CategoryOptionsBottomSheet fragment = new CategoryOptionsBottomSheet();
        fragment.category = category;
        return fragment;
    }

    public void setListener(OnCategoryOptionListener listener) {
        this.listener = listener;
    }

    @Override
    protected BottomSheetCategoryOptionsBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return BottomSheetCategoryOptionsBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (category == null) {
            dismiss();
            return;
        }

        getBinding().tvCategoryName.setText(category.getCategoryName());

        if (category.getParentCategory() != null) {
            getBinding().btnAddSub.setVisibility(View.GONE);
        } else {
            getBinding().btnAddSub.setText(R.string.label_show_sub_categories);
            getBinding().btnAddSub.setIconResource(R.drawable.ic_chart);
            getBinding().btnAddSub.setOnClickListener(v -> {
                if (listener != null) listener.onShowSubCategories(category);
                dismiss();
            });
        }

        getBinding().btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditCategory(category);
            dismiss();
        });

        getBinding().btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteCategory(category);
            dismiss();
        });
    }
}
