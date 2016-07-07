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
package com.ibuildapp.romanblack.NewsPlugin.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;

/**
 * This class contains global module variables.
 */
public class Statics {

    public static boolean isRSS = false;

    /* Color Scheme */
    public static int color1 = Color.parseColor("#ffffff");// background
    public static int color2 = Color.parseColor("#ffffff");// month in left date
    public static int color3 = Color.parseColor("#000000");// text header
    public static int color4 = Color.parseColor("#000000");// text
    public static int color5 = Color.parseColor("#000000");// date
    /* Color Scheme ends */

    public static int BackColorToFontColor(int backColor) {
        int r = (backColor >> 16) & 0xFF;
        int g = (backColor >> 8) & 0xFF;
        int b = (backColor >> 0) & 0xFF;

        double Y = (0.299 * r + 0.587 * g + 0.114 * b);
        if (Y > 127) {
            return Color.BLACK;
        } else {
            return Color.WHITE;
        }
    }

    public static Bitmap applyColorFilterForResource(Context context, int resourceId, int color, PorterDuff.Mode mode ){
        Bitmap immutable = BitmapFactory.decodeResource(context.getResources(), resourceId);
        final Bitmap mutable = immutable.copy(Bitmap.Config.ARGB_8888, true);
        Canvas c = new Canvas(mutable);
        Paint p = new Paint();
        p.setColorFilter(new PorterDuffColorFilter(color, mode));
        c.drawBitmap(mutable, 0.f, 0.f, p);
        return mutable;
    }
}
