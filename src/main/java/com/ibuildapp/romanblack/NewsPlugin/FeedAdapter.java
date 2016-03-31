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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.appbuilder.sdk.android.Utils;
import org.apache.http.util.ByteArrayBuffer;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Adapter for news list.
 * Using if func = rss or func = news.
 */
public class FeedAdapter extends BaseAdapter {

    private ArrayList<FeedItem> items;
    private LayoutInflater layoutInflater;
    private int imageWidth = 75;
    private int imageHeight = 75;
    private String cachePath = "";
    private HashMap<Integer, Bitmap> bitmaps = new HashMap<Integer, Bitmap>();
    private ImageDownloadTask downloadTask = null;

    /**
     * Constructs new FeedAdapter instance
     * @param context - Activity that using this adapter
     * @param list - event items list
     * @param bgColor
     */
    FeedAdapter(Context context, ArrayList<FeedItem> list, int bgColor) {
        items = list;
        layoutInflater = LayoutInflater.from(context);

        downloadTask = new ImageDownloadTask();
        downloadTask.execute(items);
    }

    /**
     * Set disk cache path to store downloaded rss images.
     * @param cachePath - disk cache path
     */
    public void setCachePath(String cachePath) {
        this.cachePath = cachePath;
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(cachePath + "/cache.data"));
            oos.writeObject(items);
            oos.flush();
            oos.close();
        } catch (Exception e) {
        }
    }

    /**
     * Set rss image size to show in list
     * @param width
     * @param height
     */
    public void setImageSize(int width, int height) {
        imageWidth = width;
        imageHeight = height;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public FeedItem getItem(int index) {
        return items.get(index);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    /**
     * 2 view types
     * @return 2
     */
    @Override
    public int getViewTypeCount() {
        return 2;
    }

    /**
     * @param position - rss item position in list
     * @return 1 - if rss item contains image, 0 - otherwise
     */
    @Override
    public int getItemViewType(int position) {
        if (items.get(position).hasImage()) {
            return 0;
        } else {
            return 1;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = null;

        if (null == convertView) {
        } else {
            row = convertView;
        }

        if (row == null) {
            if (items.get(position).hasImage()) {
                row = layoutInflater.inflate(R.layout.romanblack_feed_item_image, null);
                ImageView imageView = (ImageView) row.findViewById(R.id.romanblack_rss_image);
                imageView.setImageResource(R.drawable.romanblack_rss_no_image);
            } else {
                row = layoutInflater.inflate(R.layout.romanblack_feed_item, null);
            }
        }

        TextView title = (TextView) row.findViewById(R.id.romanblack_rss_title);
        title.setTextColor(Statics.color3);
        TextView description = (TextView) row.findViewById(R.id.romanblack_rss_description);
        description.setTextColor(Statics.color4);
        TextView pubdate = (TextView) row.findViewById(R.id.romanblack_rss_pubdate);
        pubdate.setTextColor(Statics.color4);

        String titleString = items.get(position).getTitle();
        title.setText(titleString);


        String descString = items.get(position).getAnounce(70);
        description.setText(descString);

        pubdate.setText(items.get(position).getPubdate(""));

        if (items.get(position).hasImage()) {
            ImageView imageView = (ImageView) row.findViewById(R.id.romanblack_rss_image);
            if (imageView != null) {
                if (items.get(position).getImagePath().length() > 0) {
                    Bitmap bitmap = null;
                    Integer key = new Integer(position);
                    if (bitmaps.containsKey(key)) {
                        bitmap = bitmaps.get(key);
                    } else {
                        try {
                            bitmap = decodeImageFile(items.get(position).getImagePath());
                            bitmaps.put(key, bitmap);
                        } catch (Exception e) {
                            Log.w("NEWS ADAPTER", e);
                        }
                    }

                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                    }
                }
            }
        }

        ((ViewGroup) row).getChildAt(0).setBackgroundColor(Statics.color1);;
        ImageView imageView = (ImageView) row.findViewById(R.id.news_arrow);
        if (Statics.BackColorToFontColor(Statics.color1) == Color.BLACK) {
            imageView.setImageResource(R.drawable.news_arrow_light);
        } else {
            imageView.setImageResource(R.drawable.news_arrow);
        }

        return row;
    }

    private void viewUpdated() {
        this.notifyDataSetChanged();
    }

    private void downloadRegistration(int position, String value) {
        this.items.get(position).setImagePath(value);
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(cachePath + "/cache.data"));
            oos.writeObject(items);
            oos.flush();
            oos.close();
            Log.d("IMAGES PLUGIN CACHE DATA", "SUCCESS");
        } catch (Exception e) {
            Log.w("IMAGES PLUGIN CACHE DATA", e);
        }
    }

    /**
     * Refreshes rss list if image was downloaded.
     */
    private void downloadComplete() {
        this.notifyDataSetChanged();
    }

    /**
     * Decode rss item image that is storing in given file.
     * @param imagePath - image file path
     * @return decoded image Bitmap
     */
    private Bitmap decodeImageFile(String imagePath) {
        try {
            File file = new File(imagePath);
            //Decode image size
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(file), null, opts);

            //Find the correct scale value. It should be the power of 2.
            int width = opts.outWidth, height = opts.outHeight;
            int scale = 1;
            while (true) {
                if (width / 2 < imageWidth || height / 2 < imageHeight) {
                    break;
                }
                width /= 2;
                height /= 2;
                scale *= 2;
            }

            //Decode with inSampleSize
            opts = new BitmapFactory.Options();
            opts.inSampleSize = scale;
            opts.inPreferredConfig = Bitmap.Config.RGB_565;

            Bitmap bitmap = null;
            try {
                // decode image with appropriate options
                try {
                    System.gc();
                    bitmap = BitmapFactory.decodeStream(new FileInputStream(file), null, opts);
                } catch (Exception ex) {
                    Log.d("", "");
                } catch (OutOfMemoryError e) {
                    Log.d("", "");
                    System.gc();
                    try {
                        bitmap = BitmapFactory.decodeStream(new FileInputStream(file), null, opts);
                    } catch (Exception ex) {
                        Log.d("", "");
                    } catch (OutOfMemoryError ex) {
                        Log.e("decodeImageFile", "OutOfMemoryError");
                    }
                }
            } catch (Exception e) {
                Log.d("", "");
                return null;
            }

            int x = 0, y = 0, l = 0;
            if (width > height) {
                x = (int) (width - height) / 2;
                y = 0;
                l = height;
            } else {
                x = 0;
                y = (int) (height - width) / 2;
                l = width;
            }

            float matrixScale = (float) (imageWidth - 4) / (float) l;
            Matrix matrix = new Matrix();
            matrix.postScale(matrixScale, matrixScale);

            Bitmap result = Bitmap.createBitmap(bitmap, x, y, l, l, matrix, true);
            bitmap.recycle();
            return result;

        } catch (Exception e) {
            Log.w("IMAGE TRANSFORMATION", e);
        }

        return null;
    }

    /**
     * This class creates a background thread to download images of RSS items.
     */
    private class ImageDownloadTask extends AsyncTask<ArrayList<FeedItem>, String, Void> {

        @Override
        protected Void doInBackground(ArrayList<FeedItem>... items) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 4;

            for (int i = 0; i < items[0].size(); i++) {
                if (isCancelled()) {
                    return null;
                }

                if (items[0].get(i).getImageUrl().length() == 0) {
                    continue;
                }

                if (items[0].get(i).getImagePath().length() > 0) {
                    File file = new File(items[0].get(i).getImagePath());
                    if (file.exists()) {
                        continue;
                    }
                }

                String filename = cachePath + "/" + Utils.md5(items[0].get(i).getImageUrl());//System.currentTimeMillis();
                File imgFile = new File(filename);
                if (imgFile.exists()) {
                    items[0].get(i).setImagePath(filename);
                    publishProgress();
                    continue;
                }

                boolean imageLoaded = false;

                try {
                    URL imageUrl = new URL(items[0].get(i).getImageUrl());
                    BufferedInputStream bis = new BufferedInputStream(imageUrl.openConnection().getInputStream());
                    ByteArrayBuffer baf = new ByteArrayBuffer(32);
                    int current = 0;
                    while ((current = bis.read()) != -1) {
                        baf.append((byte) current);
                    }
                    FileOutputStream fos = new FileOutputStream(new File(filename));
                    fos.write(baf.toByteArray());
                    fos.flush();
                    fos.close();

                    // set downloaded file path to item
                    downloadRegistration(i, filename);

                    imageLoaded = true;
                } catch (Exception e) {
                    Log.e("IMAGE ADAPTER", "An error has occurred downloading the image: " + items[0].get(i).getImageUrl() + " " + e);
                    imageLoaded = false;
                }

                if (!imageLoaded) {
                    try {
                        URL imageUrl = new URL(items[0].get(i).getImageUrlAlt());
                        BufferedInputStream bis = new BufferedInputStream(imageUrl.openConnection().getInputStream());
                        ByteArrayBuffer baf = new ByteArrayBuffer(32);
                        int current = 0;
                        while ((current = bis.read()) != -1) {
                            baf.append((byte) current);
                        }
                        FileOutputStream fos = new FileOutputStream(new File(filename));
                        fos.write(baf.toByteArray());
                        fos.close();

                        downloadRegistration(i, filename);
                    } catch (Exception e) {
                        Log.e("", "");
                    }
                }

                publishProgress();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(String... param) {
            viewUpdated();
        }

        @Override
        protected void onPostExecute(Void unused) {
            downloadComplete();
        }
    }
}