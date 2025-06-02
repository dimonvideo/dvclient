package com.dimonvideo.client.ui.main;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.databinding.OprosListBinding;
import com.dimonvideo.client.ui.forum.ForumFragmentPosts;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.ProgressHelper;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class MainFragmentOpros extends BottomSheetDialogFragment {
    private OprosListBinding binding;

    public MainFragmentOpros() {
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
        final String url = Config.VOTE_URL_VIEW + login_name;
        WebView webView = binding.webview;

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        webView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setUseWideViewPort(false);
        webView.getSettings().setLoadWithOverviewMode(true);

        ProgressHelper.showDialog(requireContext(), requireActivity().getString(R.string.please_wait));

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // Получаем URL из объекта WebResourceRequest
                view.loadUrl(request.getUrl().toString());
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

        Button btn_new = binding.predlog;
        Button btn_result = binding.results;

        btn_new.setOnClickListener(v -> {
            ForumFragmentPosts fragment = new ForumFragmentPosts();
            Bundle bundle = new Bundle();
            bundle.putString(Config.TAG_TITLE, "Предложи опрос!");
            bundle.putString(Config.TAG_ID, "1728149578");
            bundle.putString(Config.TAG_RAZDEL, "8");
            fragment.setArguments(bundle);
            fragment.show(((AppCompatActivity) requireContext()).getSupportFragmentManager(), "ForumFragmentPosts");
            dismiss();
        });

        btn_result.setOnClickListener(v -> {
            ForumFragmentPosts fragment = new ForumFragmentPosts();
            Bundle bundle = new Bundle();
            bundle.putString(Config.TAG_TITLE, "Результаты опросов");
            bundle.putString(Config.TAG_ID, "1728131216");
            bundle.putString(Config.TAG_RAZDEL, "8");
            fragment.setArguments(bundle);
            fragment.show(((AppCompatActivity) requireContext()).getSupportFragmentManager(), "ForumFragmentPosts");
            dismiss();
        });

        ImageView imageDismiss = binding.dismiss;
        imageDismiss.setOnClickListener(v -> dismiss());
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