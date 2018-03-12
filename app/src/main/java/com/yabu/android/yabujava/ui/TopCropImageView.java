package com.yabu.android.yabujava.ui;

import android.content.Context;
import android.graphics.Matrix;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;


/**
 * Custom ImageView class that scales the image and sets the top of the image to the top of the
 * image view frame for better cropping.
 */
public class TopCropImageView extends AppCompatImageView {

    public TopCropImageView(Context context) {
        super(context);
        setScaleType(ScaleType.MATRIX);
    }

    public TopCropImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setScaleType(ScaleType.MATRIX);
    }

    public TopCropImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setScaleType(ScaleType.MATRIX);
    }

    @Override
    protected boolean setFrame(int l, int t, int r, int b) {
        int frameWidth = r - l;
        int frameHeight = b - t;

        float scaleFactor = 1f;

        if (getDrawable() != null) {
            if (frameWidth > getDrawable().getIntrinsicWidth() || frameHeight > getDrawable().getIntrinsicHeight()) {
                float fitXScale = frameWidth / getDrawable().getIntrinsicWidth();
                float fitYScale = frameHeight / getDrawable().getIntrinsicHeight();

                scaleFactor = Math.max(fitXScale, fitYScale);
            }
        }

        Matrix matrix = getImageMatrix();
        matrix.setScale(scaleFactor, scaleFactor, 0f, 0f);
        setImageMatrix(matrix);

        return super.setFrame(l, t, r, b);
    }
}
