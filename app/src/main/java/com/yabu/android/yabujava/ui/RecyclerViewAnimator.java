package com.yabu.android.yabujava.ui;

import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;

/**
 * Implementation of the Recycler View Default item animator. Override the add item animation
 * for a smoother fade in.
 */
public class RecyclerViewAnimator extends DefaultItemAnimator {

    public RecyclerViewAnimator() {

    }

    /**
     * Override the animate add to give it a fade in.
     */
    @Override
    public boolean animateAdd(RecyclerView.ViewHolder holder) {
        int shortAnimTime = holder.itemView.getContext().getResources().getInteger(android.R.integer.config_mediumAnimTime);
        holder.itemView.setAlpha(0f);
        holder.itemView.animate()
                .alpha(1f)
                .setDuration(shortAnimTime);

        dispatchAddFinished(holder);
        return true;
    }
}
