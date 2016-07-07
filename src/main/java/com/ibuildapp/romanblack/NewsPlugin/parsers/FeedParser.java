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
package com.ibuildapp.romanblack.NewsPlugin.parsers;

import android.util.Log;
import android.util.Xml;

import com.ibuildapp.romanblack.NewsPlugin.model.FeedItem;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * This class used for RSS feed parsing.
 */
public class FeedParser {

    private URL feedUrl = null;
    private String url = null;

    private String encoding = "";

    /**
     * Constructs new FeedParser with given RSS feed url.
     * @param url RSS feed url
     */
    public FeedParser(String url){
        try {
            if ("http://ibuildapp.com/feed/".equals(url)) {
                url = url + "?no_redirect";
            }

            url = url.replaceAll(" ", "%20");
            feedUrl = new URL(url);
            this.url = url;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Parse RSS feed with url that was set in constructor
     * @return parsed RSS feed items list
     */
    public ArrayList<FeedItem> parseFeed(){
        FeedHandler handler = new FeedHandler();
        handler.setFeedUrl(url);

        try {
            String xml1 = "";
            String line1 = "";
            StringBuilder s1 =  new StringBuilder();

            InputStream is;

            if(url.contains("facebook.com")){
                HttpURLConnection conn = (HttpURLConnection)feedUrl.openConnection();
                conn.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (iPhone; U; "
                                + "CPU iPhone OS 4_0 like Mac OS X; en-us) AppleWebKit/532.9 "
                                + "(KHTML, like Gecko) Version/4.0.5 Mobile/8A293 "
                                + "Safari/6531.22.7");
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");

                is = conn.getInputStream();
            }else{
                HttpURLConnection conn = (HttpURLConnection)feedUrl.openConnection();
                conn.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Linux; <Android Version>; <Build Tag etc.>) " +
                                "AppleWebKit/<WebKit Rev> (KHTML, like Gecko) " +
                                "Chrome/<Chrome Rev> Mobile Safari/<WebKit Rev>");
                //conn.setRequestProperty("User-Agent","Mozilla/5.0 ( compatible ) ");
                //conn.setRequestProperty("Accept","*/*");
                //conn.setDoOutput(true);
                is = conn.getInputStream();
            }

            BufferedReader rd1 = new BufferedReader(new InputStreamReader(is));
            try {
                while ((line1 = rd1.readLine()) != null) {
                    s1.append(line1);
                    s1.append("\n");
                }
                xml1 = s1.toString();
            } catch (Exception e){
            }
            encoding = parseEncoding(xml1);

            String xml = "";
            String line = "";
            StringBuilder s =  new StringBuilder();

            if(url.contains("facebook.com")){
                HttpURLConnection conn = (HttpURLConnection)feedUrl.openConnection();
                conn.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (iPhone; U; "
                                + "CPU iPhone OS 4_0 like Mac OS X; en-us) AppleWebKit/532.9 "
                                + "(KHTML, like Gecko) Version/4.0.5 Mobile/8A293 "
                                + "Safari/6531.22.7");
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");

                is = conn.getInputStream();
            }else{
                HttpURLConnection conn = (HttpURLConnection)feedUrl.openConnection();
                conn.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Linux; <Android Version>; <Build Tag etc.>) " +
                                "AppleWebKit/<WebKit Rev> (KHTML, like Gecko) " +
                                "Chrome/<Chrome Rev> Mobile Safari/<WebKit Rev>");
                // conn.setDoOutput(true);
                is = conn.getInputStream();
            }

            BufferedReader rd = new BufferedReader(new InputStreamReader(is, getEncoding()));
            try {
                while ((line = rd.readLine()) != null) {
                    s.append(line);
                    s.append("\n");
                }
                xml = s.toString();
            } catch (Exception e){
            }

            encoding = parseEncoding(xml);
            Xml.parse(xml, handler);
        } catch (Exception e) {
            Log.w("", "");
        }

        for(Iterator<FeedItem> it = handler.getItems().iterator(); it.hasNext();){
            FeedItem item = it.next();
            item.setEncoding(getEncoding());
        }
        return handler.getItems();
    }

    /**
     * Parses RSS feed encoding
     * @param xml RSS feed
     * @return encoding
     */
    private String parseEncoding(String xml){
        String enc = "UTF-8";

        if(xml.contains("ISO-8859-1")){
            enc = "ISO-8859-1";
        }else if(xml.contains("US-ASCII")){
            enc = "US-ASCII";
        }else if(xml.contains("UTF-16")){
            enc = "UTF-16";
        }else if(xml.contains("windows-1251")){
            enc = "windows-1251";
        }else if(xml.contains("windows-1256")){
            enc = "windows-1256";
        }

        return enc;
    }

    /**
     * @return the RSS feed encoding
     */
    public String getEncoding() {
        return encoding;
    }
}
