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
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import com.appbuilder.sdk.android.AppBuilderModuleMain;
import com.appbuilder.sdk.android.Utils;
import com.appbuilder.sdk.android.Widget;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Main module class. Module entry point.
 * Represents News, RSS and Events widgets.
 */
public class NewsPlugin extends AppBuilderModuleMain {


    private ArrayList<String> notifications;
    private String cachePath = "";
    private ArrayList<FeedItem> items = new ArrayList<FeedItem>();
    private BaseAdapter adapter;
    private ProgressDialog progressDialog = null;
    private Widget widget;
    private Timer timer = null;
    private boolean isOnline = false;
    private boolean useCache = false;
    private ListView listView = null;
    private String feedURL = "";
    private String funcName = "";
    private String title = "";
    private String encoding = "";
    private String cacheMD5 = "";
    private String widgetMD5 = "";
    final private int SHOW_FEED = 0;
    final private int SHOW_NEWS = 1;
    final private int SHOW_EVENTS = 2;
    final private int INITIALIZATION_FAILED = 3;
    final private int NEED_INTERNET_CONNECTION = 4;
    final private int LOADING_ABORTED = 5;
    final private int CLEAR_ITEM_VIEW = 6;
    final private int REVERSE_LIST = 7;
    final private int REFRESH_RSS = 8;
    final private int CANT_REFRESH_RSS = 9;
    final private int ADD_NOTIFICATIONS = 10;
    final private int COLORS_RECIEVED = 11;
    final private int HIDE_PROGRESS_DIALOG = 12;
    final private int RESET_COLOR = 13;
    final private int RSS_NO_ITEMS = 20;
    private View mainlLayout = null;
    private Intent currentIntent;
    private ConnectivityManager cm = null;

    /**
     * This class used to show event notification at 30 minutes before event time
     */
    private class EventsTimerTask extends TimerTask {

        private FeedItem item = null;
        private int order = -1;

        @Override
        public void run() {
            try {

                if (item != null) {
                    // get event message <description>
                    String message = item.getAnounce(250) + " " + getString(R.string.romanblack_rss_at) + " "
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
                        notifications = new ArrayList<String>();
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

                    String notificationBarTitle = item.getTitle();
                    String notificationTitle = item.getTitle();
                    String notificationText = item.getAnounce(70);

                    Intent intent = new Intent(NewsPlugin.this, NotificationDetails.class);
                    Date tempDate = item.getPubdate();

                    String resDateStamp = new SimpleDateFormat("MMMM").format(tempDate) + " "
                            + new SimpleDateFormat("d").format(tempDate) + " "
                            + new SimpleDateFormat("y").format(tempDate) + " "
                            + convertTimeToFormat(tempDate.getHours(), tempDate.getMinutes(), false);

                    intent.putExtra("TITLE", item.getTitle());
                    intent.putExtra("DATE", resDateStamp);
                    intent.putExtra("DESCRIPTION", item.getDescription());

                    NotificationManager mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    // create one notification
                    Notification notification = new Notification(R.drawable.icon_notification,
                            notificationBarTitle, System.currentTimeMillis());
                    notification.flags |= Notification.FLAG_AUTO_CANCEL;

                    PendingIntent pendingIntent = PendingIntent.getActivity(NewsPlugin.this, (int) (widget.getOrder() * 1000 + this.order), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
                    notification.setLatestEventInfo(NewsPlugin.this, notificationTitle, notificationText, pendingIntent);

                    mManager.notify(getResources().getString(R.string.app_name)
                            + "-events-" + widget.getOrder() + " " + item.getTitle(),
                            widget.getOrder(), notification);
                }

            } catch (Exception ex) { 
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
                    Toast.makeText(NewsPlugin.this, R.string.romanblack_rss_cannot_init, Toast.LENGTH_LONG).show();
                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                            finish();
                        }
                    }, 5000);
                }
                break;

