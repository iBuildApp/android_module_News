package com.ibuildapp.romanblack.NewsPlugin.utils;


import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.widget.Toast;

import java.util.Date;

public abstract class CalendarUtils {
    public static void addToCalendar(Activity context, Long beginTime, String title) {
        ContentResolver cr = context.getContentResolver();

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
            context.startActivity(intent);
        } else {
            Toast.makeText(context, "Event already exist!", Toast.LENGTH_LONG).show();
        }
    }
}
