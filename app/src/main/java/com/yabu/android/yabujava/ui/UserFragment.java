package com.yabu.android.yabujava.ui;


import android.app.UiModeManager;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.yabu.android.yabujava.R;

import java.util.ArrayList;
import java.util.List;

import jsondataclasses.Kanji;
import viewmodel.ReviewViewModel;

/**
 * User profile fragment, to show user profile and stats.
 */
public class UserFragment extends Fragment implements MainActivity.OnPageSelectedListener  {

    private ReviewViewModel mModel;
    // init the wikiExtracts list.
    private ArrayList<Pair<Integer, Kanji>> mReviewKanjis;
    private View mRootView;
    private SharedPreferences mPrefs;
    // global line chart
    private LineChart mLineChart;
    private static final String HAS_BEEN_REFRESHED = "com.yabu.android.yabujava.HAS_BEEN_REFRESHED";

    public UserFragment() {
        // Required empty public constructor
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        prepareViewModel();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        // init an empty list
        mReviewKanjis = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Grab the root view inflated for the fragment.
        View rootView = inflater.inflate(R.layout.fragment_user, container, false);
        // Inflate the layout for this fragment
        setToolbarTitle(rootView);

        mRootView = rootView;

        setLineChart(rootView);
        setPieChart(rootView);
        // Return the inflated view to complete the onCreate process.
        return rootView;
    }

    @Override
    public void onPageSelected(int position) {
        if (position == 0) {
            mModel.loadReviewKanjis(getContext());
            if (mLineChart != null) {
                mLineChart.invalidate();
            }
        }
    }

