package com.ibuildapp.romanblack.NewsPlugin.adapters;


import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bumptech.glide.DrawableTypeRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.ibuildapp.romanblack.NewsPlugin.NewsPlugin;
import com.ibuildapp.romanblack.NewsPlugin.R;
import com.ibuildapp.romanblack.NewsPlugin.model.FeedItem;
import com.ibuildapp.romanblack.NewsPlugin.viewholders.RssViewHolder;

import java.util.List;


public class RssAdapter extends RecyclerView.Adapter<RssViewHolder>{

    private NewsPlugin context;
    private List<FeedItem> list;
    private float displayWidth;
    private float density;

    public RssAdapter(NewsPlugin context, List<FeedItem> list) {
        this.context = context;
        this.list = list;

        displayWidth = context.getResources().getDisplayMetrics().widthPixels;
        density = context.getResources().getDisplayMetrics().density;
    }

    @Override
    public RssViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.news_main_rss_item, parent, false);
        return new RssViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final RssViewHolder holder, int position) {
        final FeedItem currentItem = list.get(position);
        holder.title.setText(currentItem.getTitle());
        holder.date.setText(currentItem.getPubdate(""));

        if (currentItem.hasImage()){
            holder.image.setVisibility(View.INVISIBLE);
            DrawableTypeRequest<String> request = Glide.with(context).load(currentItem.getImageUrl());
            if (currentItem.getImageUrl().contains(".gif")) {
                request.asGif().override(200, 200).listener(new RequestListener<String, GifDrawable>() {
                    @Override
                    public boolean onException(Exception e, String model, Target<GifDrawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GifDrawable resource, String model, Target<GifDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        holder.image.setVisibility(View.VISIBLE);
                        return false;
                    }
                }).into(holder.image);

            } else request.asBitmap().into(new SimpleTarget<Bitmap>() {
                 @Override
                 public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                     postUpdateHolder(holder, resource);
                 }
             });
        }else holder.image.setVisibility(View.GONE);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                context.startRssDetails(holder.getAdapterPosition());
            }
        });
    }

    public void postUpdateHolder(RssViewHolder holder, Bitmap resource){
        holder.image.setVisibility(View.VISIBLE);

        if (resource.getWidth() < ( displayWidth / 2)) {
            LinearLayout.LayoutParams newParams = new LinearLayout.LayoutParams( (int)(100*density), (int) (100*density));
            newParams.setMargins((int) (10*density),(int) (10*density),(int) (15*density), 0);
            newParams.gravity = Gravity.LEFT;

            holder.image.setLayoutParams(newParams);
            holder.image.setScaleType(ImageView.ScaleType.CENTER_CROP);

            ((LinearLayout.LayoutParams)holder.inverseLayout.getLayoutParams()).setMargins(0, (int) (10*density), 0, 0 );
            holder.inverseLayout.setOrientation(LinearLayout.HORIZONTAL);
        }else  {
           /* int bitmapHeight = resource.getHeight();
            int bitmapWidth  =resource.getWidth();

            int cardViewWidth = holder.cardView.getWidth();
            int imageViewHeight = (bitmapHeight * cardViewWidth)/bitmapWidth;

            System.out.print(bitmapHeight * bitmapWidth);
*/
            LinearLayout.LayoutParams newParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            newParams.setMargins(0,0,0,0);

            holder.image.setLayoutParams(newParams);
            holder.image.setScaleType(ImageView.ScaleType.CENTER_CROP);

            ((LinearLayout.LayoutParams)holder.inverseLayout.getLayoutParams()).setMargins(0, 0, 0, 0);
            holder.inverseLayout.setOrientation(LinearLayout.VERTICAL);
        }

        holder.image.setImageBitmap(resource);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

   @Override
    public void onViewRecycled(RssViewHolder holder) {
        Glide.clear(holder.image);
    }
}
