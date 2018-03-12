package com.yabu.android.yabujava.ui;

import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;

import com.yabu.android.yabujava.R;

import java.util.ArrayList;

public class StartUpActivity extends AppCompatActivity {

    public static final String START_UP_KEY = "com.yabu.android.yabujava.SIGN_IN_EXTRA";

    @Override
    protected void onStart() {
        // set the animation
        AnimationDrawable frameAnimation = ((AnimationDrawable) ((ImageView) findViewById(R.id.animated_logo)).getDrawable());
        frameAnimation.start();
        super.onStart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_up);

        // Set the on click for continue without sign in option.
        findViewById(R.id.start_up_continue_no_sign).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StartUpActivity.this, InfoActivity.class);

                View statusBar = findViewById(android.R.id.statusBarBackground);
                View navigationBar = findViewById(android.R.id.navigationBarBackground);

                ArrayList<Pair<View, String>> pairs = new ArrayList<>();
                if (statusBar != null) {
                    pairs.add(Pair.create(statusBar, Window.STATUS_BAR_BACKGROUND_TRANSITION_NAME));
                }
                if (navigationBar != null) {
                    pairs.add(Pair.create(navigationBar, Window.NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME));
                }
                Bundle options = ActivityOptions.makeSceneTransitionAnimation(
                        StartUpActivity.this, pairs.get(0), pairs.get(1)).toBundle();
                startActivity(intent, options);
            }
        });


    }
}
