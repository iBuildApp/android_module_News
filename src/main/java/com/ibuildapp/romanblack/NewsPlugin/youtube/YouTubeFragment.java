package com.ibuildapp.romanblack.NewsPlugin.youtube;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;
import com.ibuildapp.romanblack.NewsPlugin.R;
import com.ibuildapp.romanblack.NewsPlugin.utils.NewsConstants;

public class YouTubeFragment extends Fragment {
    public static final String YOUTUBE_KEY = "AIzaSyBysCzHNvYFGV9e2lTMg6k3faN8BwsDLpA";

    private String url;
    private YouTubePlayerSupportFragment fragment;
    private YouTubePlayer player;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.news_details_youtube, container, false);

        Bundle args = getArguments();
        url =  args.getString(NewsConstants.YOUTUBE_URL);

        initData();
        return rootView;
    }

    private void initData() {
        fragment = YouTubePlayerSupportFragment.newInstance();
        initialize();

        android.support.v4.app.FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.news_details_youtube_container, fragment);
        transaction.commit();
    }

    public void initialize() {
        fragment.initialize(YOUTUBE_KEY, new YouTubePlayer.OnInitializedListener() {
            @Override
            public void onInitializationSuccess(YouTubePlayer.Provider provider, final YouTubePlayer youTubePlayer, boolean b) {
                if (!b) {
                    player = youTubePlayer;
                    youTubePlayer.cueVideo(YouTubeUtils.getVideoId(url));
                }
            }

            @Override
            public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {
                System.out.print("asd");
            }
        });
    }


    public void goToPortrait() {
        player.setFullscreen(false);
    }
}