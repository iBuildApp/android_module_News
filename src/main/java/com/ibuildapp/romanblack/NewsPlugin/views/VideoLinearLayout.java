package com.ibuildapp.romanblack.NewsPlugin.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class VideoLinearLayout extends LinearLayout {
    public VideoLinearLayout(Context context) {
        super(context);
    }

    public VideoLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int parentHeight = (parentWidth/16)*9;
        heightMeasureSpec = (widthMeasureSpec/16)*9;
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        this.setMeasuredDimension(parentWidth, parentHeight);
    }
}
