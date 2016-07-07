package com.ibuildapp.romanblack.NewsPlugin.viewholders;


import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ibuildapp.romanblack.NewsPlugin.R;

public class RssViewHolder extends RecyclerView.ViewHolder {

    public final LinearLayout inverseLayout;
    public final ImageView image;
    public final TextView title;
    public final TextView date;
    public final CardView cardView;

    public RssViewHolder(View itemView) {
        super(itemView);

        this.inverseLayout = (LinearLayout) itemView.findViewById(R.id.news_main_rss_item_layout);
        this.image = (ImageView) itemView.findViewById(R.id.news_main_rss_item_image);
        this.title = (TextView) itemView.findViewById(R.id.news_main_rss_item_title);
        this.date = (TextView) itemView.findViewById(R.id.news_main_rss_item_date);
        this.cardView = (CardView) itemView.findViewById(R.id.news_main_rss_item_card);
    }
}
