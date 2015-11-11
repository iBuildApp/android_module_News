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

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Activity that opens when user pressed event notification 
 */
public class EventsNotificationView extends Activity implements OnTouchListener {

    private int position = 0;
    float startPos = 0;
    private ArrayList<String> notifications = new ArrayList<String>();
    private TextView textViewNotification = null;
    private TextView textViewCounter = null;
    private LinearLayout notificationPanel = null;
    private ImageView btnNext = null;
    private ImageView btnPrev = null;
    final private int SLIDE_TO_RIGHT_START = 0;
    final private int SLIDE_TO_LEFT_START = 1;
    final private int SLIDE_COMPLETE = 2;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case SLIDE_TO_LEFT_START: {
                    if (position < notifications.size() - 1) {
                        position++;
                        slidePanel(-500);
                    }
                }
                break;
                case SLIDE_TO_RIGHT_START: {
                    if (position > 0) {
                        position--;
                        slidePanel(500);
                    }
                }
                break;
                case SLIDE_COMPLETE: {
                    showNotification();
                }
                break;
            };
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.romanblack_rss_notification_screen);

        String path = Environment.getExternalStorageDirectory() + "/AppBuilder/" + getPackageName() + "/.notifications";
        File file = new File(path);
        if (file.exists()) {
            try {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
                notifications = (ArrayList<String>) ois.readObject();
                ois.close();
            } catch (Exception e) {
            }
            file.delete();

            textViewNotification = (TextView) findViewById(R.id.romanblack_rss_notification);
            textViewCounter = (TextView) findViewById(R.id.romanblack_rss_notification_counter);
            RelativeLayout notificationMain = (RelativeLayout) findViewById(R.id.romanblack_rss_notification_main);
            notificationPanel = (LinearLayout) findViewById(R.id.romanblack_rss_notification_panel);

            Button btnClose = (Button) findViewById(R.id.romanblack_rss_push_button_close);
            btnClose.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    closeMessage();
                }
            });

            Button btnApp = (Button) findViewById(R.id.romanblack_rss_button_app);
            btnApp.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                }
            });
            btnApp.setVisibility(View.GONE);

            btnNext = (ImageView) findViewById(R.id.romanblack_rss_notification_next);
            btnNext.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    handler.sendEmptyMessage(SLIDE_TO_LEFT_START);
                }
            });

            btnPrev = (ImageView) findViewById(R.id.romanblack_rss_notification_prev);
            btnPrev.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    handler.sendEmptyMessage(SLIDE_TO_RIGHT_START);
                }
            });


            textViewNotification.setText("");
            textViewCounter.setText("");
            if (notifications.size() > 0) {
                textViewCounter.setText((position + 1) + " " + getString(R.string.romanblack_rss_from) + " " + notifications.size());
                textViewNotification.setText(notifications.get(position));

                if (notifications.size() > 1) {
                    btnNext.setVisibility(View.VISIBLE);
                }
            }
            notificationMain.setOnTouchListener(this);
        } else {
            finish();
        }
    }

    /**
     * Detect left or right sliding
     * @return true
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                startPos = event.getX();
            }
            break;
            case MotionEvent.ACTION_MOVE: {
            }
            break;
            case MotionEvent.ACTION_UP: {
                if (event.getX() - startPos > 90) {
                    handler.sendEmptyMessage(SLIDE_TO_RIGHT_START);
                } else if (startPos - event.getX() > 90) {
                    handler.sendEmptyMessage(SLIDE_TO_LEFT_START);
                }
            }
            break;
        }
        return true;
    }

    /**
     * Show slide animation after slide detected
     * @param pos - Change in X coordinate to apply at the end of the animation
     */
    private void slidePanel(int pos) {
        TranslateAnimation slideToRight = new TranslateAnimation(0, pos, 0, 0);
        slideToRight.setFillAfter(true);
        slideToRight.setInterpolator(new LinearInterpolator());
        slideToRight.setDuration(500);
        slideToRight.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                handler.sendEmptyMessage(SLIDE_COMPLETE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationStart(Animation animation) {
            }
        });
        notificationPanel.startAnimation(slideToRight);
    }

    /**
     * Fill up text fields to represend notification data
     */
    private void showNotification() {
        if (notifications.size() > 1) {
            if (position > 0) {
                btnPrev.setVisibility(View.VISIBLE);
            } else {
                btnPrev.setVisibility(View.INVISIBLE);
            }
            if (position < notifications.size() - 1) {
                btnNext.setVisibility(View.VISIBLE);
            } else {
                btnNext.setVisibility(View.INVISIBLE);
            }
        }

        textViewNotification.setText(notifications.get(position));
        textViewCounter.setText((position + 1) + " " + getString(R.string.romanblack_rss_from) + " " + notifications.size());
        notificationPanel.clearAnimation();
        notificationPanel.refreshDrawableState();
    }

    private void closeMessage() {
        finish();
    }
}
