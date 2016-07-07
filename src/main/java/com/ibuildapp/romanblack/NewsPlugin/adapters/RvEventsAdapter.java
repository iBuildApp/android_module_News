package com.ibuildapp.romanblack.NewsPlugin.adapters;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import com.ibuildapp.romanblack.NewsPlugin.NewsPlugin;
import com.ibuildapp.romanblack.NewsPlugin.R;
import com.ibuildapp.romanblack.NewsPlugin.model.FeedItem;
import com.ibuildapp.romanblack.NewsPlugin.viewholders.EventsViewHolder;

import java.util.List;
import java.util.Locale;


public class RvEventsAdapter extends RecyclerView.Adapter<EventsViewHolder> {

    private NewsPlugin context;
    private List<FeedItem> list;

    private int lastAnimatedPosition = -1;
    private boolean animationsLocked = false;

    public RvEventsAdapter(NewsPlugin context, List<FeedItem> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public EventsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.news_main_events_item, parent, false);
        return new EventsViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final EventsViewHolder holder, final int position) {
        runEnterAnimation(holder.itemView, position);
        FeedItem item = list.get(position);

        if (Locale.getDefault().toString().equals("ru_RU")) {
            holder.date.setText(item.getPubdate("EEE, d MMM yyyy HH:mm"));
        } else {
            holder.date.setText(item.getPubdate("EEE, d MMM yyyy hh:mm a"));
        }

        holder.title.setText(item.getTitle());
        holder.description.setText(item.getAnounce(75));
        holder.day.setText(item.getPubdate("dd"));
        holder.month.setText(item.getPubdate("MMM").toUpperCase());

        holder.mainLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int bgColor = Color.GRAY;
                float[] hsv = new float[3];
                Color.colorToHSV(bgColor, hsv);
                bgColor = Color.HSVToColor(127, hsv);
                holder.mainLayout.setBackgroundColor(bgColor);

                context.startEventDetails(holder.getAdapterPosition());

                holder.mainLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        holder.mainLayout.setBackgroundDrawable(null);
                    }
                }, 100);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private void runEnterAnimation(View view, int position) {
        if (animationsLocked) return;

        if (android.os.Build.VERSION.SDK_INT <  12)
            return;

        int height = context.getResources().getDisplayMetrics().heightPixels;
        if (position > lastAnimatedPosition) {
            lastAnimatedPosition = position;

            view.setTranslationY(height);

            view.animate()
                    .translationY(0)
                    .setStartDelay(300 + 100 * (position))
                    .setDuration(500)
                    .setInterpolator(new DecelerateInterpolator(2.f))
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            animationsLocked = true;
                        }
                    })
                    .start();
        }
    }
}
