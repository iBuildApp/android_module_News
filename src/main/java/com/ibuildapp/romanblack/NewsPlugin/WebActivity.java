package com.ibuildapp.romanblack.NewsPlugin;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.MailTo;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.appbuilder.sdk.android.AppBuilderModuleMainAppCompat;
import com.appbuilder.sdk.android.Widget;
import com.ibuildapp.romanblack.NewsPlugin.utils.NewsConstants;
import com.ibuildapp.romanblack.NewsPlugin.utils.Statics;


public class WebActivity extends AppBuilderModuleMainAppCompat {
    final private int SHOW_PROGRESS = 0;
    final private int HIDE_PROGRESS = 1;

    private boolean isFirstStart = true;
    private WebView webView;
    private String currentUrl;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case SHOW_PROGRESS: {
                    if (isFirstStart) {
                        showProgress();
                        isFirstStart = false;
                    }
                }
                break;
                case HIDE_PROGRESS: {
                    hideProgress();
                }
                break;
            }
        }
    };
    private ProgressDialog progressDialog;
    private Widget widget;

    @Override
    public void create() {

        setContentView(R.layout.news_web);

        setTopBarBackgroundColor(Statics.color1);
        setTopBarTitleColor(Color.parseColor("#000000"));
        setTopBarLeftButtonTextAndColor(getResources().getString(R.string.news_back_button), Color.parseColor("#000000"), true, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        currentUrl = getIntent().getStringExtra(NewsConstants.URL);

        widget = (Widget) getIntent().getSerializableExtra(NewsConstants.WIDGET);

        if (widget!= null && !TextUtils.isEmpty(widget.getTitle()) ) {
            setTopBarTitle(widget.getTitle());
        }else setTopBarTitle(" ");

        webView = (WebView) findViewById(R.id.news_web_view);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setLoadsImagesAutomatically(true);
        webView.clearHistory();

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    currentUrl = url;
                    setSession(currentUrl);
                    handler.sendEmptyMessage(SHOW_PROGRESS);

                if(Build.VERSION.SDK_INT > 18)
                    handler.sendEmptyMessageDelayed(HIDE_PROGRESS, 4000);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                handler.sendEmptyMessage(HIDE_PROGRESS);
            }

            @Override
            public void onLoadResource(WebView view, String url) {

                if (url.startsWith("http://www.youtube.com/get_video_info?")) {
                        String path = url.replace("http://www.youtube.com/get_video_info?", "");

                        String[] parqamValuePairs = path.split("&");

                        String videoId = null;

                        for (String pair : parqamValuePairs) {
                            if (pair.startsWith("video_id")) {
                                videoId = pair.split("=")[1];
                                break;
                            }
                        }

                        if (videoId != null) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com"))
                                    .setData(Uri.parse("http://www.youtube.com/watch?v=" + videoId)));

                        }
                } else {
                    super.onLoadResource(view, url);
                }
            }

            @Override
            public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(WebActivity.this);
                builder.setMessage(R.string.news_notification_error_ssl_cert_invalid);
                builder.setPositiveButton(WebActivity.this.getResources().getString(R.string.news_on_continue), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        handler.proceed();
                    }
                });
                builder.setNegativeButton(WebActivity.this.getResources().getString(R.string.news_on_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        handler.cancel();
                    }
                });
                final AlertDialog dialog = builder.create();
                dialog.show();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.contains("youtube.com")) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("http://www.youtube.com")).setData(Uri.parse(url)));
                        return true;
                    } catch (Exception ex) {
                        return false;
                    }
                } else if (url.contains("mailto")) {
                    MailTo mailTo = MailTo.parse(url);

                    Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                    emailIntent.setType("plain/text");
                    emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{mailTo.getTo()});

                    emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, mailTo.getSubject());
                    emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, mailTo.getBody());
                    WebActivity.this.startActivity(Intent.createChooser(emailIntent, getString(R.string.news_send_email)));
                    return super.shouldOverrideUrlLoading(view, url);
                } else if (url.contains("playthis:")) {
                    Intent intent = new Intent();
                    intent.setAction(android.content.Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse(url.replace("playthis:", "")), "audio/*");
                    startActivity(intent);
                    return false;
                }
                currentUrl = url;
                setSession(currentUrl);
                    view.getSettings().setLoadWithOverviewMode(true);
                    view.getSettings().setUseWideViewPort(true);
                    view.setBackgroundColor(Color.WHITE);
                return false;
            }
        });
        webView.setWebChromeClient(new WebChromeClient(){});
        webView.loadUrl(currentUrl);
    }

    private void showProgress() {
        progressDialog = ProgressDialog.show(this, null, getString(R.string.news_loading), true);
        progressDialog.setCancelable(false);
    }

    private void hideProgress() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_open_scale, R.anim.activity_close_translate);
    }
}
