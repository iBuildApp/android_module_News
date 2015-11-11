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
import android.sax.*;
import android.util.Log;
import android.util.Xml;
import org.xml.sax.Attributes;

import java.util.ArrayList;

/**
 * Used for parsing module xml data.
 */
public class EntityParser {

    private ArrayList<FeedItem> items = new ArrayList<FeedItem>();
    private String xml = "";
    private String type = "";
    /**
     * One of values: "news" | "rss" | "events"
     */
    private String func = "";
    private String feed = "";
    private String title = "";
    private FeedItem item = null;
    private int color1 = Color.parseColor("#ffffff");// background
    private int color2 = Color.parseColor("#ffffff");// month in left
    private int color3 = Color.parseColor("#000000");// text header
    private int color4 = Color.parseColor("#000000");// text
    private int color5 = Color.parseColor("#000000");// date

    /**
     * Constructs new EntityParser instance.
     * @param xml - module xml data to parse
     */
    EntityParser(String xml) {
        this.xml = xml;
    }

    /**
     * 
     * @return "news" | "rss" | "events" 
     */
    public String getFuncName() {
        return func;
    }

    /**
     * @return news feed type
     */
    public String getFeedType() {
        return type;
    }

    /**
     * @return parsed feed url if it was configured
     */
    public String getFeedUrl() {
        return feed;
    }

    /**
     * @return parsed module title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return parsed items if it was configured
     */
    public ArrayList<FeedItem> getItems() {
        return items;
    }

    /**
     * Parses module data that was set in constructor.
     */
    public void parse() {
        RootElement root = new RootElement("data");

        Element colorSchemeElement = root.getChild("colorskin");

        Element color1Element = colorSchemeElement.getChild("color1");
        color1Element.setEndTextElementListener(new EndTextElementListener() {
            public void end(String arg0) {
                color1 = Color.parseColor(arg0.trim());
            }
        });

        Element color2Element = colorSchemeElement.getChild("color2");
        color2Element.setEndTextElementListener(new EndTextElementListener() {
            public void end(String arg0) {
                color2 = Color.parseColor(arg0.trim());
            }
        });

        Element color3Element = colorSchemeElement.getChild("color3");
        color3Element.setEndTextElementListener(new EndTextElementListener() {
            public void end(String arg0) {
                color3 = Color.parseColor(arg0.trim());
            }
        });

        Element color4Element = colorSchemeElement.getChild("color4");
        color4Element.setEndTextElementListener(new EndTextElementListener() {
            public void end(String arg0) {
                color4 = Color.parseColor(arg0.trim());
            }
        });

        Element color5Element = colorSchemeElement.getChild("color5");
        color5Element.setEndTextElementListener(new EndTextElementListener() {
            public void end(String arg0) {
                color5 = Color.parseColor(arg0.trim());
            }
        });

        android.sax.Element title = root.getChild("title");

        android.sax.Element rss = root.getChild("rss");
        android.sax.Element news = root.getChild("news");
        android.sax.Element event = root.getChild("event");

        root.setEndElementListener(new EndElementListener() {
            @Override
            public void end() {
            }
        });

        title.setEndTextElementListener(new EndTextElementListener() {
            @Override
            public void end(String body) {
                EntityParser.this.title = body;
            }
        });

        event.setStartElementListener(new StartElementListener() {
            @Override
            public void start(Attributes attributes) {
                func = "events";
                item = new FeedItem();
            }
        });

        event.setEndElementListener(new EndElementListener() {
            @Override
            public void end() {
                items.add(item);
                item = null;
            }
        });

        event.getChild("title").setEndTextElementListener(new EndTextElementListener() {
            @Override
            public void end(String body) {
                if (item != null) {
                    item.setTitle(body);
                }
            }
        });

        event.getChild("date").setEndTextElementListener(new EndTextElementListener() {
            @Override
            public void end(String body) {
                if (item != null) {
                    item.setPubdate(body);
                }
            }
        });

        event.getChild("description").setEndTextElementListener(new EndTextElementListener() {
            @Override
            public void end(String body) {
                if (item != null) {
                    //body = body.replaceAll("\n", "<br/>");
                    item.setDescription(body, true);
                }
            }
        });

        news.setStartElementListener(new StartElementListener() {
            @Override
            public void start(Attributes attributes) {
                func = "news";
                item = new FeedItem();
            }
        });

        news.setEndElementListener(new EndElementListener() {
            @Override
            public void end() {
                items.add(item);
                item = null;
            }
        });

        news.getChild("title").setEndTextElementListener(new EndTextElementListener() {
            @Override
            public void end(String body) {
                if (item != null) {
                    item.setTitle(body);
                }
            }
        });

        news.getChild("indextext").setEndTextElementListener(new EndTextElementListener() {
            @Override
            public void end(String body) {
                if (item != null) {
                    item.setIndextext(body);
                }
            }
        });

        news.getChild("date").setEndTextElementListener(new EndTextElementListener() {
            @Override
            public void end(String body) {
                if (item != null) {
                    item.setPubdate(body);
                }
            }
        });

        news.getChild("url").setEndTextElementListener(new EndTextElementListener() {
            @Override
            public void end(String body) {
                if (item != null) {
                    item.setImageUrl(body);
                }
            }
        });

        news.getChild("description").setEndTextElementListener(new EndTextElementListener() {
            @Override
            public void end(String body) {
                if (item != null) {
                    item.setDescription(body, true);
                }
            }
        });

        rss.setStartElementListener(new StartElementListener() {
            @Override
            public void start(Attributes attributes) {
                func = "rss";
                type = "rss";
                if (attributes.getValue("type") != null) {
                    func = attributes.getValue("type");
                }
            }
        });

        rss.setEndElementListener(new EndElementListener() {
            @Override
            public void end() {
            }
        });

        rss.setEndTextElementListener(new EndTextElementListener() {
            @Override
            public void end(String body) {
                feed = body;
            }
        });

        String xmlString = xml;//Utils.inputStreamToString(new ByteArrayInputStream(xml.getBytes()));
        xmlString = xmlString.replace((char) 11, ' ');

        try {
            Xml.parse(xmlString, root.getContentHandler());
        } catch (Exception e) {
            Log.e("PARSER", "", e);
        }
    }

    /**
     * @return parsed color 1 of color scheme
     */
    public int getColor1() {
        return color1;
    }

    /**
     * @return parsed color 2 of color scheme
     */
    public int getColor2() {
        return color2;
    }

    /**
     * @return parsed color 3 of color scheme
     */
    public int getColor3() {
        return color3;
    }

    /**
     * @return parsed color 4 of color scheme
     */
    public int getColor4() {
        return color4;
    }

    /**
     * @return parsed color 5 of color scheme
     */
    public int getColor5() {
        return color5;
    }
}
