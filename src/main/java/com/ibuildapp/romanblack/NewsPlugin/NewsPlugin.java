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

import android.app.*;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.*;

import com.appbuilder.sdk.android.AppBuilderModuleMainAppCompat;
import com.appbuilder.sdk.android.Utils;
import com.appbuilder.sdk.android.Widget;
import com.appbuilder.sdk.android.tools.NetworkUtils;
import com.ibuildapp.romanblack.NewsPlugin.adapters.RssAdapter;
import com.ibuildapp.romanblack.NewsPlugin.adapters.RvEventsAdapter;
import com.ibuildapp.romanblack.NewsPlugin.details.FeedDetails;
import com.ibuildapp.romanblack.NewsPlugin.details.NewsDetailsActivity;
import com.ibuildapp.romanblack.NewsPlugin.model.FeedItem;
import com.ibuildapp.romanblack.NewsPlugin.utils.Statics;
import com.ibuildapp.romanblack.NewsPlugin.parsers.EntityParser;
import com.ibuildapp.romanblack.NewsPlugin.parsers.FeedParser;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;

/**
 * Main module class. Module entry point.
 * Represents News, RSS and Events widgets.
 */
public class NewsPlugin extends AppBuilderModuleMainAppCompat {

    private ArrayList<String> notifications;
    private String cachePath = "";
    private ArrayList<FeedItem> items = new ArrayList<>();
    private Widget widget;
    private Timer timer = null;
    private boolean isOnline = false;
    private boolean useCache = false;
    private RecyclerView listView = null;
    private String feedURL = "";
    private String funcName = "";
    private String title = "";
    private String encoding = "";
    final private int SHOW_FEED = 0;
    final private int SHOW_NEWS = 1;
    final private int SHOW_EVENTS = 2;
    final private int INITIALIZATION_FAILED = 3;
    final private int NEED_INTERNET_CONNECTION = 4;
    final private int REVERSE_LIST = 7;
    final private int REFRESH_RSS = 8;
    final private int CANT_REFRESH_RSS = 9;
    final private int ADD_NOTIFICATIONS = 10;
    final private int COLORS_RECEIVED = 11;
    final private int RSS_NO_ITEMS = 20;

    private View mainLayout = null;
    private Intent currentIntent;
    private View progressLayout;
    private SwipeRefreshLayout refreshLayout;

    /**
     * This class used to show event notification at 30 minutes before event time
     */
    private class EventsTimerTask extends TimerTask {

        private FeedItem item = null;
        private int order = -1;