    /**
     * Helper function to set toolbar to the User title.
     */
    private void setToolbarTitle(View rootView) {
        // Grab the title of the toolbar.
        Toolbar toolbar = rootView.findViewById(R.id.user_layout_toolbar);
        TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
        // Set the title of the toolbar to the Reading tab.
        toolbarTitle.setText(getString(R.string.your_stats_page_title));

        final ImageView nightButton = toolbar.findViewById(R.id.night_mode);
        nightButton.setSelected(checkNightMode());
        // set the night mode action button
        nightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (nightButton.isSelected()) {
                    nightButton.setSelected(false);
                    nightButton.setColorFilter(
                            ContextCompat.getColor(getContext(), R.color.color100Grey), PorterDuff.Mode.SRC_ATOP);
                    setNightMode(false);
                } else {
                    nightButton.setSelected(true);
                    nightButton.setColorFilter(
                            ContextCompat.getColor(getContext(), R.color.colorTextWhite), PorterDuff.Mode.SRC_ATOP);
                    setNightMode(true);
                }
            }
        });
    }


    /**
     * check whether night mode is set and change the app.
     */
    private boolean checkNightMode() {
        // get boolean from preferences, if not found give false
        return mPrefs.getBoolean(MainActivity.NIGHT_MODE_KEY, false);
    }

    /**
     * set the night mode in the preferences.
     */
    private void setNightMode(Boolean isNightMode) {
        // set the pref to shown to true
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putBoolean(MainActivity.NIGHT_MODE_KEY, isNightMode);
        editor.apply();

        UiModeManager uiManager = (UiModeManager) getContext().getSystemService(Context.UI_MODE_SERVICE);
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
     * Helper fun to query review words from database and feed into recycler view adapter.
     */
    private void prepareViewModel() {
        // Get the ViewModel for the Reading list
        mModel = ViewModelProviders.of(this)
                .get(ReviewViewModel.class);

        // Create the observer which updates the UI. Whenever the data changes, the new pojo list
        // is fed through and the UI can be updated.
        Observer<ArrayList<Pair<Integer, Kanji>>> observer = new Observer<ArrayList<Pair<Integer, Kanji>>>() {
            @Override
            public void onChanged(@Nullable ArrayList<Pair<Integer, Kanji>> kanjis) {
                // Check for null since addAll() accepts only non null
                if (kanjis != null && kanjis.size() != 0) {
                    // clear the previous entries
                    mReviewKanjis.clear();
                    // Add the received wikiExtract list to the list hooked in the adapter.
                    //mWikiExtracts.addAll(wikiExtracts)
                    mReviewKanjis.addAll(kanjis);

                    setPieChart(mRootView);
                }
            }
        };

        // Load the daily extracts from the main wiki page
        // through a retrofit call and set the LiveData value.
        mModel.loadReviewKanjis(getContext());

        // Observe the LiveData in the view model which will be set to the extracts,
        // passing in the fragment as the LifecycleOwner and the observer created above.
        mModel.reviewKanjis.observe(this, observer);
    }


    /**
     * Helper fun to set the chart data and format
     */
    private void setLineChart(View rootView) {
        mLineChart = rootView.findViewById(R.id.chart);
        ArrayList<Entry> entries = new ArrayList<>();

        // grab the articles as a list from monday to sunday.
        List<Integer> articlesRead = getArticlesRead();

        int i = 0;
        while (i < articlesRead.size()) {
            // set entries to show in chart
            entries.add(new Entry(i, articlesRead.get(i)));
            i++;
        }

        // the labels that should be drawn on the XAxis
        final String[] quarters = new String[]{"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

        // format the axis labels to be the days
        IAxisValueFormatter formatter = new IAxisValueFormatter() {

            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return quarters[Integer.valueOf(String.valueOf((int) value))];
            }

            // we don't draw numbers, so no decimal digits needed
            int decimalDigits;
        };

        // set data set
        LineDataSet dataSet = new LineDataSet(entries, "");
        dataSet.setColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
        dataSet.setValueTextColor(ContextCompat.getColor(getContext(), R.color.color300Grey));
        dataSet.setFillColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
        dataSet.setDrawFilled(true);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(3f);
        dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        dataSet.setCircleColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
        dataSet.setCircleColorHole(ContextCompat.getColor(getContext(), R.color.colorAccent));

        // create line data set
        LineData lineData = new LineData(dataSet);
        mLineChart.setData(lineData);
        // no grid
        mLineChart.setDrawGridBackground(false);
        mLineChart.fitScreen();
        Description des = new Description();
        des.setText("");
        mLineChart.setDescription(des);
        mLineChart.getLegend().setEnabled(false);

        // x axis format
        mLineChart.getXAxis().setValueFormatter(formatter);
        mLineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        mLineChart.getXAxis().setDrawGridLines(false);
        mLineChart.getXAxis().setGranularity(1f);
        mLineChart.getXAxis().setTextSize(10f);
        mLineChart.getXAxis().setTextColor(ContextCompat.getColor(getContext(), R.color.color700Grey));

        // y axis format
        mLineChart.getAxisLeft().setDrawGridLines(false);
        mLineChart.getAxisLeft().setDrawLabels(false);
        mLineChart.getAxisLeft().setAxisMinimum(0f);
        mLineChart.getAxisLeft().setGranularity(1f);
        mLineChart.getAxisLeft().setTextSize(10f);
        mLineChart.getAxisRight().setEnabled(false);
        // refresh chart
        mLineChart.invalidate();
        mLineChart.animateX(1000, Easing.EasingOption.EaseOutBack);
    }

    /**
     * Helper fun to set the articles to persist for data graph in user fragment
     */
    private List<Integer> getArticlesRead() {
        // init the articles
        ArrayList<Integer> articlesRead = new ArrayList<>();
        // get todays day
        Integer today = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK);
        if (today == 2) {
            boolean isRefreshed = mPrefs.getBoolean(HAS_BEEN_REFRESHED, false);
            // cycle through the articles read to get all days
            int i = 1;
            while (i < 8) {
                if (!isRefreshed) {
                    mPrefs.edit().putInt(String.valueOf(i), 0).apply();
                }
                i++;
            }
            mPrefs.edit().putBoolean(HAS_BEEN_REFRESHED, true).apply();
        }
        // cycle through the articles read to get all days
        int i = 1;
        while (i < 8) {
            articlesRead.add(mPrefs.getInt(String.valueOf(i), 0));
            i++;
        }

        return articlesRead;
    }

    /**
     * Helper fun to set the chart data and format
     */
    private void setPieChart(View rootView) {
        PieChart chart = rootView.findViewById(R.id.pie_chart);
        ArrayList<PieEntry> entries = new ArrayList<>();
        int jlpt1Val = 0;
        int jlpt2Val = 0;
        int jlpt3Val = 0;
        int jlpt4Val = 0;
        int jlpt5Val = 0;

        for (Pair<Integer, Kanji> kanji : mReviewKanjis) {
            switch (kanji.second.mJlptTag) {
                case -1: {
                    break;
                }
                case 1: jlpt1Val++;
                break;
                case 2: jlpt2Val++;
                break;
                case 3: jlpt3Val++;
                break;
                case 4: jlpt4Val++;
                break;
                case 5: jlpt5Val++;
                break;
            }
        }

        // set entries to show in chart
        entries.add(new PieEntry(jlpt5Val, getString(R.string.JLPTN5)));
        entries.add(new PieEntry(jlpt4Val, getString(R.string.JLPTN4)));
        entries.add(new PieEntry(jlpt3Val, getString(R.string.JLPTN3)));
        entries.add(new PieEntry(jlpt2Val, getString(R.string.JLPTN2)));
        entries.add(new PieEntry(jlpt1Val, getString(R.string.JLPTN1)));
        // set data set
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ContextCompat.getColor(getContext(), R.color.colorJLPTN5),
                ContextCompat.getColor(getContext(), R.color.colorJLPTN4),
                ContextCompat.getColor(getContext(), R.color.colorJLPTN3),
                ContextCompat.getColor(getContext(), R.color.colorJLPTN2),
                ContextCompat.getColor(getContext(), R.color.colorJLPTN1));
        // create line data set
        chart.setData(new PieData(dataSet));

        // format the chart
        Description des = new Description();
        des.setText("");
        chart.setDescription(des);
        chart.setMaxAngle(180f);
        chart.setHoleRadius(48f);
        chart.setRotationAngle(-240f);
        chart.setDrawEntryLabels(false);
        chart.getData().setValueTextColor(ContextCompat.getColor(getContext(), R.color.colorBackground));
        chart.getData().setValueTextSize(8f);
        chart.setNoDataText("No Data!");
        chart.setNoDataTextColor(ContextCompat.getColor(getContext(), R.color.color500Grey));
        chart.setTransparentCircleAlpha(0);
        chart.setEntryLabelTextSize(8f);
        chart.getLegend().setEnabled(false);
        chart.highlightValue(0f, 0);
        chart.invalidate();
    }
}
