package com.yabu.android.yabujava.ui;

import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import com.yabu.android.yabujava.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Main Activity holding ViewPager tabs of the main 3 fragments. This is the launch activity unless
 * it is first install, which would show StartUpActivity instead.
 */
public class MainActivity extends AppCompatActivity {

    // late init a shared prefs var to check start up screen
    private SharedPreferences mPrefs;

    // Executor variable to execute in worker threads
    public static ExecutorService executor;

    // Preference key for night mode
    public static String NIGHT_MODE_KEY = "com.yabu.android.yabujava.NIGHT_MODE";

    public OnPageSelectedListener mListener;

    interface OnPageSelectedListener {
        void onPageSelected(int position);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // init a worker thread for the fragments
        // Executor variable to execute in worker threads
        executor = Executors.newCachedThreadPool();

        // Get the tab
        TabLayout tabLayout = findViewById(R.id.tab_layout);

        ViewPager pager = findViewById(R.id.pager);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        // check night mode and set it
        checkNightMode();

        // check if start up has been shown
        checkStartUpScreen();

        // Setup the TabLayout with the ViewPager with helper function.
        setupTabWithViewPager(tabLayout, pager);
    }

    /**
     * check whether night mode is set and change the app.
     */
    private void checkNightMode() {
        // get boolean from preferences, if not found give false
        boolean isNightMode = mPrefs.getBoolean(MainActivity.NIGHT_MODE_KEY, false);

        // if it is set the night mode
        UiModeManager uiManager = (UiModeManager) this.getSystemService(Context.UI_MODE_SERVICE);
        // check if it has been shown
        if (isNightMode) {
            // if it is set the night mode
            if (uiManager != null) {
                uiManager.setNightMode(UiModeManager.MODE_NIGHT_YES);
            }
        } else {
            // if it isnt set the night mode off
            if (uiManager != null) {
                uiManager.setNightMode(UiModeManager.MODE_NIGHT_NO);
            }
        }
    }

    /**
     * Function to check for start up screen in shared preferences and show.
     */
    private void checkStartUpScreen() {
        // get boolean from preferences, if not found give false
        boolean startUpScreenShown = mPrefs.getBoolean(StartUpActivity.START_UP_KEY, false);

        // check if it has been shown
        if (!startUpScreenShown) {
            // if not, show start up activity
            Intent intent = new Intent(this, StartUpActivity.class);
            // start the activity
            startActivity(intent);
            // set the pref to shown to true
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putBoolean(StartUpActivity.START_UP_KEY, true);
            editor.apply();
        }
    }

    /**
     * Helper function to setup Tab Layout with ViewPager adapter in activity_main, and creates
     * and sets a tab selected listener to change icon colours to the primary color.
     */
    private void setupTabWithViewPager(TabLayout tabLayout, ViewPager pager) {
        // The index of the 'primary' tab, i.e. the tab that will first show to the user
        // when the Main Activity is shown. Index 1 is the Reading Tab.
        int primaryTab = 1;

        TabPagerAdapter adapter = new TabPagerAdapter(getSupportFragmentManager());
        // Set the adapter to the view pager in the xml layout, passing the support fragment manager.
        pager.setAdapter(adapter);
        // Set the Tab layout with the ViewPager. tab_layout comes from the kotlinx import which
        // binds the views from the imported layout.
        tabLayout.setupWithViewPager(pager);
        // Make the current item shown be the Reading tab.
        pager.setCurrentItem(primaryTab);
        // Set tab icons with helper function.
        setTabLayout(tabLayout);
        // Implement the Tab listener.
        TabLayout.OnTabSelectedListener tabListener = new TabLayout.OnTabSelectedListener() {

            /**
             * Override TabSelected to change the icon color on selected.
             */
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getIcon() != null) {
                    tab.getIcon().setTint(ContextCompat
                            .getColor(MainActivity.this, R.color.colorPrimary));
                }
            }

            /**
             * Override TabUnselected to change the icon color back.
             */
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                if (tab.getIcon() != null) {
                    tab.getIcon().setTint(ContextCompat
                            .getColor(MainActivity.this, R.color.colorTabUnselected));
                }
            }

            /**
             * Override TabReselected to change the icon color on reselected.
             */
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                if (tab.getIcon() != null) {
                    tab.getIcon().setTint(ContextCompat
                            .getColor(MainActivity.this, R.color.colorPrimary));
                }
            }
        };
        // Set the tab listener
        tabLayout.addOnTabSelectedListener(tabListener);

        pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mListener.onPageSelected(position);
            }
        });
    }

    /**
     * Helper function to set tab icons and tint color in the Tab Layout hooked to the Viewpager.
     */
    private void setTabLayout( TabLayout tabLayout) {
        // Set the tab icons within a loop
        int i = 0;
        while (i < tabLayout.getTabCount()) {
            switch (i) {
                // In the user tab set the user icon
                case 0: {
                    TabLayout.Tab tab = tabLayout.getTabAt(0);
                    if (tab != null) {
                        tab.setIcon(R.drawable.ic_person_black_24dp);
                        tab.getIcon().setTint(ContextCompat.getColor(this, R.color.colorTabUnselected));
                    }
                    break;
                }
                // In the Reading tab set the reading icon
                case 1: {
                    TabLayout.Tab tab = tabLayout.getTabAt(1);
                    if (tab != null) {
                        tab.setIcon(R.drawable.ic_sort_black_24dp);
                        tab.getIcon().setTint(ContextCompat.getColor(this, R.color.colorPrimary));
                    }
                    break;
                }
                // In the Saved Review words set the review icon
                case 2: {
                    TabLayout.Tab tab = tabLayout.getTabAt(2);
                    if (tab != null) {
                        tab.setIcon(R.drawable.ic_folder_special_black_24dp);
                        tab.getIcon().setTint(ContextCompat.getColor(this, R.color.colorTabUnselected));
                    }
                    break;
                }
            }
            i++;
        }
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
        super.onDestroy();
    }

    /**
     * Adapter for the ViewPager. This adapter will feed the correct fragments to the pager when
     * swiping through the tabs. FragmentPagerAdapter was used instead of FragmentStatePagerAdapter,
     * since 3 screens is contained, and the Reading tab is expected to not reload.
     */
    public class TabPagerAdapter extends FragmentPagerAdapter {

        public TabPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        // This is the total count of fragments in the ViewPager. The user, reading and review
        // fragments, i.e. 3 tabs.
        public static final int fragmentCount = 3;

        /**
         * Override function to get total count. Count is arrived from global $fragmentCount val
         * set above.
         */
        @Override
        public int getCount() {
            return 3;
        }

        /**
         * Override function for adapter to get item according to position.
         */
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:  return new UserFragment();
                case 1: return new ReadingFragment();
                case 2: return new ReviewWordsFragment();
                default: return new ReadingFragment();
            }
        }
    }
}
