package com.upreyvan.carti.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.upreyvan.carti.R;
import com.upreyvan.carti.ui.home.CustomizeCategoryAdapter;
import com.upreyvan.carti.base.BaseActivity;
import com.upreyvan.carti.databinding.ActivityCustomizeQuickLogBinding;
import com.upreyvan.carti.data.local.CategoryManager;
import com.upreyvan.carti.util.Utils;

public class CustomizeQuickLogActivity extends BaseActivity<ActivityCustomizeQuickLogBinding> {

    private CustomizeCategoryAdapter adapter;

    @Override
    protected ActivityCustomizeQuickLogBinding inflateBinding(LayoutInflater inflater) {
        return ActivityCustomizeQuickLogBinding.inflate(inflater);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupDynamicPadding();
        setupToolbar();
        setupRecyclerView();
        setupListeners();
    }

    private void setupDynamicPadding() {
        Utils.applySystemBarInsets(
                getBinding().layoutToolbar.getRoot(),
                getBinding().btnSaveOrder,
                1f,
                0
        );
    }

    private void setupToolbar() {
        getBinding().layoutToolbar.tvToolbarTitle.setText("Customize Quick Log");
        getBinding().layoutToolbar.backButtonContainer.setVisibility(View.VISIBLE);
        getBinding().layoutToolbar.btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new CustomizeCategoryAdapter(CategoryManager.getInstance(this).getCategories());
        getBinding().rvCustomize.setLayoutManager(new LinearLayoutManager(this));
        getBinding().rvCustomize.setAdapter(adapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                adapter.moveItem(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // No swipe to delete for now
            }
        });
        itemTouchHelper.attachToRecyclerView(getBinding().rvCustomize);
    }

    private void setupListeners() {
        getBinding().btnSaveOrder.setOnClickListener(v -> {
            CategoryManager.getInstance(this).updateCategories(adapter.getCategories());
            Toast.makeText(this, "Order Saved!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