        @Override
        public void run() {
            try {//ErrorLogging

                if (item != null) {
                    // get event message <description>
                    String message = item.getAnounce(250) + " " + getString(R.string.news_at) + " "
                            + item.getPubdate("");

                    String path = Environment.getExternalStorageDirectory()
                            + "/AppBuilder/" + getPackageName();
                    File file = new File(path);
                    if (!file.exists()) {
                        file.mkdirs();
                    }
                    path += "/.notifications";

                    file = new File(path);
                    if (!file.exists()) {
                        try {
                            file.createNewFile();
                        } catch (Exception ex) {
                            Log.e("", "");
                        }
                    } else {
                        // get notifications from cache file
                        try {
                            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
                            notifications = (ArrayList<String>) ois.readObject();
                            ois.close();
                        } catch (Exception ex) {
                            Log.e("", "");
                        }
                    }

                    // add notification to array
                    if (notifications == null) {
                        notifications = new ArrayList<>();
                    }
                    notifications.add(message);

                    // save modified notification array to cache file
                    try {
                        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
                        oos.writeObject(notifications);
                        oos.close();
                    } catch (Exception ex) {
                        Log.e("", "");
                    }

                    String notificationTitle = item.getTitle();
                    String notificationText = item.getAnounce(70);

                    Intent intent = new Intent(NewsPlugin.this, NotificationDetails.class);
                    Date tempDate = item.getPubdate();

                    String resDateStamp = new SimpleDateFormat("MMMM", Locale.getDefault()).format(tempDate) + " "
                            + new SimpleDateFormat("d", Locale.getDefault()).format(tempDate) + " "
                            + new SimpleDateFormat("y", Locale.getDefault()).format(tempDate) + " "
                            + convertTimeToFormat(tempDate.getHours(), tempDate.getMinutes(), false);

                    intent.putExtra("TITLE", item.getTitle());
                    intent.putExtra("DATE", resDateStamp);
                    intent.putExtra("DESCRIPTION", item.getDescription());

                    NotificationManager mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    // create one notification

                    Notification.Builder builder = new Notification.Builder(NewsPlugin.this);
                    builder.setSmallIcon(R.drawable.news_icon_notification);
                    builder.setTicker(notificationText);
                    builder.setWhen(System.currentTimeMillis());

                    PendingIntent pendingIntent = PendingIntent.getActivity(NewsPlugin.this, (widget.getOrder() * 1000 + this.order), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

                    builder.setContentIntent(pendingIntent);
                    builder.setContentTitle(notificationTitle);
                    builder.setContentText(notificationText);
                    builder.setOngoing(true);
                    builder.setAutoCancel(false);

                    mManager.notify(widget.getTitle()
                                    + "-events-" + widget.getOrder() + " " + item.getTitle(),
                            widget.getOrder(), builder.build());
                }

            } catch (Exception ex) { // Error Logging
            }
        }

        /**
         * Sets the event item and widget order
         *
         * @param item  event item
         * @param order widget order
         */
        public void setItem(FeedItem item, int order) {
            this.item = item;
            this.order = order;
        }
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case INITIALIZATION_FAILED: {
                    Toast.makeText(NewsPlugin.this, R.string.news_cannot_init, Toast.LENGTH_LONG).show();
                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                            finish();
                        }
                    }, 5000);
                }
                break;

                case RSS_NO_ITEMS: {
                    Toast.makeText(NewsPlugin.this, R.string.news_no_items, Toast.LENGTH_LONG).show();
                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                            finish();
                        }
                    }, 2000);
                }
                break;

                case NEED_INTERNET_CONNECTION: {
                    Toast.makeText(NewsPlugin.this, R.string.news_alert_no_internet,
                            Toast.LENGTH_LONG).show();
                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                            finish();
                        }
                    }, 5000);
                }
                break;
                case SHOW_FEED: {
                    NewsPlugin.this.showFeed();
                }
                break;
                case SHOW_NEWS: {
                    NewsPlugin.this.showNews();
                }
                break;
                case SHOW_EVENTS: {
                    NewsPlugin.this.showEvents();
                }
                break;

                case REVERSE_LIST: {
                    reverseItems();
                }
                break;
                case REFRESH_RSS: {
                    loadRSS();
                }
                break;
                case ADD_NOTIFICATIONS: {
                    addNotification();
                }
                break;
                case CANT_REFRESH_RSS: {
                    Toast.makeText(NewsPlugin.this, R.string.news_alert_no_internet,
                            Toast.LENGTH_LONG).show();
                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                        }
                    }, 5000);
                }
                break;
                case COLORS_RECEIVED: {
                    colorsReceived();
                }
                break;
            }
        }
    };

    @Override
    public void create() {
        try {
            setContentView(R.layout.news_feed_main);
            setTitle(R.string.news_feed);
            setTopbarTitleTypeface(Typeface.NORMAL);

            mainLayout = findViewById(R.id.news_feed_main_layout);
            listView = (RecyclerView) findViewById(R.id.news_feed_main_list);
            listView.setLayoutManager(new LinearLayoutManager(this));

            progressLayout = findViewById(R.id.news_feed_main_progress_layout);
            refreshLayout = (SwipeRefreshLayout) findViewById(R.id.news_feed_main_refresh);
            refreshLayout.setEnabled(false);

            refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    loadRSS();
                }
            });

            currentIntent = getIntent();
            Bundle store = currentIntent.getExtras();
            widget = (Widget) store.getSerializable("Widget");

            if (widget == null) {
                handler.sendEmptyMessageDelayed(INITIALIZATION_FAILED, 100);
                return;
            }

            if ( !TextUtils.isEmpty(widget.getTitle()) ) {
                setTopBarTitle(widget.getTitle());
            }

            setTopBarTitleColor(Color.parseColor("#000000"));

            setTopBarLeftButtonTextAndColor(getResources().getString(R.string.news_home_button), Color.parseColor("#000000"), true, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                }
            });

            try {
                if (widget.getPluginXmlData().length() == 0) {
                    if (widget.getPathToXmlFile().length() == 0) {
                        handler.sendEmptyMessageDelayed(INITIALIZATION_FAILED, 3000);
                        return;
                    }
                }
            } catch (Exception e) {
                handler.sendEmptyMessageDelayed(INITIALIZATION_FAILED, 3000);
                return;
            }


            cachePath = widget.getCachePath() + "/feed-" + widget.getOrder();
            File cache = new File(this.cachePath);
            if (!cache.exists()) {
                cache.mkdirs();
            }

            // 3 seconds

            String widgetMD5 = Utils.md5(widget.getPluginXmlData());
            File cacheData = new File(cachePath + "/cache.data");
            if (cacheData.exists() && cacheData.length() > 0) {
                String cacheMD5 = readFileToString(cachePath + "/cache.md5").replace("\n", "");
                if (cacheMD5.equals(widgetMD5)) {
                    useCache = true;
                } else {
                    File[] files = cache.listFiles();
                    for (File file : files) {
                        file.delete();
                    }
                    try {
                        BufferedWriter bw = new BufferedWriter(new FileWriter(new File(cachePath + "/cache.md5")));
                        bw.write(widgetMD5);
                        bw.close();
                        Log.d("IMAGES PLUGIN CACHE MD5", "SUCCESS");
                    } catch (Exception e) {
                        Log.w("IMAGES PLUGIN CACHE MD5", e);
                    }
                }
            }

            isOnline = NetworkUtils.isOnline(this);

            if (!isOnline && !useCache) {
                handler.sendEmptyMessage(NEED_INTERNET_CONNECTION);
                return;
            }

            new Thread() {
                @Override
                public void run() {
                    timer = new Timer("EventsTimer", true);
                    EntityParser parser;

                    if (widget.getPluginXmlData().length() > 0) {
                        parser = new EntityParser(widget.getPluginXmlData());

                    } else {
                        String xmlData = readXmlFromFile(widget.getPathToXmlFile());
                        parser = new EntityParser(xmlData);

                    }

                    parser.parse();

                    Statics.color1 = parser.getColor1();
                    Statics.color2 = parser.getColor2();
                    Statics.color3 = parser.getColor3();
                    Statics.color4 = parser.getColor4();
                    Statics.color5 = parser.getColor5();

                    handler.sendEmptyMessage(COLORS_RECEIVED);

                    title = (widget.getTitle().length() > 0) ? widget.getTitle() : parser.getFuncName();
                    items = parser.getItems();

                    if ("rss".equals(parser.getFeedType())) {
                        Statics.isRSS = true;
                        AndroidSchedulers.mainThread().createWorker().schedule(new Action0() {
                            @Override
                            public void call() {
                                refreshLayout.setColorSchemeColors(Statics.color3);
                            }
                        });

                        feedURL = parser.getFeedUrl();
                        if (isOnline) {
                            FeedParser reader = new FeedParser(parser.getFeedUrl());
                            items = reader.parseFeed();

                            encoding = reader.getEncoding();

                            if (items.size() > 0) {
                                try {
                                    ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(cachePath + "/cache.data"));

                                    oos.writeObject(items);
                                    oos.flush();
                                    oos.close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                            }

                            Statics.isRSS = true;
                            AndroidSchedulers.mainThread().createWorker().schedule(new Action0() {
                                @Override
                                public void call() {
                                    refreshLayout.setEnabled(true);
                                }
                            });
                        } else {
                            try {
                                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(cachePath + "/cache.data"));
                                items = (ArrayList<FeedItem>) ois.readObject();
                                ois.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }else{
                        Statics.isRSS = false;
                        AndroidSchedulers.mainThread().createWorker().schedule(new Action0() {
                            @Override
                            public void call() {
                                refreshLayout.setEnabled(false);
                            }
                        });
                    }

                    for (int i = 0; i < items.size(); i++) {
                        items.get(i).setTextColor(widget.getTextColor());
                        items.get(i).setDateFormat(widget.getDateFormat());
                    }

                    funcName = parser.getFuncName();
                    selectShowType();
                }
            }.start();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * This menu contains reverse and refresh buttons
     *
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.news_menu_main, menu);
        menu.clear();

        MenuItem menuItem = menu.add("");
        menuItem.setTitle(getString(R.string.news_reverse));
        menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                handler.sendEmptyMessage(REVERSE_LIST);
                return true;
            }
        });

        if (Statics.isRSS) {
            menuItem = menu.add("");
            menuItem.setTitle(R.string.news_refresh);
            menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (isOnline) {
                        handler.sendEmptyMessage(REFRESH_RSS);
                    } else {
                        handler.sendEmptyMessage(CANT_REFRESH_RSS);
                    }
                    return true;
                }
            });
        }

        return false;
    }

    /**
     * Reverse RSS items and redraw them
     */
    private void reverseItems() {
        Collections.reverse(items);

        selectShowType();
    }

    /**
     * Async loading and parsing RSS feed.
     */
    private void loadRSS() {
        try {//ErrorLogging

            new Thread() {
                @Override
                public void run() {
                    // parsing
                    FeedParser reader = new FeedParser(feedURL);
                    items = reader.parseFeed();

                    if (items.size() > 0) {
                        File cache = new File(cachePath);
                        File[] files = cache.listFiles();
                        for (File file : files) {
                            if (!file.getName().equals("cache.md5"))
                                file.delete();
                        }

                        try {
                            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(cachePath + "/cache.data"));
                            oos.writeObject(items);
                            oos.flush();
                            oos.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    for (int i = 0; i < items.size(); i++) {
                        items.get(i).setTextColor(widget.getTextColor());
                        items.get(i).setDateFormat(widget.getDateFormat());
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            refreshLayout.setRefreshing(false);
                        }
                    });
                    selectShowType();
                }
            }.start();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Select show type depending on function name
     */
    private void selectShowType() {
        if (!(items != null && items.size() > 0)) {
            handler.sendEmptyMessage(RSS_NO_ITEMS);
            finish();
            return;
        }

        if (funcName.equalsIgnoreCase("EVENTS")) {
            handler.sendEmptyMessage(SHOW_EVENTS);
        } else if (funcName.equalsIgnoreCase("NEWS")) {
            handler.sendEmptyMessage(SHOW_NEWS);
        } else if (funcName.equalsIgnoreCase("RSS")) {
            handler.sendEmptyMessage(SHOW_FEED);
        }
    }

    /**
     * Shows RSS feed list.
     * Called when func = "rss"
     */
    private void showFeed() {
        setTitle(title);
        if (items.isEmpty()) {
            return;
        }

        progressLayout.setVisibility(View.GONE);
        listView.setAdapter(new RssAdapter(this, items));
    }

    /**
     * Show RSS feed list.
     * Called when func = "news"
     */
    private void showNews() {
        try {
            setTitle(title);
            if (items.isEmpty()) {
                return;
            }
            progressLayout.setVisibility(View.GONE);
            listView.setAdapter(new RssAdapter(this, items));

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Show RSS feed list.
     * Called when func = "events"
     */
    private void showEvents() {
        try {

            Log.e("ibuildapp time", "Show events start " + new Date(System.currentTimeMillis()));
            setTitle(title);
            if (items.isEmpty()) {
                return;
            }

            progressLayout.setVisibility(View.GONE);
            listView.setAdapter(new RvEventsAdapter(this, items));

            if (widget.hasParameter("add_local_notific")) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getString(R.string.news_dialog_add_notification));
                builder.setPositiveButton(getString(R.string.news_yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        handler.sendEmptyMessage(ADD_NOTIFICATIONS);
                    }
                });
                builder.setNegativeButton(getString(R.string.news_no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                    }
                });
                builder.create().show();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void startRssDetails(int adapterPosition) {
        FeedItem currentItem = items.get(adapterPosition);

        if ((currentItem.getMediaType()!= null && currentItem.getMediaType().contains("image")) || !currentItem.hasMedia() || currentItem.getDescription().length() > 70) {
            Intent details = new Intent(this, NewsDetailsActivity.class);
            Bundle store = new Bundle();
            store.putString("func", funcName);
            store.putSerializable("Widget", widget);
            store.putSerializable("item", items.get(adapterPosition));
            if (encoding.equals("")) {
                store.putString("enc", "UTF-8");
            } else {
                store.putString("enc", encoding);
            }
            details.putExtras(store);
            startActivity(details);
            overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);
        } else if (currentItem.getMediaType().contains("video")) {
            Intent details = new Intent(this, VideoPlayer.class);
            Bundle store = new Bundle();
            store.putString("link", currentItem.getMediaUrl());
            store.putString("cache", cachePath);
            store.putSerializable("Widget", widget);
            store.putSerializable("item", currentItem);
            details.putExtras(store);
            startActivity(details);
            overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);
        } else if (currentItem.getMediaType().contains("audio")) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(currentItem.getMediaUrl()),
                    "audio/*");
            startActivity(intent);
            overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);
        }
    }

    public void startEventDetails(int adapterPosition) {
        showDetails(adapterPosition);
    }

    private void showDetails(int position) {
        try {

            if ((items.get(position).getMediaType()!= null && items.get(position).getMediaType().contains("image")) || !items.get(position).hasMedia() || items.get(position).getDescription().length() > 70) {

                Intent details = new Intent(this, FeedDetails.class);
                Bundle store = new Bundle();
                store.putString("func", funcName);
                store.putSerializable("Widget", widget);
                store.putSerializable("item", items.get(position));
                if (encoding.equals("")) {
                    store.putString("enc", "UTF-8");
                } else {
                    store.putString("enc", encoding);
                }
                details.putExtras(store);
                startActivity(details);
                overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);
            } else {
                if (items.get(position).getMediaType().contains("video")) {
                    Intent details = new Intent(this, VideoPlayer.class);
                    Bundle store = new Bundle();
                    store.putString("link", items.get(position).getMediaUrl());
                    store.putString("cache", cachePath);
                    store.putSerializable("Widget", widget);
                    store.putSerializable("item", items.get(position));
                    details.putExtras(store);
                    startActivity(details);
                    overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);
                } else if (items.get(position).getMediaType().contains("audio")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse(items.get(position).getMediaUrl()),
                            "audio/*");
                    startActivity(intent);
                    overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);

                }
            }

        } catch (Exception ex) { // Error Logging
            ex.printStackTrace();
        }
    }

    private String readFileToString(String pathToFile) {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(pathToFile)));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    /**
     * Called when module colors was parsed.
     */
    private void colorsReceived() {
        setTopBarBackgroundColor(Statics.color1);
        try {
            mainLayout.setBackgroundColor(Statics.color1);
        } catch (NullPointerException nPEx) {
            nPEx.printStackTrace();
        }
    }

    /**
     * Set Timer Tasks to show events notifications.
     */
    private void addNotification() {
        // create array of events and set wait state

        int counter = 0;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getPubdate().getTime()
                    > System.currentTimeMillis()) {

                try {
                    EventsTimerTask tt = new EventsTimerTask();
                    tt.setItem(items.get(i), i);
                    timer.schedule(tt, new Date(
                            items.get(i).getPubdate().getTime()
                                    - 30 * 60 * 1000));
                    counter++;
                } catch (Exception ex) {
                    Log.w("", "");
                }
            }
        }
        Toast.makeText(NewsPlugin.this, getString(R.string.news_alert_notifications_added_first_part) + " " + counter + "  " + getString(R.string.news_alert_notifications_added_seconds_part), Toast.LENGTH_LONG).show();
    }

    /**
     * This function converts data and time represented in minutes in necessary format
     */
    private String convertTimeToFormat(int hh, int mm, boolean format) {
        String tempHourString;
        String tempMinString;
        if (format){ // use 24 format
            // processing houres
            if (Integer.toString(hh).length() < 2) {
                tempHourString = Integer.toString(hh);
                tempHourString = "0" + tempHourString;
            } else
                tempHourString = Integer.toString(hh);

            // processing minutes
            if (Integer.toString(mm).length() < 2) {
                tempMinString = Integer.toString(mm);
                tempMinString = "0" + tempMinString;
            } else
                tempMinString = Integer.toString(mm);

            return tempHourString + ":" + tempMinString;
        } else {// use am/pm format
            String amPm;
            int temp_sum = hh * 100 + mm;
            if (temp_sum >= 1200)
                amPm = "PM";
             else
                amPm = "AM";

            // processing minutes
            if (Integer.toString(mm).length() < 2) {
                tempMinString = Integer.toString(mm);
                tempMinString = "0" + tempMinString;
            } else
                tempMinString = Integer.toString(mm);

            // processing hours
            if (hh > 12) {
                int tempHH = hh;
                tempHH = tempHH - 12;

                if (Integer.toString(tempHH).length() < 2) {
                    tempHourString = Integer.toString(tempHH);
                    tempHourString = "0" + tempHourString;
                } else {
                    tempHourString = Integer.toString(tempHH);
                }
            } else {
                if (Integer.toString(hh).length() < 2) {
                    tempHourString = Integer.toString(hh);
                    tempHourString = "0" + tempHourString;
                } else
                    tempHourString = Integer.toString(hh);
            }
            return tempHourString + ":" + tempMinString + " " + amPm;

        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_open_scale, R.anim.activity_close_translate);
    }
}