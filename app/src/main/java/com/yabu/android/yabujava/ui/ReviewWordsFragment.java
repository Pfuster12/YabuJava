package com.yabu.android.yabujava.ui;


import android.annotation.SuppressLint;
import android.app.UiModeManager;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v4.util.SparseArrayCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.yabu.android.yabujava.R;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import jsondataclasses.Kanji;
import viewmodel.ReviewViewModel;

/**
 * A simple {@link Fragment} subclass.
 */
public class ReviewWordsFragment extends Fragment implements MainActivity.OnPageSelectedListener {

    public static final String REVIEW_KEY = "com.yabu.android.yabujava.REVIEW_KEY";

    private ReviewViewModel mModel;

    // init the wikiExtracts list.
    private ArrayList<Pair<Integer, Kanji>> mReviewKanjis;

    // The linear manager of the recycler view.
    private LinearLayoutManager mLinearLayoutManager;
    // init the adapter
    private ReviewRecyclerViewAdapter mAdapterReview;
    private View mRootView;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private SharedPreferences mPrefs;

    public ReviewWordsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onPageSelected(int position) {
        if (position == 2) {
            mModel.loadReviewKanjis(getContext());
        }
    }

    /**
     * Override function when activity is created to prepare and load data from the view model.
     * This will allow data load to be ready to display when the fragment appears to the user.
     */
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Get the query from the database
        prepareViewModel();

        ((MainActivity) getActivity()).mListener = this;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mReviewKanjis = new ArrayList<>();
        if (savedInstanceState == null) {
            // init an empty list
        } else {
            // if not grab the list from the saved inst state
            if (savedInstanceState.containsKey(REVIEW_KEY)) {
                HashMap<Integer, Kanji> maps = Parcels.unwrap(savedInstanceState.getParcelable(REVIEW_KEY));
                Set<Integer> keys = maps.keySet();
                Collection<Kanji> kanjis = maps.values();
                int i = 0;
                while (i < keys.size()) {
                    mReviewKanjis.add(new Pair<>((Integer) keys.toArray()[i], (Kanji) kanjis.toArray()[i]));
                    i++;
                }
            } else {
                // init an empty list
            }
        }

        mPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Change pair list to map for parceler purposes
        HashMap<Integer, Kanji> map = new HashMap<>();
        for (Pair<Integer, Kanji> reviewKanji : mReviewKanjis) {
            map.put(reviewKanji.first, reviewKanji.second);
        }
        outState.putParcelable(ReviewWordsFragment.REVIEW_KEY, Parcels.wrap(map));

        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Grab the root view inflated for the fragment.
        final View rootView = inflater.inflate(R.layout.fragment_review_words, container, false);
        mRootView = rootView;

        setToolbarTitle(rootView);

        // init the linear layout manager.
        mLinearLayoutManager = new LinearLayoutManager(getContext()) {
            // override to support predictive animations aka, shows non screen elements in animation

            @Override
            public boolean supportsPredictiveItemAnimations() {
                return super.supportsPredictiveItemAnimations();
            }
        };

        RecyclerView recyclerView = rootView.findViewById(R.id.review_recycler_view);
        // Set the layout manager.
        recyclerView.setLayoutManager(mLinearLayoutManager);

        // Set the adapter for the recycler view with the empty list, and declare the listener
        // function.
        mAdapterReview = new ReviewRecyclerViewAdapter(mReviewKanjis,
                getContext(), new ReviewRecyclerViewAdapter.OnEmptyListener() {
            @Override
            public void onEmptyNotify(boolean isEmpty) {
                if (isEmpty) {
                    showNoWords(rootView);
                } else {
                    showRecyclerView(rootView);
                }
            }
        }, mRootView);
        recyclerView.setAdapter(mAdapterReview);
        recyclerView.setItemAnimator(new RecyclerViewAnimator());

        mSwipeRefreshLayout = rootView.findViewById(R.id.review_swipe_refresh);
        mSwipeRefreshLayout.setEnabled(true);
        final ArrayList<Pair<Integer, Kanji>> oldReviewWords = mReviewKanjis;
        // Set the swipe refresh listener
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                onRefreshLayout(rootView, oldReviewWords);
            }
        });

        // Return the inflated view to complete the onCreate process.
        return rootView;
    }

    /**
     * Swipe refresh function helper
     */
    private void onRefreshLayout(final View rootView, final ArrayList<Pair<Integer, Kanji>> oldReviewWords) {
        mModel.loadReviewKanjis(getContext());
        showRecyclerView(rootView);

        // Post a delayed check whether articles are the same and notify.
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // set the refresh indicator to false
                if (mReviewKanjis.containsAll(oldReviewWords)) {
                    mSwipeRefreshLayout.setRefreshing(false);
                    Snackbar.make(rootView.findViewById(R.id.review_recycler_parent),
                            "Review words are updated.", Snackbar.LENGTH_SHORT).show();
                }
            }
        }, 3000);
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
                    mSwipeRefreshLayout.setRefreshing(false);
                    // clear the previous entries
                    mReviewKanjis.clear();
                    showRecyclerView(mRootView);
                    // Add the received wikiExtract list to the list hooked in the adapter.
                    //mWikiExtracts.addAll(wikiExtracts)
                    mReviewKanjis.addAll(kanjis);
                    mAdapterReview.notifyDataSetChanged();
                } else {
                    // Show message
                    if (mReviewKanjis.size() == 0) {
                        showNoWords(mRootView);
                    }
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

    private void showNoWords(View rootView) {
        rootView.findViewById(R.id.review_recycler_view).setVisibility(View.GONE);
        rootView.findViewById(R.id.review_no_words).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.review_no_words).animate().alpha(1.0f).start();
    }


    private void showRecyclerView(View rootView) {
        rootView.findViewById(R.id.review_recycler_view).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.review_no_words).setVisibility(View.GONE);
        rootView.findViewById(R.id.review_no_words).setAlpha(0f);
    }


    /**
     * Helper function to set toolbar to the Review Words title.
     */
    private void setToolbarTitle(View rootView) {
        Toolbar toolbar = rootView.findViewById(R.id.review_layout_toolbar);
        // Grab the title of the toolbar.
        TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
        // Set the title of the toolbar to the Reading tab.
        toolbarTitle.setText(getString(R.string.review_words_page_title));


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
}
