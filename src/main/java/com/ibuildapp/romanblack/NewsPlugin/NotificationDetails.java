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
import android.content.DialogInterface;
import android.content.Intent;
import android.net.http.SslError;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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
        setContentView(R.layout.news_notification_details);

        titleText = (TextView) findViewById(R.id.news_notification_details_title);
        dateText = (TextView) findViewById(R.id.news_notification_details_date);
        descriptionText = (WebView) findViewById(R.id.news_notification_details_description);
        descriptionText.setWebViewClient(new WebViewClient(){
            @Override
            public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(NotificationDetails.this);
                builder.setMessage(R.string.news_notification_error_ssl_cert_invalid);
                builder.setPositiveButton(NotificationDetails.this.getResources().getString(R.string.news_on_continue), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        handler.proceed();
                    }
                });
                builder.setNegativeButton(NotificationDetails.this.getResources().getString(R.string.news_on_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        handler.cancel();
                    }
                });
                final AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
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
