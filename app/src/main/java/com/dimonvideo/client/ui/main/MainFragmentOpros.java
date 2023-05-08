package com.dimonvideo.client.ui.main;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.databinding.OprosListBinding;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.ProgressHelper;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.greenrobot.eventbus.EventBus;

public class MainFragmentOpros extends BottomSheetDialogFragment {
    private OprosListBinding binding;
    private WebView webView;


    public MainFragmentOpros(){
        // Required empty public constructor
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = OprosListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        TextView title = binding.title;
        if (this.getArguments() != null) {
            String vote_title = getArguments().getString(Config.TAG_TITLE);
            title.setText(vote_title);
        }

        final String login_name = AppController.getInstance().userName("dvclient");

        final String url = Config.VOTE_URL_VIEW+login_name;
        WebView webView = binding.webview;
        webView.setWebViewClient(new WebViewClient());

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        webView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setUseWideViewPort(false);
        webView.getSettings().setLoadWithOverviewMode(true);

        ProgressHelper.showDialog(requireContext(), requireActivity().getString(R.string.please_wait));

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (ProgressHelper.isDialogVisible()) {
                    ProgressHelper.dismissDialog();
                }
            }
        });
        webView.loadUrl(url);


        ImageView imageDismiss = binding.dismiss;
        imageDismiss.setOnClickListener(v -> {
            dismiss();
        });

    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        if (ProgressHelper.isDialogVisible()) {
            ProgressHelper.dismissDialog();
        }
        binding = null;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }


}