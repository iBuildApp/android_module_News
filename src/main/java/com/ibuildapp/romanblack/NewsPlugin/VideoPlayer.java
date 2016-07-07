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

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.*;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.appbuilder.sdk.android.AppBuilderModuleMainAppCompat;
import com.appbuilder.sdk.android.Utils;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * This class used to play video if RSS item contains media video link
 */
public class VideoPlayer extends AppBuilderModuleMainAppCompat implements SurfaceHolder.Callback {

    private String videoUrl = "";
    private String cachePath = "";
    private final int sourceCache = 1;
    private final int sourceUrl = 2;
    private int sourceType = sourceUrl;
    private final int statePlay = 1;
    private final int statePause = 2;
    private int state = statePause;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private MediaPlayer mediaPlayer;
    private AudioManager audioManager = null;
    private int audioVolume = 0;
    private int videoCurrentPos = 0;

    private SeekBar seekBar = null;
    private View playButton;
    private View pauseButton;

    private View playLayout;
    private View controlsLayout;
    private TextView durationPositiveTextView;
    private TextView durationNegativeTextView;

    private int btnActive = 0;
    private TelephonyManager telephonyManager = null;
    private boolean isTouchSeekBar = false;
    private ProgressDialog progressDialog;
    private long downloadFileSize = 0;
    final private int VIDEO_PLAYER_ERROR = 0;

