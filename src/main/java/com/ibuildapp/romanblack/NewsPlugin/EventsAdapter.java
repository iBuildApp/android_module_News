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

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Adapter for events list.
 * Using if func = events only.
 */
public class EventsAdapter extends BaseAdapter {

    private static final String TAG = "com.ibuildapp.newsplugin.eventsadapter";
    private ArrayList<FeedItem> items;
    private LayoutInflater layoutInflater;

    /**
     * Constructs new EventsAdapter instance
     * @param context - Activity that using this adapter
     * @param resource
     * @param list - event items list
     * @param bgColor
     */
    EventsAdapter(Context context, int resource, ArrayList<FeedItem> list, int bgColor) {
        super();
        items = list;
        layoutInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int i) {
        return items.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row;

        if (null == convertView) {
            row = layoutInflater.inflate(R.layout.romanblack_events_item, null);
        } else {
            row = convertView;
        }

        try {
            TextView title = (TextView) row.findViewById(R.id.romanblack_rss_title);
            title.setTextColor(Statics.color3);

            TextView description = (TextView) row.findViewById(R.id.romanblack_rss_description);
            description.setTextColor(Statics.color4);

            TextView date = (TextView) row.findViewById(R.id.romanblack_rss_date);
            date.setTextColor(Statics.color4);

            if (Locale.getDefault().toString().equals("ru_RU")) {
                date.setText(items.get(position).getPubdate("EEE, d MMM yyyy HH:mm"));
            } else {
                date.setText(items.get(position).getPubdate("EEE, d MMM yyyy hh:mm a"));
            }

            TextView day = (TextView) row.findViewById(R.id.romanblack_rss_day);

            TextView month = (TextView) row.findViewById(R.id.romanblack_rss_month);
            month.setTextColor(Statics.color2);

            title.setText(items.get(position).getTitle());
            description.setText(items.get(position).getAnounce(75));
            day.setText(items.get(position).getPubdate("dd"));
            month.setText(items.get(position).getPubdate("MMM").toUpperCase());

            row.setBackgroundColor(Statics.color1);
        } catch (Exception e) {
            Log.d(TAG, "", e);
        }
        return row;
    }
}