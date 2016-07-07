package com.ibuildapp.romanblack.NewsPlugin.utils;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Iterator;

public abstract class HtmlUtils {
    /**
     * Prepare text from HTML without HTML tags
     * @param html - convert
     * @return text without html tags
     */
    public static String html2text(String html) {
        return Jsoup.parse(html).text();
    }

    private static String pastImgStyleTag(String pHTML) {
        Document doc = Jsoup.parse(pHTML);

        String styletoPast = "<style>img{display: inline;height: auto;max-width: 100%;}</style>";
        Elements head = doc.select("head");
        Iterator<Element> iterator = head.iterator();
        for (; iterator.hasNext(); ) {
            Element headEl = iterator.next();
            headEl.append(styletoPast);
        }
        return doc.html();
    }
}