    final private int VIDEO_PLAYER_START = 2;
    final private int DOWNLOAD_START = 10;
    final private int DOWNLOAD_UPDATE = 11;
    final private int DOWNLOAD_COMPLETE = 5;
    final private int DOWNLOAD_ERROR = 6;
    final private int UPDATE_SEEK_BAR = 7;
    final private int SHOW_CONTROLS = 8;
    final private int HIDE_CONTROLS = 9;
    final private int CHECK_CONTROLS_STATE = 12;
    final private int UPDATE_CONTROLS_STATE = 13;
    final private int LOADING_ABORTED = 14;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case VIDEO_PLAYER_START: {
                    startVideoPlayer();
                }
                break;
                case VIDEO_PLAYER_ERROR: {
                    Toast.makeText(VideoPlayer.this, "This Video Link Is Not Valid.", Toast.LENGTH_LONG).show();
                    File file = new File(cachePath);
                    if (file.exists()) {
                        file.delete();
                    }
                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                            finish();
                        }
                    }, 3000);
                }
                break;
                case DOWNLOAD_START: {
                    openProgressDialog();
                }
                break;
                case DOWNLOAD_UPDATE: {
                    updateProgressDialog();
                }
                break;
                case DOWNLOAD_COMPLETE: {
                    File file = new File(cachePath);
                    if (file.exists()) {
                        startVideoPlayer();
                    } else {
                        this.sendEmptyMessage(VIDEO_PLAYER_ERROR);
                    }
                }
                break;
                case DOWNLOAD_ERROR: {
                    this.sendEmptyMessage(VIDEO_PLAYER_ERROR);
                }
                break;
                case UPDATE_SEEK_BAR: {
                    updateSeekBar();
                }
                break;
                case SHOW_CONTROLS: {
                    showControls();
                }
                break;
                case HIDE_CONTROLS: {
                    hideControls();
                }
                break;
                case CHECK_CONTROLS_STATE: {
                    if (state == statePlay) {
                        checkControlsState();
                    }
                }
                break;
                case UPDATE_CONTROLS_STATE: {
                    btnActive = 3;
                }
                break;
                case LOADING_ABORTED: {
                    closeProgressDialog();
                    File file = new File(cachePath);
                    if (file.exists()) {
                        file.delete();
                    }
                    finish();
                }
                break;
            }
        }
    };

    @Override
    public void create() {

        try {//ErrorLogging
            setContentView(R.layout.news_details_video_player);

            hideTopBar();

            Intent currentIntent = getIntent();
            Bundle store = currentIntent.getExtras();
            videoUrl = store.getString("link");
            cachePath = store.getString("cache") + "/video";

            state = statePause;
            videoCurrentPos = 0;

            telephonyManager = (TelephonyManager) getApplicationContext()
                    .getSystemService(Context.TELEPHONY_SERVICE);
            telephonyManager.listen(new PhoneStateListener() {
                @Override
                public void onCallStateChanged(int state, String incomingNumber) {
                    switch (state) {
                        case TelephonyManager.CALL_STATE_IDLE:
                            break;
                        case TelephonyManager.CALL_STATE_OFFHOOK:
                            Log.d("DEBUG", "OFFHOOK");
                            break;
                        case TelephonyManager.CALL_STATE_RINGING:
                            if (VideoPlayer.this.state == statePlay) {
                                playButton.setVisibility(View.VISIBLE);
                                pauseButton.setVisibility(View.INVISIBLE);
                                mediaPlayer.pause();
                                VideoPlayer.this.state = statePause;
                            }
                            Log.d("DEBUG", "RINGING");
                            break;
                    }
                }
            }, PhoneStateListener.LISTEN_CALL_STATE);

            File cache = new File(cachePath);
            if (!cache.exists())
                cache.mkdirs();

            cachePath += "/" + Utils.md5(videoUrl) + videoUrl.substring(videoUrl.lastIndexOf("."));

            surfaceView = (SurfaceView) findViewById(R.id.news_details_video_view);
            surfaceHolder = surfaceView.getHolder();
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            surfaceView.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    handler.sendEmptyMessage(SHOW_CONTROLS);
                    handler.sendEmptyMessage(UPDATE_CONTROLS_STATE);
                    handler.sendEmptyMessageDelayed(CHECK_CONTROLS_STATE, 300);
                    return false;
                }
            });

            audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            mediaPlayer.setOnErrorListener(new OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    if (sourceType == sourceUrl) {
                        sourceType = sourceCache;
                        downloadFile();
                    } else
                        handler.sendEmptyMessage(VIDEO_PLAYER_ERROR);

                    return true;
                }
            });

            mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    videoCurrentPos = 0;
                    state = statePause;
                    playButton.setVisibility(View.VISIBLE);
                    pauseButton.setVisibility(View.INVISIBLE);

                    mp.seekTo(videoCurrentPos);
                    handler.sendEmptyMessage(SHOW_CONTROLS);
                }
            });

            mediaPlayer.setOnPreparedListener(new OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    closeProgressDialog();

                    int videoWidth = mp.getVideoWidth();
                    int videoHeight = mp.getVideoHeight();
                    int screenWidth = getWindowManager().getDefaultDisplay().getWidth();
                    int screenHeight = getWindowManager().getDefaultDisplay().getHeight();
                    int playerWidth = videoWidth;
                    int playerHeight = videoHeight;

                    if (videoWidth > videoHeight) {
                        playerWidth = screenWidth;
                        playerHeight = (int) (((float) screenWidth / (float) videoWidth) * (float) videoHeight);
                        if (playerHeight > screenHeight) {
                            playerHeight = screenHeight;
                            playerWidth = (int) (((float) screenHeight / (float) videoHeight) * (float) videoWidth);
                        }
                    } else {
                        playerHeight = screenHeight;
                        playerWidth = (int) (((float) screenHeight / (float) videoHeight) * (float) videoWidth);
                        if (playerWidth > screenWidth) {
                            playerWidth = screenWidth;
                            playerHeight = (int) (((float) screenWidth / (float) videoWidth) * (float) videoHeight);
                        }
                    }

                    android.view.ViewGroup.LayoutParams lp = surfaceView.getLayoutParams();
                    lp.width = playerWidth;
                    lp.height = playerHeight;
                    surfaceView.setLayoutParams(lp);

                    if (videoCurrentPos > 0)
                        handler.sendEmptyMessage(UPDATE_SEEK_BAR);

                    if (state == statePlay) {
                        btnActive = 5;
                        handler.sendEmptyMessage(CHECK_CONTROLS_STATE);
                    }
                    mp.seekTo(videoCurrentPos);
                }
            });

            mediaPlayer.setOnSeekCompleteListener(new OnSeekCompleteListener() {
                @Override
                public void onSeekComplete(MediaPlayer mp) {
                    audioVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    if (state == statePause)
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
                    mp.start();
                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioVolume, 0);
                            try {
                                if (state == statePause)
                                    mediaPlayer.pause();

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }, 256);
                    if (seekBar.getVisibility() == View.INVISIBLE) {
                        seekBar.setVisibility(View.VISIBLE);
                    }

                    handler.sendEmptyMessage(UPDATE_CONTROLS_STATE);
                }
            });

            seekBar = (SeekBar) findViewById(R.id.news_details_video_seek_bar);
            seekBar.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    btnActive = 100;
                    isTouchSeekBar = true;
                    videoCurrentPos = (mediaPlayer.getDuration() / 100) * seekBar.getProgress();
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        mediaPlayer.seekTo(videoCurrentPos);
                        isTouchSeekBar = false;
                        btnActive = 3;
                    }
                    return false;
                }
            });
            seekBar.setVisibility(View.INVISIBLE);

            playButton = findViewById(R.id.news_details_video_play);
            pauseButton = findViewById(R.id.news_details_video_pause);
            playLayout = findViewById(R.id.news_details_video_play_layout);

            durationPositiveTextView = (TextView) findViewById(R.id.news_details_video_duration);
            durationNegativeTextView = (TextView) findViewById(R.id.news_details_video_negative_duration);

            controlsLayout = findViewById(R.id.news_details_video_controls_layout);

            if (state == statePlay) {
                playButton.setVisibility(View.INVISIBLE);
                pauseButton.setVisibility(View.VISIBLE);
            }
            playLayout.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (state == statePlay) {
                        playButton.setVisibility(View.VISIBLE);
                        pauseButton.setVisibility(View.INVISIBLE);
                        mediaPlayer.pause();
                        state = statePause;
                    } else if (state == statePause) {
                        playButton.setVisibility(View.INVISIBLE);
                        pauseButton.setVisibility(View.VISIBLE);
                        mediaPlayer.start();
                        state = statePlay;
                        handler.sendEmptyMessage(CHECK_CONTROLS_STATE);
                        handler.sendEmptyMessage(UPDATE_SEEK_BAR);
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void destroy() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mediaPlayer.setDisplay(surfaceHolder);
        handler.sendEmptyMessage(VIDEO_PLAYER_START);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    /**
     * Check if need to hide player controls.
     */
    private void checkControlsState() {
        if (btnActive > 0) {
            btnActive--;
            handler.sendEmptyMessageDelayed(CHECK_CONTROLS_STATE, 1000);
        } else
            handler.sendEmptyMessageDelayed(HIDE_CONTROLS, 1000);
    }

    /**
     * Hides player controls.
     */
    private void hideControls() {
        controlsLayout.setVisibility(View.INVISIBLE);
        playLayout.setVisibility(View.INVISIBLE);
    }

    /**
     * Shows player controls.
     */
    private void showControls() {
        controlsLayout.setVisibility(View.VISIBLE);
        playLayout.setVisibility(View.VISIBLE);
    }

    /**
     * Updates SeekBer state.
     */
    private void updateSeekBar() {
        if (mediaPlayer != null) {
            try {
                if (state == statePlay && !isTouchSeekBar) {
                    seekBar.setProgress((int) (((float) mediaPlayer.getCurrentPosition() / mediaPlayer.getDuration()) * 100));
                }
                if (state == statePause && !isTouchSeekBar) {
                    seekBar.setProgress((int) (((float) mediaPlayer.getCurrentPosition() / mediaPlayer.getDuration()) * 100));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            int duration = mediaPlayer.getDuration();
            int posit = mediaPlayer.getCurrentPosition();

            Log.d("", "");

            SimpleDateFormat sdf = new SimpleDateFormat("mm:ss", Locale.getDefault());
            durationPositiveTextView.setText(sdf.format(new Date(posit)));
            durationNegativeTextView.setText(sdf.format(new Date(duration - posit)));
            if (state == statePlay) {
                handler.sendEmptyMessageDelayed(UPDATE_SEEK_BAR, 300);
            }
        }
    }

    /* progress dialog methods */
    private void openProgressDialog() {
        if (downloadFileSize > 0) {
            try {
                progressDialog = new ProgressDialog(this);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setMessage(getString(R.string.news_loading));
                progressDialog.setProgress(0);
                progressDialog.show();
                handler.sendEmptyMessageDelayed(DOWNLOAD_UPDATE, 500);
            } catch (Exception ex) {
            }
        } else {
            progressDialog = ProgressDialog.show(this, "", getString(R.string.news_loading), true);
        }
        progressDialog.setCancelable(true);
        progressDialog.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                handler.sendEmptyMessage(LOADING_ABORTED);
            }
        });
    }

    private void updateProgressDialog() {
        if (progressDialog != null) {

            checkInternetConnection();

            File file = new File(cachePath);
            int progress = progressDialog.getProgress();
            if (file.exists()) {
                progress = (int) ((float) file.length() / (float) downloadFileSize * 100);
            }
            progressDialog.setProgress(progress);
            if (progress == 100) {
            } else {
                handler.sendEmptyMessageDelayed(DOWNLOAD_UPDATE, 3000);
            }
        }
    }

    private void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    /**
     * Prepares to play MediaPlayer and start it.
     */
    private void startVideoPlayer() {
        try {

            boolean waitForDownload = false;
            boolean hasCache = false;
            File file = new File(cachePath);
            if (file.exists()) {
                hasCache = true;
                try {
                    URL url = new URL(videoUrl);
                    URLConnection conn = url.openConnection();
                    downloadFileSize = conn.getContentLength();
                    sourceType = sourceCache;

                    if (file.length() < downloadFileSize)
                        waitForDownload = true;

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                sourceType = sourceUrl;
            }

            if (waitForDownload) {
                closeProgressDialog();
                handler.sendEmptyMessage(DOWNLOAD_START);
            } else {
                if (mediaPlayer != null) {
                    if (!hasCache) {
                        openProgressDialog();
                    }

                    new Thread() {
                        public void run() {
                            String sourcePath = "";
                            if (sourceType == sourceUrl) {
                                sourcePath = videoUrl;
                            } else if (sourceType == sourceCache) {
                                sourcePath = cachePath;
                            }
                            try {
                                mediaPlayer.reset();
                                mediaPlayer.setDataSource(sourcePath);
                                mediaPlayer.prepare();
                            } catch (Exception e) {
                                closeProgressDialog();
                                if (sourceType == sourceUrl) {
                                    sourceType = sourceCache;
                                    downloadFile();
                                }
                            }
                        }
                    }.start();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Check if device has network connection.
     * @return true if device has connection, false otherwise 
     */
    private boolean checkInternetConnection() {
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        boolean isOnline = false;
        if (ni != null && ni.isConnectedOrConnecting()) {
            isOnline = true;
        }

        if (!isOnline) {
            Toast.makeText(this, R.string.news_alert_no_internet, Toast.LENGTH_LONG).show();
        }
        return isOnline;
    }

    /**
     * Downloads file to devise external storage.
     */
    private void downloadFile() {
        try {//ErrorLogging

            if (!checkInternetConnection()) {
                handler.sendEmptyMessage(DOWNLOAD_ERROR);
            }
            try {
                URL url = new URL(videoUrl);
                URLConnection conn = url.openConnection();
                downloadFileSize = conn.getContentLength();
                String contentType = conn.getContentType();
                if (!contentType.contains("mpeg")
                        && !contentType.contains("mp4")
                        && !contentType.contains("3gpp")
                        && !contentType.contains("H263")
                        && !contentType.contains("H264")) {

                    handler.sendEmptyMessage(DOWNLOAD_ERROR);
                    return;
                }

                if (downloadFileSize <= 0) {
                    handler.sendEmptyMessage(DOWNLOAD_ERROR);
                    return;
                }

                StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
                double sdAvailSize = (double) stat.getAvailableBlocks() * (double) stat.getBlockSize();
                if (downloadFileSize * 0.5 > sdAvailSize) {
                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                            handler.sendEmptyMessage(DOWNLOAD_ERROR);
                        }
                    }, 5000);
                    return;
                }
            } catch (Exception e) {
                handler.sendEmptyMessage(DOWNLOAD_ERROR);
                return; // swallow a 404
            }

            new Thread() {
                public void run() {
                    try {
                        URL url = new URL(videoUrl);
                        URLConnection conn = url.openConnection();
                        File file = new File(cachePath);
                        handler.sendEmptyMessage(DOWNLOAD_START);

                        if (file.exists()) {
                            if (file.length() == downloadFileSize) {
                                handler.sendEmptyMessage(DOWNLOAD_COMPLETE);
                            }
                        }

                        BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
                        byte[] baf = new byte[2048];
                        FileOutputStream fos = new FileOutputStream(file);
                        int current = 0;
                        while (current != -1) {
                            fos.write(baf, 0, current);
                            current = bis.read(baf, 0, 2048);
                        }
                        fos.close();
                        bis.close();

                        handler.sendEmptyMessage(DOWNLOAD_COMPLETE);
                    } catch (IOException e) {
                        handler.sendEmptyMessage(DOWNLOAD_ERROR);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}