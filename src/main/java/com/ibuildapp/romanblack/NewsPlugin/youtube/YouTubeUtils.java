package com.ibuildapp.romanblack.NewsPlugin.youtube;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YouTubeUtils {
    private static final String pattern = "(?<=watch\\?v=|/videos/|embed\\/)[^#\\&\\?]*";
    public static String getVideoId(String youTubeUrl){
        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(youTubeUrl);

        if(matcher.find()){
            return matcher.group();
        }
        else return "";
    }
}