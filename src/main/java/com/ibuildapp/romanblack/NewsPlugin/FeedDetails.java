/****************************************************************************
 *                                                                           *
 *  Copyright (C) 2014-2015 iBuildApp, Inc. ( http://ibuildapp.com )         *
 *                                                                           *
 *  This file is part of iBuildApp.                                          *
 *                                                                           *
 *  This Source Code Form is subject to the terms of the iBuildApp License.  *
 *  You can obtain one at http://ibuildapp.com/license/                      *
 *                                                                           *
 ****************************************************************************/
package com.ibuildapp.romanblack.NewsPlugin;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.*;
import android.content.DialogInterface.OnCancelListener;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Picture;
import android.net.ConnectivityManager;
import android.net.MailTo;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.appbuilder.sdk.android.AppBuilderModuleMain;
import com.appbuilder.sdk.android.DialogSharing;
import com.appbuilder.sdk.android.Utils;
import com.appbuilder.sdk.android.Widget;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

/**
 * This Activity represents rss item or event details.
 */
public class FeedDetails extends AppBuilderModuleMain {

    private int screenWidth;

    private enum states {

        EMPTY, LOAD_START, LOAD_PROGRESS, LOAD_COMPLETE
    }
    private boolean needRefresh = false;
    private AlertDialog ad;
    private Widget widget = null;
    private String func;
    private WebView webView = null;
    private ProgressDialog progressDialog = null;
    final private int SHOW_PROGRESS = 0;
    final private int HIDE_PROGRESS = 1;
    final private int NEED_INTERNET_CONNECTION = 2;
    final private int LOADING_ABORTED = 5;
    private FeedItem item = null;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case SHOW_PROGRESS: {
                    showProgress();
                }
                break;
                case HIDE_PROGRESS: {
                    hideProgress();
                }
                break;
                case NEED_INTERNET_CONNECTION: {
                    Toast.makeText(FeedDetails.this, R.string.romanblack_rss_alert_no_internet, Toast.LENGTH_LONG).show();
                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                            closeActivity();
                        }
                    }, 5000);
                }
                break;
                case LOADING_ABORTED: {
//                    closeActivity();
                }
                break;
            }
        }
    };
    private String currentUrl = "";
    private states state = states.EMPTY;
    private boolean isOnline = false;

    @Override
    public void create() {
        setContentView(R.layout.romanblack_feed_details);

        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics metrix = new DisplayMetrics();
        display.getMetrics(metrix);
        screenWidth = metrix.widthPixels;

        setTopBarTitle(getString(R.string.romanblack_rss_html_page));
        Intent currentIntent = getIntent();
        Bundle store = currentIntent.getExtras();
        item = (FeedItem) store.getSerializable("item");
        if (item == null) {
            finish();
        }

        widget = (Widget) store.getSerializable("Widget");
        if (widget != null) {
            if (widget.getTitle().length() > 0) {
                setTopBarTitle(widget.getTitle());
            }
        }

        func = store.getString("func");

        setTopBarLeftButtonText(getString(R.string.rss_back_button), true, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        if (!(!widget.hasParameter("send_mail")
                && !widget.hasParameter("send_sms")
                && (!func.equals("events") || !widget.hasParameter("add_event")))) {
            ImageView shareButton = (ImageView) getLayoutInflater().inflate(R.layout.romanblack_rss_share_btn, null);
            shareButton.setLayoutParams(new LinearLayout.LayoutParams((int) (29 * getResources().getDisplayMetrics().density), (int) (39 * getResources().getDisplayMetrics().density)));
            shareButton.setColorFilter(navBarDesign.itemDesign.textColor);
            setTopBarRightButton(shareButton, getString(R.string.rss_list_share), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DialogSharing.Configuration.Builder sharingDialogBuilder = new DialogSharing.Configuration.Builder();

                    if (func.equals("events") && widget.hasParameter("add_event")) {
                        sharingDialogBuilder.addCustomListener(R.string.news_add_event_to_calendar, R.drawable.add_to_calendar, true, new DialogSharing.Item.OnClickListener() {
                            @Override
                            public void onClick() {
                                addToCalendar(item.getPubdate().getTime(), item.getTitle());
                            }
                        });
                    }

                    if (widget.hasParameter("send_sms")) {
                        sharingDialogBuilder.setSmsSharingClickListener(new DialogSharing.Item.OnClickListener() {
                            @Override
                            public void onClick() {
                                String message = item.getTitle();
                                message = message + "\n" + html2text(item.getDescription());

                                try {
                                    Utils.sendSms(FeedDetails.this, message);
                                } catch (ActivityNotFoundException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }

                    if (widget.hasParameter("send_mail")) {
                        sharingDialogBuilder.setEmailSharingClickListener(new DialogSharing.Item.OnClickListener() {
                            @Override
                            public void onClick() {
                                SimpleDateFormat sdf = null;

                                StringBuilder message = new StringBuilder();

                                if (Locale.getDefault().toString().equals("en_US")) {
                                    sdf = new SimpleDateFormat("MMMM dd yyyy hh:mm");
                                } else {
                                    sdf = new SimpleDateFormat("dd MMMM yyyy HH:mm");
                                }


                                message.append(sdf.format(item.getPubdate()));
                                message.append("\n");
                                message.append(html2text(item.getDescription()));

                                Intent email = new Intent(Intent.ACTION_SEND);
                                email.putExtra(Intent.EXTRA_SUBJECT, item.getTitle());
                                email.putExtra(Intent.EXTRA_TEXT, message.toString());
                                email.setType("message/rfc822");
                                startActivity(Intent.createChooser(email, getString(R.string.choose_email_client)));
                            }
                        });
                    }showDialogSharing(sharingDialogBuilder.build());
                }
            });
        }
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni != null && ni.isConnectedOrConnecting()) {
            isOnline = true;
        }


        if (widget.hasParameter("add_event")) {
            HashMap<String, String> hm = new HashMap<String, String>();
            hm.put("title", item.getTitle());
            hm.put("begin", item.getPubdate("dd MMM yyyy HH:mm"));
            hm.put("end", "");
            hm.put("frequency", "FREQ=ONCE");
            addNativeFeature(NATIVE_FEATURES.ADD_EVENT, null, hm);
        }
        if (widget.hasParameter("send_sms"))
            if (widget.hasParameter("send_mail")) {{
                HashMap<String, String> hm = new HashMap<String, String>();
                StringBuilder sb = new StringBuilder();
                sb.append(item.getTitle());
                sb.append(", ");
                if (item.getPubdate("").length() > 0) {
                    sb.append(item.getPubdate(""));
                    sb.append(", ");
                }
                sb.append(item.getAnounce(0));
                hm.put("text", sb.toString());
                addNativeFeature(NATIVE_FEATURES.SMS, null, hm);
            }
                HashMap<String, CharSequence> hm = new HashMap<String, CharSequence>();
                hm.put("subject", item.getTitle());

                StringBuilder sb = new StringBuilder();
                sb.append(item.getTitle());
                sb.append("<br>\n");
                if (item.getPubdate("").length() > 0) {
                    sb.append("");
                    sb.append(item.getPubdate(""));
                    sb.append("<br>\n");
                }
                sb.append(item.getAnounce(0));
                if (widget.isHaveAdvertisement()) {
                    sb.append("<br/><br/>\n (sent from <a href=\"http://ibuildapp.com\">iBuildApp</a>)");
                }
                hm.put("text", sb.toString());
                addNativeFeature(NATIVE_FEATURES.EMAIL, null, hm);
            }

        webView = (WebView) findViewById(R.id.romanblack_rss_feedDetails);
        webView.getSettings().setJavaScriptEnabled(true);
        //webView.getSettings().setPluginsEnabled(true);
        webView.getSettings().setBuiltInZoomControls(true);
        //webView.getSettings().setPluginsEnabled(true);
        webView.getSettings().setLoadsImagesAutomatically(true);
        webView.clearHistory();

        webView.setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (state == states.EMPTY) {
                    currentUrl = url;
                    setSession(currentUrl);
                    state = states.LOAD_START;
                    handler.sendEmptyMessage(SHOW_PROGRESS);

                    if(Build.VERSION.SDK_INT > 18)
                        handler.sendEmptyMessageDelayed(HIDE_PROGRESS, 4000);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                state = states.LOAD_COMPLETE;
                handler.sendEmptyMessage(HIDE_PROGRESS);
            }

            @Override
            public void onLoadResource(WebView view, String url) {

                if (url.startsWith("http://www.youtube.com/get_video_info?")) {
                    try {
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
                            needRefresh = true;

                            return;
                        }
                    } catch (Exception ex) {
                    }
                } else {
                    super.onLoadResource(view, url);
                }
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
                    FeedDetails.this.startActivity(Intent.createChooser(emailIntent, getString(R.string.romanblack_rss_send_email)));
                    return super.shouldOverrideUrlLoading(view, url);
                } else if (url.contains("playthis:")) {
                    String newUrl = new String(url);
                    Intent intent = new Intent();
                    intent.setAction(android.content.Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse(newUrl.replace("playthis:", "")), "audio/*");
                    startActivity(intent);
                    return false;
                }
                currentUrl = url;
                setSession(currentUrl);
                if (!isOnline) {
                    handler.sendEmptyMessage(HIDE_PROGRESS);
                    handler.sendEmptyMessage(NEED_INTERNET_CONNECTION);
                } else {
                    view.getSettings().setLoadWithOverviewMode(true);
                    view.getSettings().setUseWideViewPort(true);
                    view.setBackgroundColor(Color.WHITE);
                }
                return false;
            }
        });
        webView.setWebChromeClient(new WebChromeClient(){});

        currentUrl = (String) getSession();
        if (currentUrl == null) {
            currentUrl = "";
        }

        if (currentUrl.length() > 0 && !currentUrl.equals("about:blank")) {
            webView.loadUrl(currentUrl);
            webView.getSettings().setLoadWithOverviewMode(true);
            webView.getSettings().setUseWideViewPort(true);
            webView.setBackgroundColor(Color.WHITE);

            handler.sendEmptyMessageDelayed(HIDE_PROGRESS, 10000);
        } else {
            StringBuilder html = new StringBuilder();
            SimpleDateFormat sdf = null;

            if (Locale.getDefault().toString().equals("en_US")) {
                sdf = new SimpleDateFormat("MMMM dd yyyy hh:mm");
            } else {
                sdf = new SimpleDateFormat("dd MMMM yyyy HH:mm");
            }

            if(Statics.isRSS){
                html.append("<div style=\"overflow:hidden;\">");
                html.append("<style>a { text-decoration: none; color:#3399FF;}</style>");
                html.append("<span style='font-family:Helvetica; font-size:16px; font-weight:bold;'>");
                html.append(item.getTitle());
                html.append("</span><br/>");
                if (item.getPubdate() != null) {
                    html.append("<span style='font-family:Helvetica; font-size:12px;  color:#555555;'' >");

                    if (widget.getDateFormat() == 0) {
                        html.append(sdf.format(item.getPubdate()));
                    } else {
                        html.append(sdf.format(item.getPubdate()));
                    }

                    html.append("</span><br/>");
                }
                html.append("<br/>");
                html.append(item.getDescription());
                html.append("<br/><br/>");
                html.append("</div>");
            }else{
                html.append("<style>a { text-decoration: none; color:#3399FF;}</style>");
                html.append("<span style='font-family:Helvetica; font-size:16px; font-weight:bold;'>");
                html.append(item.getTitle());
                html.append("</span><br/>");
                if (item.getPubdate() != null) {
                    html.append("<span style='font-family:Helvetica; font-size:12px;  color:#555555;'' >");

                    if (widget.getDateFormat() == 0) {
                        html.append(sdf.format(item.getPubdate()));
                    } else {
                        html.append(sdf.format(item.getPubdate()));
                    }

                    html.append("</span><br />");
                }
                html.append("<br />");
                if (item.hasImage() && !item.isDescriptionContainsImages()) {
                    html.append("<img src=\"");
                    html.append(item.getImageUrl());
                    html.append("\" height=\"150\" border=\"0\" align=\"left\" style=\"margin-right:5px;\">");
                }
                html.append(item.getDescription());
            }

            if (item.getLink().length() > 0) {
                html.append("<br/><a href=\"");
                html.append(item.getLink());
                html.append("\"><strong>" + getString(R.string.romanblack_rss_read_more) + "...</strong></a>");
            }
            if (item.hasMedia()) {
                html.append("<br/><a href=\"playthis:"+ item.getMediaUrl() +"\" "
                        + "onClick=\"window.jsi.clickOnAndroid()\"><strong>");
                html.append(getString(R.string.romanblack_rss_show_media) + "...");
                html.append("</strong></a>");
            }

            String result = html.toString();
            result = result.replace("img src=\"//", "img src=\"http://");


            result = pastImgStyleTag(result);
            webView.loadDataWithBaseURL(null, result, "text/html", "UTF-8", null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }


    @Override
    public void resume() {
        if (currentUrl.equals("about:blank")) {
            StringBuilder html = new StringBuilder();
            html.append("<html>");
            html.append("<body>");
            html.append("<font size=\"4\"><b>");
            html.append(item.getTitle());
            html.append("</b></font><br/>");
            html.append("<font size=\"2\" color=\"#5A5A5A\">");
            if (widget.getDateFormat() == 0) {
                html.append(item.getPubdate("MMM d yy hh:mm a"));
            } else {
                html.append(item.getPubdate("MMM d yy HH:mm"));
            }
            html.append("</font>");
            html.append("<br/><br/>");
            if (item.hasImage() && !item.isDescriptionContainsImages()) {
                html.append("<img src=\"");
                html.append(item.getImageUrl());
                html.append("\"/ width=\"100%\">");
                html.append("<br/><br/>");
            }
            html.append(item.getDescription());
            if (item.getLink().length() > 0) {
                html.append("<br><br><a href=\"");
                html.append(item.getLink());
                html.append("\"><strong>" + getString(R.string.romanblack_rss_read_more) + "...</strong></a>");
            }
            if (item.hasMedia()) {
                html.append("<br/><a href=\"\" "
                        + "onClick=\"window.jsi.clickOnAndroid()\">");
                html.append(getString(R.string.romanblack_rss_show_media) + "...");
                html.append("</a>");
            }
            html.append("</body>");
            html.append("</html>");

            String result = html.toString();
            result = result.replace("img src=\"//", "img src=\"http://");
            webView.loadDataWithBaseURL(null, result, "text/html", "UTF-8", null);
        } else if (needRefresh) {
            webView.reload();
            needRefresh = false;
        }
    }

    /**
     * Add event to calendar
     * @param beginTime - event time
     * @param title - event title
     */
    void addToCalendar(Long beginTime, String title) {
        ContentResolver cr = getContentResolver();

        Uri.Builder builder = Uri.parse(
                "content://com.android.calendar/instances/when")
                .buildUpon();
        Long time = new Date(beginTime).getTime();
        ContentUris.appendId(builder, time - 10 * 60 * 1000);
        ContentUris.appendId(builder, time + 10 * 60 * 1000);

        String[] projection = new String[]{
                "title", "begin"};
        Cursor cursor = cr.query(builder.build(),
                projection, null, null, null);

        boolean exists = false;
        if (cursor != null) {
            while (cursor.moveToNext()) {
                if ((time == cursor.getLong(1))
                        && title.equals(cursor.getString(0))) {
                    exists = true;
                }
            }
        }

        if (!exists) {
            Intent intent = new Intent(Intent.ACTION_EDIT);
            intent.setType("vnd.android.cursor.item/event");
            intent.putExtra("beginTime", time);
            intent.putExtra("allDay", false);
            intent.putExtra("endTime", time + 60 * 60 * 1000);
            intent.putExtra("title", title);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Event already exist!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void pause() {
        webView.stopLoading();
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    @Override
    public void destroy() {
        webView.stopLoading();
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    /* PRIVATE METHODS */
    private void showProgress() {
        if (state == states.LOAD_START) {
            state = states.LOAD_PROGRESS;
        }

        progressDialog = ProgressDialog.show(this, null, getString(R.string.romanblack_rss_loading), true);
        progressDialog.setCancelable(false);
        progressDialog.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                handler.sendEmptyMessage(LOADING_ABORTED);
            }
        });
    }

    private void hideProgress() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        state = states.EMPTY;
    }

    public void closeActivity() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        finish();
    }

    /**
     * Prepare text from HTML without HTML tags
     * @param html - convert
     * @return text without html tags
     */
    public static String html2text(String html) {
        return Jsoup.parse(html).text();
    }

    private String pastImgStyleTag(String pHTML) {
        Document doc = Jsoup.parse(pHTML);

        String styletoPast = "<style>img{display: inline;height: auto;max-width: 100%;}</style>";
        Elements head = doc.select("head");
        Iterator<Element> iterator = head.iterator();
        for (; iterator.hasNext(); ) {
            Element headEl = iterator.next();
            headEl.append(styletoPast);
        }
        return doc.html();
    }
}
