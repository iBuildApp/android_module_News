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
package com.ibuildapp.romanblack.NewsPlugin.model;

import android.graphics.Color;
import android.text.Html;
import android.util.Log;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Entity class that represents item in RSS list.
 */
public class FeedItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Flag that shows that the image ling was handled from description
     */
    private boolean imageFromDescription = false;

    private String title = "";

    private String description = "";
    private String anounce = "";
    private Date pubdate = null;
    private String imageUrl = "";
    private String imageUrlAlt = "";
    private String imagePath = "";
    private String indextext = "";
    private int color = Color.DKGRAY;
    private int dateformat = 0;
    private String link = "";
    private String encoding = "";

    private String mediaUrl = "";
    private String mediaType = "";

    private String feedUrl = "";

    public FeedItem() {
    }

    /**
     * Sets the title of feed item.
     *
     * @param value title
     */
    public void setTitle(String value) {
        value = value.trim();
        title = value.replaceAll("[\\n\\t]", "");
    }

    /**
     * Returns a string title of feed item.
     *
     * @return title of item
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the description of feed item.
     * @param value description to set 
     */
    synchronized public void setDescription(String value, boolean replace) {
        if (replace) value = value.replaceAll("[\\n\\t]", "<br/>");
        value = value.trim();

        Document doc = Jsoup.parse(value);
        if ((imageUrl.length() == 0) || imageFromDescription) {
            Element img = doc.select("img").first();
            if (img != null) {
                setImageUrl(img.attr("src"));
                imageFromDescription = true;
            }

            if (imageUrl == null) {
                imageUrl = "";
                imageFromDescription = false;
            }
        }

        //anounce = doc.text();
        try {
            description = value;
        } catch (Exception e) {
            description = e.getMessage();
        }

        // cut all <img> tags
        String temp = description.replaceAll("\\<img.*?>", "");
        anounce = Html.fromHtml(temp).toString().trim();
    }

    /**
     * Returns a string description of feed item.
     *
     * @return description of item
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return true if description contains image links, false otherwise
     */
    public boolean isDescriptionContainsImages() {
        Document doc = Jsoup.parse(description);

        Elements elems = doc.select("img");

        if (elems.isEmpty()) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Returns the short description of item.
     * @param length announce lenght
     * @return 
     */
    public String getAnounce(int length) {
        /*if (length == 0) {
            return anounce;
        } else {
            if (length > 30) {
                length -= 3;
            }
            return (anounce.length() > length) ? anounce.substring(0, length) + "..." : anounce;
        }*/
        return anounce;
    }

    /**
     * Sets pubdate of feed item.
     * @param value date string
     * @param pattern date sreing pattern
     */
    synchronized public void setPubdate(String value, String pattern) {
        value = value.replaceAll("[\\n\\t]", "");
        value = value.trim();
        if (value.contains("+")) {
            value = value.substring(0, value.lastIndexOf("+") - 1);
        }

        SimpleDateFormat sdf;
        sdf = new SimpleDateFormat(pattern, Locale.ENGLISH);


        try {
            if (getPubdate() == null) {
                pubdate = sdf.parse(value);

            }
        } catch (Exception e) {
        }
    }

    /**
     * Sets pubdate of feed item.
     * If you have date string patter use {@code setPubdate(String, String)} instead.
     * @param value date string
     * @return 
     */
    synchronized public String setPubdate(String value) {
        value = value.replaceAll("[\\n\\t]", "");
        value = value.trim();
        if (value.contains("+")) {
            value = value.substring(0, value.lastIndexOf("+") - 1);
        }

        String[] patterns = {
                "dd MMM yyyy hh:mm:ss",
                "dd MMM yyyy hh:mm:ss zzzzz",
                "yyyy.MM.dd G 'at' HH:mm:ss z",
                "EEE, MMM d, ''yy",
                "yyyyy.MMMMM.dd GGG hh:mm aaa",
                "EEE, d MMM yyyy HH:mm:ss Z",
                "yyMMddHHmmssZ",
                "d MMM yyyy HH:mm:ss z",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ssZ",
                "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                "yyyy-MM-dd'T'HH:mm:ssz",
                "yyyy-MM-dd'T'HH:mm:ss.SSSz",
                "EEE, d MMM yy HH:mm:ssz",
                "EEE, d MMM yy HH:mm:ss",
                "EEE, d MMM yy HH:mm z",
                "EEE, d MMM yy HH:mm Z",
                "EEE, d MMM yyyy HH:mm:ss z",
                "EEE, d MMM yyyy HH:mm:ss Z",
                "EEE, d MMM yyyy HH:mm:ss ZZZZ",
                "EEE, d MMM yyyy HH:mm z",
                "EEE, d MMM yyyy HH:mm Z",
                "d MMM yy HH:mm z",
                "d MMM yy HH:mm:ss z",
                "d MMM yyyy HH:mm z",
                "d MMM yyyy HH:mm:ss z"};

        for (int i = 0; i < patterns.length; i++) {
            SimpleDateFormat sdf = new SimpleDateFormat(patterns[i], Locale.ENGLISH);
            try {
                if (getPubdate() == null) {
                    pubdate = sdf.parse(value);
                    return patterns[i];
                }
            } catch (Exception e) {
            }
        }

        return "";
    }

    /**
     * Returns publication date of feed item with given format.
     * @param format
     * @return 
     */
    public String getPubdate(String format) {
        if (getPubdate() == null) {
            return "";
        } else {
            if (format.length() == 0) {
                StringBuilder result = new StringBuilder();
                Log.d("FeedItem", "Locale = " + Locale.getDefault());
                if (dateformat == 1) {
                    SimpleDateFormat sdf;

                    /*if (Locale.getDefault().toString().equals("en_US")) {
                        sdf = new SimpleDateFormat("MMM dd yyyy hh:mm a");
                    } else {*/
                        sdf = new SimpleDateFormat("dd MMM yyyy HH:mm");
                    //}
                    result.append(sdf.format(pubdate));
                } else {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy hh:mm a");

                    //if (Locale.getDefault().toString().equals("en_US")) {
                        sdf = new SimpleDateFormat("MMM dd yyyy hh:mm a");
                    /*} else {
                        sdf = new SimpleDateFormat("dd MMM yyyy HH:mm");
                    }*/
                    result.append(sdf.format(pubdate));
                }
                return result.toString();
            }
            
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            try {
                return sdf.format(getPubdate());
            } catch (Exception e) {
                return "";
            }
        }
    }

    /**
     * @return true if this feed item has image, false otherwise
     */
    public boolean hasImage() {
        return (imageUrl.length() > 0 || imagePath.length() > 0) ? true : false;
    }

    /**
     * Sets the image url to this feed item.
     * @param value image url to set
     */
    synchronized public void setImageUrl(String value) {
        if (value != null) {
            if (!value.equals("")) {
                try {
                    URI feedUri = new URI(feedUrl);
                    URI imageUri = new URI(value);

                    if (imageUri.getHost() != null) {
                        if (imageUri.getHost().equals("")) {
                            Log.e("", "");
                        }
                    }

                    try {
                        imageUrlAlt = "http://" + feedUri.getHost() + value;
                    } catch (Exception e) {
                    }

                    if (imageUri.getScheme() == null) {
                        if (value.startsWith("://")) {
                            value = "http" + value;
                        } else if (value.startsWith("//")) {
                            value = "http:" + value;
                        } else if (value.startsWith("/")) {
                            value = "http:/" + value;
                        } else {
                            value = "http://" + value;
                        }
                    }

                    imageUrl = value;
                    Log.e("", "");
                } catch (URISyntaxException uSEx) {
                    imageUrl = value;
                }
            }
        }
    }

    /**
     * Returns the image url of this feed item.
     * @return image url
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * Sets the path of downloaded image.
     * @param value path of downloaded image
     */
    synchronized public void setImagePath(String value) {
        imagePath = value;
    }

    /**
     * Returns the path of downloadad image.
     * @return image path
     */
    public String getImagePath() {
        return imagePath;
    }

    /**
     * Sets the index text to this feed item.
     * @param value index text to set
     */
    synchronized public void setIndextext(String value) {
        indextext = value.replaceAll("[\\n\\t]", "");
    }

    /**
     * @return the index text
     */
    public String getIndextext() {
        return indextext;
    }

    /**
     * Sets feed item text color.
     * @param color text color to set
     */
    synchronized public void setTextColor(int color) {
        if (color != Color.TRANSPARENT) {
            this.color = color;
        }
    }

    /**
     * Returns feed item text color.
     * @return feed item text color
     */
    public int getTextColor() {
        return color;
    }

    /**
     * Sets feed item date format.
     * @param value date format to set 1 - 24 hours format, 0 - 12 hours format
     */
    synchronized public void setDateFormat(int value) {
        //dateformat = (value != 0 || value != 1) ? 0 : 1;
        switch(value){
            case 0:
            case 1:
                dateformat = value;
                break;
            default:
                dateformat = 0;
        }
    }

    /**
     * Sets link.
     * @param value link to set 
     */
    synchronized public void setLink(String value) {
        link = value.replaceAll("[\\n\\t]", "");
    }

    /**
     * Returns link.
     * @return link
     */
    public String getLink() {
        return link;
    }

    /**
     * Adds media url to this feed item.
     * @param mediaUrl the mediaUrl to set
     */
    public void addMediaItem(String mediaUrl, String mediaType) {
        if (validUrl(mediaUrl)) {
            this.mediaUrl = mediaUrl;
            this.mediaType = mediaType;
        }
    }

    /**
     * Returns true if this item contains media urls, false otherwise.
     * @return true if this item contains media urls, false otherwise
     */
    public boolean hasMedia() {
        boolean has = false;

        if (getMediaUrl() != null && getMediaType() != null) {
            if ((getMediaUrl().length() > 0) && (getMediaType().length() > 0)) {
                has = true;
            }
        }

        return has;
    }

    /**
     * Check if resource URL is valid.
     * @param url URL to check
     * @return true if URL is valid
     */
    private boolean validUrl(String url) {
        boolean valid = false;

        Pattern pattern = Pattern.compile("^(http:\\/\\/|https:\\/\\/"
                + ")?([^\\.\\/]+\\.)*([a-zA-Z0-9])([a-zA-Z0-9-]*)\\.("
                + "[a-zA-Z]{2,4})(\\/.*)?$", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(url);
        if (matcher.matches()) {
            valid = true;
        }

        return valid;
    }

    /**
     * @return the mediaUrl
     */
    public String getMediaUrl() {
        return mediaUrl;
    }

    /**
     * @return the mediaType
     */
    public String getMediaType() {
        return mediaType;
    }

    /**
     * @return the encoding
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * @param encoding the encoding to set
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * @return the pubdate
     */
    public Date getPubdate() {
        return pubdate;
    }

    /**
     * @return true if image URL was parsed from description, false otherwise
     */
    public boolean isImageFromDescription() {
        return imageFromDescription;
    }

    /**
     * Returns the alternative image URL.
     * @return alrenative image URL 
     */
    public String getImageUrlAlt() {
        return imageUrlAlt;
    }

    /**
     * Sets RSS feed URL.
     * @param feedUrl RSS feed URL
     */
    public void setFeedUrl(String feedUrl) {
        this.feedUrl = feedUrl;
    }


    public String getFeedUrl() {
        return feedUrl;
    }
}


