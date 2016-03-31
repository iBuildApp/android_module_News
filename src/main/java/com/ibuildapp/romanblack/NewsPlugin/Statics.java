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

import android.graphics.Color;

/**
 * This class contains global module variables.
 */
public class Statics {

    static boolean isRSS = false;

    /* Color Scheme */
    static int color1 = Color.parseColor("#ffffff");// background
    static int color2 = Color.parseColor("#ffffff");// month in left date
    static int color3 = Color.parseColor("#000000");// text header 
    static int color4 = Color.parseColor("#000000");// text
    static int color5 = Color.parseColor("#000000");// date
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
}
