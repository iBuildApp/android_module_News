package com.ibuildapp.romanblack.NewsPlugin.viewholders;


import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ibuildapp.romanblack.NewsPlugin.R;
import com.ibuildapp.romanblack.NewsPlugin.utils.Statics;

public class EventsViewHolder extends RecyclerView.ViewHolder{
    public final TextView title;
    public final TextView description;
    public final TextView date;
    public final TextView day;
    public final TextView month;
    public final LinearLayout mainLayout;

    public EventsViewHolder(View itemView) {
        super(itemView);

        mainLayout = (LinearLayout) itemView.findViewById(R.id.news_main_events_item);

        title = (TextView) itemView.findViewById(R.id.news_main_events_item_title);
        title.setTextColor(Statics.color3);

        description = (TextView) itemView.findViewById(R.id.news_main_events_item_description);
        description.setTextColor(Statics.color4);

        date = (TextView) itemView.findViewById(R.id.news_main_events_item_date);
        date.setTextColor(Statics.color4);

        day = (TextView) itemView.findViewById(R.id.news_main_events_item_day);
        month = (TextView) itemView.findViewById(R.id.news_main_events_item_month);

        itemView.setBackgroundColor(Statics.color1);
    }

}
