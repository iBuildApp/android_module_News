package com.ibuildapp.romanblack.NewsPlugin.details;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.appbuilder.sdk.android.AppBuilderModuleMainAppCompat;
import com.appbuilder.sdk.android.DialogSharing;
import com.appbuilder.sdk.android.Utils;
import com.appbuilder.sdk.android.Widget;
import com.bumptech.glide.DrawableTypeRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.ibuildapp.romanblack.NewsPlugin.R;
import com.ibuildapp.romanblack.NewsPlugin.WebActivity;
import com.ibuildapp.romanblack.NewsPlugin.model.FeedItem;
import com.ibuildapp.romanblack.NewsPlugin.utils.HtmlUtils;
import com.ibuildapp.romanblack.NewsPlugin.utils.NewsConstants;
import com.ibuildapp.romanblack.NewsPlugin.utils.Statics;
import com.ibuildapp.romanblack.NewsPlugin.utils.TextViewLinkHandler;
import com.ibuildapp.romanblack.NewsPlugin.youtube.YouTubeFragment;
import com.restfb.util.StringUtils;


import org.xml.sax.XMLReader;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rx.functions.Action0;
import rx.schedulers.Schedulers;

public class NewsDetailsActivity extends AppBuilderModuleMainAppCompat {
    private Widget widget = null;
    private String func;

    private FeedItem item = null;

    private View mainLayout;
    private ImageView titleImage;
    private TextView titleText;

    private TextView date;

    private LinearLayout shareLayout;
    private ImageView shareImage;
    private TextView shareText;

    private TextView description;
    private TextView readMore;
    private LinearLayout youTubeContainer;

    private List<String> urls;

    private HashMap<String, Drawable> imageContainer;
    private String textDescription;
    private boolean fullScreenStarted = false;
    private YouTubeFragment fragment;


