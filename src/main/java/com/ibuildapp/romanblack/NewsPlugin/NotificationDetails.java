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

import android.content.Intent;
import android.webkit.WebView;
import android.widget.TextView;
import com.appbuilder.sdk.android.AppBuilderModule;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class called to represent event notification details
 */
public class NotificationDetails extends AppBuilderModule {

    private TextView titleText;
    private TextView dateText;
    private WebView descriptionText;

    @Override
    public void create() {
        setContentView(R.layout.romanblack_notification_details);

        titleText = (TextView) findViewById(R.id.romanblack_feed_title);
        dateText = (TextView) findViewById(R.id.romanblack_feed_date);
        descriptionText = (WebView) findViewById(R.id.romanblack_feed_description);

        Intent parent = getIntent();
        String title = parent.getStringExtra("TITLE");
        String date = parent.getStringExtra("DATE");
        String description = parent.getStringExtra("DESCRIPTION");
        String utf = null;
        try {
            utf = new String(description.getBytes(), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(NotificationDetails.class.getName()).log(Level.SEVERE, null, ex);
        }

        titleText.setText(title);
        dateText.setText(date);
        descriptionText.loadDataWithBaseURL("fake://not/needed", utf, "text/html", "utf-8", "");
    }
}
