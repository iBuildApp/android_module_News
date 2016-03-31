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

import android.text.Html;
import android.text.TextUtils;
import com.appbuilder.sdk.android.Utils;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Sax handler that handle XML configuration tags and prepare module data
 * structure.
 */
public class FeedHandler extends DefaultHandler {

    private ArrayList<FeedItem> items = new ArrayList<FeedItem>();
    private FeedItem item = null;
    private StringBuilder builder;
    private String datePattern = "";
    private String feedUrl = "";
    private boolean isMedia = false;
    private boolean hasLink = false;
    private boolean wasGoogleDate = false;

    /**
     * Returns the parsed RSS feed items array.
     *
     * @return the RSS feed items array
     */
    public ArrayList<FeedItem> getItems() {
        return items;
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (this.item != null) {
            builder.append(ch, start, length);
        }
    }

    @Override
    public void endDocument() throws SAXException {
    }

    @Override
    public void startDocument() throws SAXException {
        this.items = new ArrayList<FeedItem>();
        this.builder = new StringBuilder();
    }

    @Override
    public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
        if (localName.equalsIgnoreCase("content")
                && uri.equalsIgnoreCase("http://search.yahoo.com/mrss/")) {
            if (item != null) {
                try {
                    if (!attributes.getValue("url").equals("")) {
                        item.addMediaItem(attributes.getValue("url"),
                                attributes.getValue("type"));
                    }
                } catch (NullPointerException nPEx) {
                }

                try {
                    if (attributes.getValue("medium").equalsIgnoreCase("image")) {
                        if (!item.hasImage()) {
                            item.setImageUrl(attributes.getValue("url"));
                        }
                    }
                } catch (NullPointerException nPEx) {
                }
            }
        } else if (localName.equalsIgnoreCase("when")
                && uri.equalsIgnoreCase("http://schemas.google.com/g/2005")) {
            if (item != null) {
                if (!attributes.getValue("startTime").equalsIgnoreCase("")) {
                    item.setPubdate(attributes.getValue("startTime"));
                    wasGoogleDate = true;
                }
            }
        } else if (localName.equalsIgnoreCase("enclosure")) {
            if (item != null) {
                if (attributes.getValue("url") != null) {
                    if (!attributes.getValue("url").equals("") && !item.hasMedia()) {
                        item.addMediaItem(attributes.getValue("url"),
                                attributes.getValue("type"));
                    }
                }
            }
        } else if (localName.equalsIgnoreCase("player")
                && uri.equalsIgnoreCase("http://search.yahoo.com/mrss/")) {
            if (item != null) {
                if (!attributes.getValue("url").equals("")
                        && item.getMediaUrl().equals("")) {
                    item.addMediaItem(attributes.getValue("url"),
                            item.getMediaType());
                }
            }
        } else if (localName.equalsIgnoreCase("ITEM") || localName.equalsIgnoreCase("ENTRY")) {
            this.item = new FeedItem();
            item.setFeedUrl(feedUrl);
        }
        isMedia = (uri.indexOf("mrss") == -1) ? false : true;

        if (isMedia && localName.equalsIgnoreCase("thumbnail")) {
            if (this.item != null) {
                String url = attributes.getValue("url");
                if (url.length() > 0) {
                    item.setImageUrl(url);
                }
            }
        }
        if (this.item != null) {
            if (localName.equalsIgnoreCase("LINK")) {
                String rel = attributes.getValue("rel");
                if (rel == null || rel.equalsIgnoreCase("ALTERNATE") || rel.length() == 0) {
                    hasLink = true;
                    String lnk = attributes.getValue("href");
                    if (lnk != null) {
                        if (lnk.length() != 0) {
                            this.item.setLink(lnk);
                        }
                    }
                }
                if (rel == null || rel.equalsIgnoreCase("IMAGE") || rel.length() == 0) {
                    this.item.setImageUrl(attributes.getValue("href"));
                }
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String name) throws SAXException {

        if (this.item != null) {
            if (uri.equalsIgnoreCase("http://www.w3.org/2005/Atom")
                    || uri.equalsIgnoreCase("http://purl.org/rss/1.0/")) {
                uri = "";
            }

            if (localName.equalsIgnoreCase("TITLE")) {
                if (TextUtils.isEmpty(uri)) // ignori if it has any prefix (namespace) <title>
                {
                    this.item.setTitle(/*Utils.removeSpec(*/Html.fromHtml(builder.toString()).toString()/*)*/);
                }
            } else if (localName.equalsIgnoreCase("description")) {
                this.item.setDescription(builder.toString(), false);
            } else if (localName.equalsIgnoreCase("CONTENT")) {
                if (TextUtils.isEmpty(this.item.getDescription())) {
                    this.item.setDescription(builder.toString(), false);
                }
            } else if (localName.equalsIgnoreCase("SUMMARY")) {
                if (TextUtils.isEmpty(this.item.getDescription())) {
                    this.item.setDescription(builder.toString(), false);
                }
            } else if (localName.equalsIgnoreCase("PUBDATE")) {
                if (!wasGoogleDate) {
                    if (datePattern.length() == 0) {
                        datePattern = this.item.setPubdate(builder.toString());
                    } else {
                        this.item.setPubdate(builder.toString(), datePattern);
                    }
                }
            } else if (localName.equalsIgnoreCase("UPDATED")) {
                if (!wasGoogleDate) {
                    if (datePattern.length() == 0) {
                        datePattern = this.item.setPubdate(builder.toString());
                    } else {
                        this.item.setPubdate(builder.toString(), datePattern);
                    }
                }
            } else if (localName.equalsIgnoreCase("LINK")) {
                if (hasLink) {
                    if (builder.toString().trim().length() != 0) {
                        this.item.setLink(builder.toString().trim());
                    }
                }
            } else if (localName.equalsIgnoreCase("ITEM") || localName.equalsIgnoreCase("ENTRY")) {
                this.items.add(item);
                item = null;
            } else if (localName.equalsIgnoreCase("date")
                    && uri.equalsIgnoreCase("http://purl.org/dc/elements/1.1/")) {
                if ("".equals(this.item.getPubdate(""))) {
                    if (datePattern.length() == 0) {
                        datePattern = this.item.setPubdate(builder.toString());
                    } else {
                        this.item.setPubdate(builder.toString(), datePattern);
                    }
                }
            } else if (localName.equalsIgnoreCase("encoded")
                    && uri.equalsIgnoreCase("http://purl.org/rss/1.0/modules/content/")) {
                this.item.setDescription(builder.toString(), false);
            }
            this.builder.setLength(0);
        }
    }

    /**
     * Sets the RSS feed URL.
     *
     * @param feedUrl the feed URL to set
     */
    public void setFeedUrl(String feedUrl) {
        this.feedUrl = feedUrl;
    }
}
