package com.upreyvan.carti.ui.profile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.upreyvan.carti.R;
import com.upreyvan.carti.base.BaseFragment;
import com.upreyvan.carti.databinding.FragmentHelpBinding;

import androidx.recyclerview.widget.LinearLayoutManager;
import com.upreyvan.carti.base.GenericAdapter;
import com.upreyvan.carti.databinding.ItemFaqBinding;
import com.upreyvan.carti.model.FaqItem;
import java.util.ArrayList;
import java.util.List;

public class HelpFragment extends BaseFragment<FragmentHelpBinding> {

    @Override
    protected FragmentHelpBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentHelpBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupToolbar(getBinding().layoutToolbar, R.string.help_title);
        setupFaqList();
        setupListeners();
    }


    private void setupFaqList() {
        List<FaqItem> faqItems = new ArrayList<>();
        faqItems.add(new FaqItem(getString(R.string.faq_q1), getString(R.string.faq_a1)));
        faqItems.add(new FaqItem(getString(R.string.faq_q2), getString(R.string.faq_a2)));
        faqItems.add(new FaqItem(getString(R.string.faq_q3), getString(R.string.faq_a3)));
        faqItems.add(new FaqItem(getString(R.string.faq_q4), getString(R.string.faq_a4)));
        faqItems.add(new FaqItem(getString(R.string.faq_q5), getString(R.string.faq_a5)));
        faqItems.add(new FaqItem(getString(R.string.faq_q6), getString(R.string.faq_a6)));
        faqItems.add(new FaqItem(getString(R.string.faq_q7), getString(R.string.faq_a7)));
        faqItems.add(new FaqItem(getString(R.string.faq_q8), getString(R.string.faq_a8)));

        GenericAdapter<FaqItem, ItemFaqBinding> adapter = new GenericAdapter<>(
                FaqItem.DIFF_CALLBACK,
                (inflater, parent) -> ItemFaqBinding.inflate(inflater, parent, false),
                (binding, item) -> {
                    binding.tvQuestion.setText(item.getQuestion());
                    binding.tvAnswer.setText(item.getAnswer());
                }
        );
        adapter.submitList(faqItems);
        getBinding().rvFaq.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvFaq.setAdapter(adapter);
    }

    private void setupListeners() {
        getBinding().btnEmailSupport.setOnClickListener(v -> sendEmail(getString(R.string.support_email_subject)));
        getBinding().btnReportBug.setOnClickListener(v -> sendEmail(getString(R.string.bug_report_subject)));
    }

    private void sendEmail(String subject) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.support_email)});
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        
        try {
            startActivity(Intent.createChooser(intent, "Send email..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(requireContext(), "No email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }
}