                case RSS_NO_ITEMS: {
                    Toast.makeText(NewsPlugin.this, R.string.rss_no_items, Toast.LENGTH_LONG).show();
                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                            finish();
                        }
                    }, 2000);
                }
                break;

                case NEED_INTERNET_CONNECTION: {
                    Toast.makeText(NewsPlugin.this, R.string.romanblack_rss_alert_no_internet,
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
                case LOADING_ABORTED: {
                    NewsPlugin.this.closeActivity();
                }
                break;
                case CLEAR_ITEM_VIEW: {
                    clearItemView();
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
                    Toast.makeText(NewsPlugin.this, R.string.romanblack_rss_alert_no_internet,
                            Toast.LENGTH_LONG).show();
                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                        }
                    }, 5000);
                }
                break;
                case COLORS_RECIEVED: {
                    colorsRecieved();
                }
                break;
                case HIDE_PROGRESS_DIALOG: {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                }
                break;

                case RESET_COLOR: {
                    View v = (View) message.obj;
                    v.setBackgroundColor(Statics.color1);
                    listView.invalidate();
                }
                break;
            }
        }
    };

    @Override
    public void create() {
        try {
            setContentView(R.layout.romanblack_feed_main);
            setTitle(R.string.romanblack_rss_feed);

            mainlLayout = findViewById(R.id.romanblack_feed_main);
            listView = (ListView) findViewById(R.id.romanblack_feedList);

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

            boolean showSideBar = ((Boolean) getIntent().getExtras().getSerializable("showSideBar")).booleanValue();
            if (!showSideBar) {
                setTopBarLeftButtonText(getString(R.string.rss_home_button), true, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        finish();
                    }
                });
            }

            try {
                if (widget.getPluginXmlData().length() == 0) {
                    if (currentIntent.getStringExtra("WidgetFile").length() == 0) {
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

            widgetMD5 = Utils.md5(widget.getPluginXmlData());
            File cacheData = new File(cachePath + "/cache.data");
            if (cacheData.exists() && cacheData.length() > 0) {
                cacheMD5 = readFileToString(cachePath + "/cache.md5").replace("\n", "");
                if (cacheMD5.equals(widgetMD5)) {
                    useCache = true;
                } else {
                    File[] files = cache.listFiles();
                    for (int i = 0; i < files.length; i++) {
                        files[i].delete();
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

            cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm.getActiveNetworkInfo();
            if (ni != null && ni.isConnectedOrConnecting()) {
                isOnline = true;
            }

            if (!isOnline && !useCache) {
                handler.sendEmptyMessage(NEED_INTERNET_CONNECTION);
                return;
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog = ProgressDialog.show(NewsPlugin.this, null, getString(R.string.romanblack_rss_loading), true);
                    progressDialog.setCancelable(true);
                    progressDialog.setOnCancelListener(new OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            handler.sendEmptyMessage(LOADING_ABORTED);
                        }
                    });
                }
            });


            new Thread() {
                @Override
                public void run() {
                    timer = new Timer("EventsTimer", true);
                    EntityParser parser;

                    if (widget.getPluginXmlData().length() > 0) {
                        parser = new EntityParser(widget.getPluginXmlData());

                    } else {
                        String xmlData = readXmlFromFile(currentIntent.getStringExtra("WidgetFile"));
                        parser = new EntityParser(xmlData);

                    }

                    parser.parse();

                    Statics.color1 = parser.getColor1();
                    Statics.color2 = parser.getColor2();
                    Statics.color3 = parser.getColor3();
                    Statics.color4 = parser.getColor4();
                    Statics.color5 = parser.getColor5();

                    handler.sendEmptyMessage(COLORS_RECIEVED);

                    title = (widget.getTitle().length() > 0) ? widget.getTitle() : parser.getFuncName();
                    items = parser.getItems();

                    if ("rss".equals(parser.getFeedType())) {
                        Statics.isRSS = true;
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
                                }

                            }
                        } else {
                            try {
                                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(cachePath + "/cache.data"));
                                items = (ArrayList<FeedItem>) ois.readObject();
                                ois.close();
                            } catch (Exception e) {
                            }
                        }
                    }else{
                        Statics.isRSS = false;
                    }

                    for (int i = 0; i < items.size(); i++) {
                        items.get(i).setTextColor(widget.getTextColor());
                        items.get(i).setDateFormat(widget.getDateFormat());
                    }

                    funcName = parser.getFuncName();
                    selectShowType();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            boolean needBlackSeparator =
                                    Statics.color1 == Color.WHITE ||
                                    Statics.color1 == Color.parseColor("#c2e793") || //green
                                    Statics.color1 == Color.parseColor("#f3ea98"); //yellow
                            listView.setDivider(new ColorDrawable(Color.parseColor(needBlackSeparator ? "#4d000000" : "#4dffffff")));
                            listView.setDividerHeight(1);
                        }
                    });
                }
            }.start();

        } catch (Exception ex) { 
        }
    }

    @Override
    public void resume() {
        super.resume();

        clearItemView();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });

    }

    @Override
    public void restart() {
        super.restart();

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        listView.setCacheColorHint(Color.TRANSPARENT);
        listView.invalidate();
    }

    /**
     * @param menu
     * @return true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    /**
     * This menu contains reverse and refresh buttons
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.romanblack_rss_menu_main, menu);
        menu.clear();

        MenuItem menuItem = menu.add("");
        menuItem.setTitle(getString(R.string.romanblack_rss_reverse));
        menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                handler.sendEmptyMessage(REVERSE_LIST);
                return true;
            }
        });

        if (Statics.isRSS) {
            menuItem = menu.add("");
            menuItem.setTitle(R.string.romanblack_rss_refresh);
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

        return true;
    }

    /**
     * Reverse RSS items and redraw them
     */
    private void reverseItems() {
        Collections.reverse(items);

        progressDialog = ProgressDialog.show(this, null, getString(R.string.romanblack_rss_loading), true);

        selectShowType();
    }

    /**
     * Async loading and parsing RSS feed.
     */
    private void loadRSS() {
        try {

            progressDialog = ProgressDialog.show(this, null, getString(R.string.romanblack_rss_loading), true);

            new Thread() {
                @Override
                public void run() {
                    
                    FeedParser reader = new FeedParser(feedURL);
                    items = reader.parseFeed();

                    if (items.size() > 0) {
                        File cache = new File(cachePath);
                        File[] files = cache.listFiles();
                        for (int i = 0; i < files.length; i++) {
                            if (!files[i].getName().equals("cache.md5")) {
                                files[i].delete();
                            }
                        }

                        try {
                            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(cachePath + "/cache.data"));
                            oos.writeObject(items);
                            oos.flush();
                            oos.close();
                        } catch (Exception e) {
                        }
                    }

                    for (int i = 0; i < items.size(); i++) {
                        items.get(i).setTextColor(widget.getTextColor());
                        items.get(i).setDateFormat(widget.getDateFormat());
                    }

                    selectShowType();
                }
            }.start();

        } catch (Exception ex) { 
        }
    }

    /**
     * Async loading and parsing RSS feed when PullToRefresh header released.
     */
    private void loadRSSOnScroll() {
        if (Statics.isRSS) {
            if (cm != null) {
                NetworkInfo ni = cm.getActiveNetworkInfo();
                if (ni != null && ni.isConnectedOrConnecting()) {
                    isOnline = true;
                    loadRSS();
                } else {
                    isOnline = false;
                }
            }
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
        } else {
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
        }
    }

    /**
     * Shows RSS feed list.
     * Called when func = "rss"
     */
    private void showFeed() {
        try {

            setTitle(title);
            if (items.isEmpty()) {
                return;
            }


            listView.setCacheColorHint(Color.TRANSPARENT);

            FeedAdapter adapter = new FeedAdapter(this, items, widget.getBackgroundColor());
            this.adapter = adapter;
            adapter.setCachePath(cachePath);
            listView.setAdapter(adapter);

            listView.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> arg0, View view, int position, long arg3) {

                    int bgColor = Color.GRAY;
                    float[] hsv = new float[3];
                    Color.colorToHSV(bgColor, hsv);
                    bgColor = Color.HSVToColor(127, hsv);
                    ((ViewGroup) view).getChildAt(0).setBackgroundColor(bgColor);

                    Message msg = handler.obtainMessage(RESET_COLOR, (Object) view);
                    handler.sendMessageDelayed(msg, 300);

                    if (Statics.isRSS) {
                        showDetails(position - 1);
                    } else {
                        showDetails(position);
                    }
                }
            });

            if (Statics.isRSS) {
                ((PullToRefreshListView) listView).setOnRefreshListener(new PullToRefreshListView.OnRefreshListener() {
                    public void onRefresh() {
                        loadRSSOnScroll();
                        ((PullToRefreshListView) listView).onRefreshComplete();
                    }
                });
            } else {
                ((PullToRefreshListView) listView).refreshOff();
            }

            handler.sendEmptyMessage(HIDE_PROGRESS_DIALOG);

        } catch (Exception ex) { 
        }
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

            listView.setBackgroundColor(Color.WHITE);
            try {
                listView.setBackgroundColor(Statics.color1);
            } catch (IllegalArgumentException e) {
            }

            FeedAdapter adapter = new FeedAdapter(this, items, widget.getBackgroundColor());
            adapter.setCachePath(cachePath);
            listView.setAdapter(adapter);

            listView.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> arg0, View view, int position, long arg3) {
                    int bgColor = Color.GRAY;
                    float[] hsv = new float[3];
                    Color.colorToHSV(bgColor, hsv);
                    bgColor = Color.HSVToColor(127, hsv);
                    ((ViewGroup) view).getChildAt(0).setBackgroundColor(bgColor);

                    if (Statics.isRSS) {
                        showDetails(position - 1);
                    } else {
                        showDetails(position);
                    }
                }
            });

            if (Statics.isRSS) {
                ((PullToRefreshListView) listView).setOnRefreshListener(new PullToRefreshListView.OnRefreshListener() {
                    public void onRefresh() {
                        loadRSSOnScroll();
                        ((PullToRefreshListView) listView).onRefreshComplete();
                    }
                });
            } else {
                ((PullToRefreshListView) listView).refreshOff();
            }

            handler.sendEmptyMessage(HIDE_PROGRESS_DIALOG);

        } catch (Exception ex) { 
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

            listView.setCacheColorHint(Color.TRANSPARENT);
            listView.setBackgroundColor(Color.WHITE);

            try {
                listView.setBackgroundColor(Statics.color1);
            } catch (IllegalArgumentException e) {
            }

            EventsAdapter adapter = new EventsAdapter(this, R.layout.romanblack_events_item, items, widget.getBackgroundColor());
            this.adapter = adapter;
            listView.setAdapter(adapter);

            listView.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> arg0, View view, int position, long arg3) {
                    int bgColor = Color.GRAY;
                    float[] hsv = new float[3];
                    Color.colorToHSV(bgColor, hsv);
                    bgColor = Color.HSVToColor(127, hsv);
                    view.setBackgroundColor(bgColor);

                    if (Statics.isRSS) {
                        showDetails(position - 1);
                    } else {
                        showDetails(position);
                    }
                }
            });


            if (Statics.isRSS) {
                ((PullToRefreshListView) listView).setOnRefreshListener(new PullToRefreshListView.OnRefreshListener() {
                    public void onRefresh() {
                        loadRSSOnScroll();
                        ((PullToRefreshListView) listView).onRefreshComplete();
                    }
                });
            } else {
                ((PullToRefreshListView) listView).refreshOff();
            }

            if (widget.hasParameter("add_local_notific")) {
                // show alert dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getString(R.string.romanblack_rss_dialog_add_notofications));
                builder.setPositiveButton(getString(R.string.romanblack_rss_yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        handler.sendEmptyMessage(ADD_NOTIFICATIONS);
                    }
                });
                builder.setNegativeButton(getString(R.string.romanblack_rss_no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                    }
                });
                builder.create().show();
            }


            handler.sendEmptyMessage(HIDE_PROGRESS_DIALOG);
        } catch (Exception ex) { 
        }
    }

    /**
     * Go to item details page.
     *
     * @param position list item position
     */
    private void showDetails(int position) {
        try {

            if (items.get(position).getMediaType().contains("image") || !items.get(position).hasMedia() || items.get(position).getDescription().length() > 70) {

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
                } else if (items.get(position).getMediaType().contains("audio")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse(items.get(position).getMediaUrl()),
                            "audio/*");
                    startActivity(intent);

                }
            }

        } catch (Exception ex) { 
        }
    }

    /**
     * Restore views color.
     */
    private void clearItemView() {
        int bgColor = Statics.color1;

        for (int i = 0; i < listView.getChildCount(); i++) {
            if (i == 0 && Statics.isRSS) {
            } else {
                View itemView = ((ViewGroup) listView.getChildAt(i)).getChildAt(0);
                itemView.setBackgroundColor(bgColor);
            }
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
        }

        return sb.toString();
    }

    private void closeActivity() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        finish();
    }

    /**
     * Called when module colors was parsed.
     */
    private void colorsRecieved() {
        try {
            mainlLayout.setBackgroundColor(Statics.color1);
        } catch (NullPointerException nPEx) {
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
        Toast.makeText(NewsPlugin.this, getString(R.string.romanblack_rss_alert_notifications_added_first_part) + " " + counter + "  " + getString(R.string.romanblack_rss_alert_notifications_added_seconds_part), Toast.LENGTH_LONG).show();
    }

    /**
     * This function converts data and time represented in minutes in necessary format
     */
    private String convertTimeToFormat(int hh, int mm, boolean format) {
        String temphhstr = null;
        String tempminstr = null;
        if (format) // use 24 format
        {
            // proccessing houres
            if (Integer.toString(hh).length() < 2) {
                temphhstr = Integer.toString(hh);
                temphhstr = "0" + temphhstr;
            } else {
                temphhstr = Integer.toString(hh);
            }

            // proccessing minutes
            if (Integer.toString(mm).length() < 2) {
                tempminstr = Integer.toString(mm);
                tempminstr = "0" + tempminstr;
            } else {
                tempminstr = Integer.toString(mm);
            }

            return temphhstr + ":" + tempminstr;
        } else // use am/pm format
        {
            String am_pm = "";
            int temp_sum = hh * 100 + mm;
            if (temp_sum >= 1200) {
                am_pm = "PM";
            } else {
                am_pm = "AM";
            }

            // processing minutes
            if (Integer.toString(mm).length() < 2) {
                tempminstr = Integer.toString(mm);
                tempminstr = "0" + tempminstr;
            } else {
                tempminstr = Integer.toString(mm);
            }

            // processing hours
            if (hh > 12) {
                int tempHH = hh;
                tempHH = tempHH - 12;

                if (Integer.toString(tempHH).length() < 2) {
                    temphhstr = Integer.toString(tempHH);
                    temphhstr = "0" + temphhstr;
                } else {
                    temphhstr = Integer.toString(tempHH);
                }
            } else {
                if (Integer.toString(hh).length() < 2) {
                    temphhstr = Integer.toString(hh).toString();
                    temphhstr = "0" + temphhstr;
                } else {
                    temphhstr = Integer.toString(hh).toString();
                }
            }

            return temphhstr + ":" + tempminstr + " " + am_pm;

        }
    }
}