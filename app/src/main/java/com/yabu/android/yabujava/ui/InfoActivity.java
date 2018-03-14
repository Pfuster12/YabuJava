package com.yabu.android.yabujava.ui;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.yabu.android.yabujava.R;

public class InfoActivity extends AppCompatActivity {

    private static final long PEEK_START_DURATION = 500;
    private static final long PEEK_END_DURATION = 280;
    private static final int PEEK_DELAY_DURATION = 900;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        ViewPager pager = findViewById(R.id.info_viewpager);
        pager.setAdapter(new InfoPagerAdapter(getSupportFragmentManager()));

        findViewById(R.id.dot_page_0_accent).setVisibility(View.VISIBLE);
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                // Change the dots color to highlight the position
                switch (position) {
                    case 0: {
                        findViewById(R.id.dot_page_0_accent).setVisibility(View.VISIBLE);
                        findViewById(R.id.dot_page_1_accent).setVisibility(View.INVISIBLE);
                        findViewById(R.id.dot_page_2_accent).setVisibility(View.INVISIBLE);
                        ((TextView) findViewById(R.id.info_skip_text)).setText(getString(R.string.info_skip_text));
                        break;
                    }
                    case 1: {
                        findViewById(R.id.dot_page_0_accent).setVisibility(View.INVISIBLE);
                        findViewById(R.id.dot_page_1_accent).setVisibility(View.VISIBLE);
                        findViewById(R.id.dot_page_2_accent).setVisibility(View.INVISIBLE);
                        ((TextView) findViewById(R.id.info_skip_text)).setText(getString(R.string.info_skip_text));
                        break;
                    }
                    case 2: {
                        findViewById(R.id.dot_page_0_accent).setVisibility(View.INVISIBLE);
                        findViewById(R.id.dot_page_1_accent).setVisibility(View.INVISIBLE);
                        findViewById(R.id.dot_page_2_accent).setVisibility(View.VISIBLE);
                        ((TextView) findViewById(R.id.info_skip_text)).
                                setText(getString(R.string.info_start_reading_text));
                        break;
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        // set an on click to launch main activity
        findViewById(R.id.info_skip_text).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(InfoActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        peekViewPagerAction();
    }


    private void peekViewPagerAction() {
        final ViewPager pager = findViewById(R.id.info_viewpager);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // launch an object animator to scroll by x offset
                int x = 100;
                int y = 0;
                ObjectAnimator xTranslate = ObjectAnimator.ofInt(pager, "scrollX", x);
                ObjectAnimator yTranslate = ObjectAnimator.ofInt(pager, "scrollY", y);
                AnimatorSet animators = new AnimatorSet();
                animators.setDuration(PEEK_START_DURATION);
                animators.playTogether(xTranslate, yTranslate);
                animators.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    // listen to the end to start the return animation
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        int x2 = 0;
                        int y2 = 0;
                        ObjectAnimator x2Translate = ObjectAnimator.ofInt(pager, "scrollX", x2);
                        ObjectAnimator y2Translate = ObjectAnimator.ofInt(pager, "scrollY", y2);
                        AnimatorSet animators2 = new AnimatorSet();
                        animators2.setDuration(PEEK_END_DURATION);
                        animators2.playTogether(x2Translate, y2Translate);
                        animators2.start();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                animators.start();
            }
        }, PEEK_DELAY_DURATION);
    }

    class InfoPagerAdapter extends FragmentPagerAdapter {

        public InfoPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // Return the correct fragment according to its position.
            switch (position) {
                // We are in the first tab aka User tab
                case 0: return new Info1Fragment();
                // We are in the middle tab aka Reading tab.
                case 1: return new Info2Fragment();
                // We are in the third tab aka Review Words tab
                case 2: return new Info3Fragment();
                // Handle default cases, in which case return to main Reading tab.
                default: return new Info1Fragment();
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    }
}