    @Override
    public void create() {
        setContentView(R.layout.news_details_news);
        setTopBarTitle(getString(R.string.news_html_page));
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

        setTopBarBackgroundColor(Statics.color1);
        setTopBarTitleColor(Color.parseColor("#000000"));
        setTopBarLeftButtonTextAndColor(getResources().getString(R.string.news_back_button), Color.parseColor("#000000"), true, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        titleImage = (ImageView) findViewById(R.id.news_details_news_image);
        titleText = (TextView) findViewById(R.id.news_details_news_title);

        date = (TextView) findViewById(R.id.news_details_news_date);
        youTubeContainer = (LinearLayout) findViewById(R.id.news_details_news_video_container);

        shareLayout = (LinearLayout) findViewById(R.id.news_details_news_share_layout);
        shareImage = (ImageView) findViewById(R.id.news_details_news_share_image);
        shareText = (TextView) findViewById(R.id.news_details_news_share_caption);

        mainLayout = findViewById(R.id.news_details_news_main_layout);
        description = (TextView) findViewById(R.id.news_details_news_description);
        readMore = (TextView) findViewById(R.id.news_details_news_read_more);
        readMore.setTextColor(Statics.color5);

        if (StringUtils.isBlank(item.getLink()))
            readMore.setVisibility(View.GONE);
        else {
            readMore.setVisibility(View.VISIBLE);
            readMore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(NewsDetailsActivity.this, WebActivity.class);
                    intent.putExtra(NewsConstants.URL, item.getLink());
                    startActivity(intent);
                    overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);
                }
            });
        }

        mainLayout.setBackgroundColor(Statics.color1);

        if (item.getFeedUrl()!= null && item.getFeedUrl().contains("youtube.com/feeds/videos.xml")){
            youTubeContainer.setVisibility(View.VISIBLE);
            titleImage.setVisibility(View.GONE);

            fragment = new YouTubeFragment();

            Bundle bundle = new Bundle();
            bundle.putSerializable(NewsConstants.YOUTUBE_URL, item.getLink());
            fragment.setArguments(bundle);

            android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.news_details_news_video_container, fragment);
            transaction.commit();
        }else {
            youTubeContainer.setVisibility(View.GONE);
            if (item.hasImage()) {
                titleImage.setVisibility(View.VISIBLE);

                DrawableTypeRequest<String> request = Glide.with(this).load(item.getImageUrl());

                if (item.getImageUrl().contains(".gif")) {
                    request.asGif().into(titleImage);
                } else request.asBitmap().into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                        postUpdateHolder(titleImage, resource);
                    }
                });

            } else titleImage.setVisibility(View.GONE);
        }
        titleText.setText(item.getTitle());
        titleText.setTextColor(Statics.color3);

        date.setText(item.getPubdate(""));
        date.setTextColor(Statics.color4);

        shareImage.setImageBitmap(Statics.applyColorFilterForResource(this, R.drawable.news_share, Statics.color4, PorterDuff.Mode.MULTIPLY));
        shareText.setTextColor(Statics.color4);

        description.setTextColor(Statics.color4);

        if (!widget.hasParameter("send_mail") && !widget.hasParameter("send_sms"))
            shareLayout.setVisibility(View.GONE);
        else {
            shareLayout.setVisibility(View.VISIBLE);
            shareLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (widget.hasParameter("send_mail") && widget.hasParameter("send_sms")){
                        DialogSharing.Configuration.Builder sharingDialogBuilder = new DialogSharing.Configuration.Builder();

                        sharingDialogBuilder.setSmsSharingClickListener(new DialogSharing.Item.OnClickListener() {
                            @Override
                            public void onClick() {
                                sendSms();
                            }
                        });

                        sharingDialogBuilder.setEmailSharingClickListener(new DialogSharing.Item.OnClickListener() {
                            @Override
                            public void onClick() {
                                sendEmail();
                            }
                        });

                        showDialogSharing(sharingDialogBuilder.build());
                    }else if (widget.hasParameter("send_sms")) {
                                sendSms();
                    }else sendEmail();

                }
            });
        }

        Schedulers.io().createWorker().schedule(new Action0() {
            @Override
            public void call() {
                loadData();
            }
        });
    }

    private void postUpdateHolder(ImageView titleImage, Bitmap resource) {
        titleImage.setImageBitmap(resource);
        float density = getResources().getDisplayMetrics().density;
        int displayWidth = getResources().getDisplayMetrics().widthPixels;

        if (resource.getWidth() < ( displayWidth / 2)) {
            LinearLayout.LayoutParams newParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            newParams.setMargins(0,(int) (10*density),0, 0);

            titleImage.setLayoutParams(newParams);
            titleImage.setScaleType(ImageView.ScaleType.CENTER);
        }else  {

            LinearLayout.LayoutParams newParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            newParams.setMargins(0,(int) (10*density),0, 0);
            titleImage.setLayoutParams(newParams);
            titleImage.setScaleType(ImageView.ScaleType.FIT_START);
        }
    }

    public void loadData(){
        textDescription = item.getDescription();
        final String imgRegex = "<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>";
        Pattern pattern = Pattern.compile(imgRegex);
        Matcher m = pattern.matcher(textDescription);

        List<String> urls = new ArrayList<>();
        while (m.find()) {
            if (m.group(0).toLowerCase().contains(item.getImageUrl())) {
                textDescription = textDescription.replace(m.group(0), "");
                continue;
            }
            Pattern pattern1 = Patterns.WEB_URL;
            Matcher m1 = pattern1.matcher(m.group(0));
            if (m1.find())
                urls.add(m1.group(0));
        }
        imageContainer = new HashMap<>();

        for (String url: urls){
            try {
                Drawable drawable;
                if (url.toLowerCase().contains(".gif"))
                     drawable = Glide.with(this).load(url).asGif().into(-1, -1).get();
                    else drawable = new BitmapDrawable(getResources(), Glide.with(this).load(url).asBitmap().into(-1, -1).get());

                drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
                imageContainer.put(url, drawable);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        textDescription = textDescription == null? "": textDescription.replaceAll("<p></p>", "");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                runActivity();
            }
        });
    }

    public void sendSms(){
        String message = item.getTitle();
        message = message + "\n" + HtmlUtils.html2text(item.getDescription());

        try {
            Utils.sendSms(NewsDetailsActivity.this, message);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void sendEmail(){
        SimpleDateFormat sdf;

        StringBuilder message = new StringBuilder();

        if (Locale.getDefault().toString().equals("en_US"))
            sdf = new SimpleDateFormat("MMMM dd yyyy hh:mm", Locale.getDefault());
        else
            sdf = new SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.getDefault());

        message.append(sdf.format(item.getPubdate()));
        message.append("\n");
        message.append(HtmlUtils.html2text(item.getDescription()));

        Intent email = new Intent(Intent.ACTION_SEND);
        email.putExtra(Intent.EXTRA_SUBJECT, item.getTitle());
        email.putExtra(Intent.EXTRA_TEXT, message.toString());
        email.setType("message/rfc822");
        startActivity(Intent.createChooser(email, getString(R.string.news_choose_email_client)));
    }

    public void runActivity(){
        Spanned spanned = Html.fromHtml(textDescription, new Html.ImageGetter() {
            @Override
            public Drawable getDrawable(String source) {
                if (imageContainer.containsKey(source))
                    return imageContainer.get(source);
                else
                    return null;
            }
        }, new Html.TagHandler() {
            @Override
            public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
                System.out.println("asd");
            }
        });
        final URLSpan[] spans = spanned.getSpans(0, spanned.length(), URLSpan.class);

        urls = new ArrayList<>();
        for (URLSpan span:spans)
            urls.add(span.getURL());

        description.setText(spanned);
        description.setLinkTextColor(Statics.color5);

        description.setMovementMethod(new TextViewLinkHandler() {
            @Override
            public void onLinkClick(String url) {
                /*String newUrl = url;
                for (String span : urls)
                    if (span.contains(url))
                        newUrl = span;*/

                Intent intent = new Intent(NewsDetailsActivity.this, WebActivity.class);
                intent.putExtra(NewsConstants.URL, url);
                startActivity(intent);
                overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);
            }
        });
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_open_scale, R.anim.activity_close_translate);
    }

    @Override
    public void onBackPressed() {
        if (fullScreenStarted){
            if (fragment !=null) {
                fragment.goToPortrait();
            }
        }else super.onBackPressed();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        fullScreenStarted = newConfig.orientation != Configuration.ORIENTATION_PORTRAIT;
    }
}
