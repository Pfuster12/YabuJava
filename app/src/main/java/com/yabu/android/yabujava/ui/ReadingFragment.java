package com.yabu.android.yabujava.ui;

import android.app.UiModeManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader;
import com.bumptech.glide.util.ViewPreloadSizeProvider;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.yabu.android.yabujava.R;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jsondataclasses.WikiExtract;
import jsondataclasses.WikiThumbnail;
import repository.WikiExtractRepository;
import sql.WikiExtractsContract.WikiExtractsEntry;
import viewmodel.WikiExtractsViewModel;
import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

/**
 * Reading list fragment, to be paired with ViewPager for tab slide animations in Main Activity.
 * This fragment displays a list of articles extracted from the JsonUtils API.
 */
public class ReadingFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String WIKI_EXTRACTS_KEY = "com.yabu.android.yabujava.WIKI_EXTRACTS_KEY";

    // The ViewModel to instantiate it through the provider.
    private WikiExtractsViewModel mModel;
    private WikiExtractRepository mRepo;
    // The linear manager of the recycler view.
    private LinearLayoutManager mLinearLayoutManager;
    // init the wikiExtracts list.
    private static ArrayList<WikiExtract> mWikiExtracts;
    // init the adapter
    private ReadingRecyclerViewAdapter mAdapterReading;
    // swipe refresh global var
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private View mRootView;

    private DetailFragment mBottomSheet;

    // late init a shared prefs var to check start up screen
    private SharedPreferences mPrefs;

    private FirebaseAnalytics mFirebaseAnalytics;

    private static final int REFRESH_SNACK_DELAY = 3000;
    private static final int NO_CONNECTION_ALPHA_DELAY = 1500;
    private static final int NO_CONNECTION_ALPHA_DURATION = 2000;
    private static final int EXECUTOR_TERMINATION_WAIT = 800;

    private static final int READING_CURSOR_LOADER_ID = 1;

    public ReadingFragment() {
        // Required empty public constructor
    }

    /**
     * Create cursor loader with projection to get wiki extracts
     */
    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Set the query params for the definition query.
        String[] projection = new String[]{WikiExtractsEntry._ID,
                WikiExtractsEntry.COLUMN_PAGE_ID,
                WikiExtractsEntry.COLUMN_TITLE,
                WikiExtractsEntry.COLUMN_EXTRACT,
                WikiExtractsEntry.COLUMN_THUMBNAIL};
       return new CursorLoader(getContext(), WikiExtractsEntry.CONTENT_URI,
                projection, null,
                null,
                null);
    }

    /**
     * Set the views with the cursor data
     */
    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor cursor) {
        // Show the recycler view layout
        showRecyclerView(mRootView);
        mSwipeRefreshLayout.setRefreshing(false);
        ArrayList<WikiExtract> oldExtracts = mWikiExtracts;
        // clear the previous entries
        mWikiExtracts.clear();

        // Check for empty cursor
        if (cursor != null && cursor.getCount() > 0){
            cursor.moveToFirst();
            do {
                // Grab all the new data
                int pageId = cursor.getInt(cursor.getColumnIndex(WikiExtractsEntry.COLUMN_PAGE_ID));
                String title = cursor.getString(cursor.getColumnIndex(WikiExtractsEntry.COLUMN_TITLE));
                String extract = cursor.getString(cursor.getColumnIndex(WikiExtractsEntry.COLUMN_EXTRACT));
                String thumbnail = cursor.getString(cursor.getColumnIndex(WikiExtractsEntry.COLUMN_THUMBNAIL));

                // Create the new wiki extract with data from database
                WikiExtract wikiExtract =
                        new WikiExtract(pageId, title, extract, new WikiThumbnail(thumbnail));
                // Add the received wikiExtract list to the list hooked in the adapter.
                mWikiExtracts.add(wikiExtract);
            } while (cursor.moveToNext());

            mAdapterReading.notifyDataSetChanged();
            if (mWikiExtracts.containsAll(oldExtracts)) {
                Snackbar.make(mRootView.findViewById(R.id.reading_recycler_parent),
                        getString(R.string.articles_already_updated_snack), Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(mRootView.findViewById(R.id.reading_recycler_parent),
                        getString(R.string.articles_updated_snack),
                        Snackbar.LENGTH_SHORT).show();
            }
        } else {
            showOnlyNoInternetConnection(mRootView);
        }
    }

    /**
     * reset the loader
     */
    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mRepo = WikiExtractRepository.getInstance();
        mRepo.refreshExtracts(getContext());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // init firebase analytics
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this.getContext());

        if (savedInstanceState == null) {
            //
            mWikiExtracts = new ArrayList<>();
            getLoaderManager().initLoader(READING_CURSOR_LOADER_ID, null, this);
        } else {
            // if not grab the list from the saved inst state
            Parcelable wikiParcelable = savedInstanceState.getParcelable(ReadingFragment.WIKI_EXTRACTS_KEY);
            // set the global list to this
            mWikiExtracts = Parcels.unwrap(wikiParcelable);
            if (mWikiExtracts != null) {
                if (mWikiExtracts.size() == 0) {
                    getLoaderManager().restartLoader(READING_CURSOR_LOADER_ID, null, this);
                }
            }
        }


        // init the linear layout manager.
        mLinearLayoutManager = new LinearLayoutManager(this.getContext());
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this.getContext());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(ReadingFragment.WIKI_EXTRACTS_KEY, Parcels.wrap(mWikiExtracts));

        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Grab the root view inflated for the fragment.
        final View rootView = inflater.inflate(R.layout.fragment_reading, container, false);
        // Set toolbar title.
        setToolbarTitle(rootView);

        RecyclerView recyclerView = rootView.findViewById(R.id.reading_recycler_view);
        final FrameLayout fragmentContainer = rootView.findViewById(R.id.landscape_detail_fragment_container);
        // Set the layout manager.
        recyclerView.setLayoutManager(mLinearLayoutManager);

        if (fragmentContainer != null) {
            TextView refreshButton = rootView.findViewById(R.id.refresh_button_land);
            refreshButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onRefreshSwipe(rootView, mWikiExtracts);
                }
            });
        }
        // Add on scroll listener for glide integration to preload images before scrolling.
        recyclerView.addOnScrollListener(prepareGlideRecyclerViewIntegration());
        // Set the adapter for the recycler view with the empty list, and declare the listener
        // function.
        mAdapterReading = new ReadingRecyclerViewAdapter(mWikiExtracts,
                this.getContext(), new ReadingRecyclerViewAdapter.ReadingOnClick() {
            @Override
            public void readingOnClick(WikiExtract extract) {
                // log analytics event
                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.ITEM_ID, extract.title);
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

                // check if its sw600 landscape layout
                if (fragmentContainer != null) {
                    // if it is add fragment to container
                    mBottomSheet = DetailFragment.newInstance(Parcels.wrap(extract));
                    if (fragmentContainer.getId() != -1) {
                        getFragmentManager().beginTransaction()
                                .replace(fragmentContainer.getId(), mBottomSheet)
                                .commit();
                    }
                } else {
                    // we are in non sw600 layout
                    // set the function with the clicked extract parameter to start the framgent
                    // bottom sheet. Put the extract as a parcelable.
                    mBottomSheet = DetailFragment.newInstance(Parcels.wrap(extract));
                    mBottomSheet.show(getFragmentManager(), mBottomSheet.getTag());
                }
            }
        });

        recyclerView.setAdapter(mAdapterReading);
        recyclerView.setItemAnimator(new RecyclerViewAnimator());

        // Show the recycler view layout
        showRecyclerView(rootView);

        mSwipeRefreshLayout = rootView.findViewById(R.id.swipe_refresh);
        if (fragmentContainer == null) {
            mSwipeRefreshLayout.setEnabled(true);
            final ArrayList<WikiExtract> oldExtracts = mWikiExtracts;
            // Set the swipe refresh listener
            mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    onRefreshSwipe(rootView, oldExtracts);
                }
            });
        } else {
            mSwipeRefreshLayout.setEnabled(false);
        }

        // assign a global var
        mRootView = rootView;
        // check if its sw600 landscape layout
        if (fragmentContainer != null) {
            setRetainInstance(false);
        }
        // Return the inflated view to complete the onCreate process.
        return rootView;
    }

    /**
     * On refresh helper function for the swipe refresh layout
     */
    private void onRefreshSwipe(final View rootView, final ArrayList<WikiExtract> oldExtracts) {
        // Load the daily extracts from the main wiki page per user refresh
        mRepo.refreshExtracts(getContext());
        getLoaderManager().restartLoader(READING_CURSOR_LOADER_ID, null, this);
        // Post a delayed check whether articles are the same and notify.
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // set the refresh indicator to false
                if (mWikiExtracts.containsAll(oldExtracts) && mWikiExtracts.size() != 0) {
                    mSwipeRefreshLayout.setRefreshing(false);
                    Snackbar.make(rootView.findViewById(R.id.reading_recycler_parent),
                            getString(R.string.articles_already_updated_snack), Snackbar.LENGTH_SHORT).show();
                } else if (mWikiExtracts.size() == 0) {
                    mSwipeRefreshLayout.setRefreshing(false);
                    Snackbar.make(rootView.findViewById(R.id.reading_recycler_parent),
                            getString(R.string.no_articles_loaded_snack), Snackbar.LENGTH_SHORT).show();
                }
            }
        }, REFRESH_SNACK_DELAY);
    }

    /**
     * Helper function to set toolbar to the Reading title.
     */
    private void setToolbarTitle(View rootView) {
        Toolbar toolbar = rootView.findViewById(R.id.layout_toolbar);
        // Grab the title of the toolbar.
        TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
        // Set the title of the toolbar to the Reading tab.
        toolbarTitle.setText(getString(R.string.reading_page_title));

        final ImageView nightButton = toolbar.findViewById(R.id.night_mode);
        nightButton.setSelected(checkNightMode());
        // set the night mode action button
        nightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (nightButton.isSelected()) {
                    nightButton.setSelected(false);
                    nightButton.setColorFilter(
                            ContextCompat.getColor(ReadingFragment.super.getContext(),
                                    R.color.color100Grey), PorterDuff.Mode.SRC_ATOP);
                    setNightMode(false);
                } else {
                    nightButton.setSelected(true);
                    nightButton.setColorFilter(
                            ContextCompat.getColor(ReadingFragment.super.getContext(),
                                    R.color.colorTextWhite), PorterDuff.Mode.SRC_ATOP);
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
    private void setNightMode(boolean isNightMode) {
        // set the pref to shown to true
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putBoolean(MainActivity.NIGHT_MODE_KEY, isNightMode);
        editor.apply();

        UiModeManager uiManager = (UiModeManager) super.getContext().getSystemService(Context.UI_MODE_SERVICE);
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
     * Helper function to set up Glide's recycler view pre load image integration
     */
    private RecyclerViewPreloader<String> prepareGlideRecyclerViewIntegration() {
        // Create a size provider which tells it its a set view to load
        ListPreloader.PreloadSizeProvider<String> sizeProvider = new ViewPreloadSizeProvider<>();
        // init a model provider to implement functions getting preload items
        ListPreloader.PreloadModelProvider<String> modelProvider =
                new MyPreloadModelProvider(mWikiExtracts, this.getContext());

        // Return a preloader object.
        return new RecyclerViewPreloader<>(Glide.with(this), modelProvider, sizeProvider, 4);
    }

    /**
     * Helper function to show the no connection message
     */
    private void showOnlyNoInternetConnection(View rootView) {
        rootView.findViewById(R.id.reading_recycler_view).setVisibility(View.GONE);
        rootView.findViewById(R.id.reading_no_connection).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.reading_no_connection)
                .findViewById(R.id.reading_no_connection_detail)
                .animate().alpha(1f).setDuration(NO_CONNECTION_ALPHA_DURATION).setStartDelay(NO_CONNECTION_ALPHA_DELAY).start();
    }

    /**
     * Helper function to show recycler view
     */
    private void showRecyclerView(View rootView) {
        rootView.findViewById(R.id.reading_recycler_view).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.reading_no_connection).setVisibility(View.GONE);
        rootView.findViewById(R.id.reading_no_connection)
                .findViewById(R.id.reading_no_connection_detail).setAlpha(0f);
    }

    @Override
    public void onDestroy() {
        WikiExtractRepository.executor.shutdown();
        try {
            if (!WikiExtractRepository.executor.awaitTermination(EXECUTOR_TERMINATION_WAIT, TimeUnit.MILLISECONDS)) {
                WikiExtractRepository.executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            WikiExtractRepository.executor.shutdownNow();
        }
        super.onDestroy();
    }

    /**
     * Preload Model provider for the Glide integration with Recycler View. The class receives
     * preload urls (models) to load when the user scrolls so the images are preloaded.
     */
    private class MyPreloadModelProvider implements ListPreloader.PreloadModelProvider<String> {

        private Context mContext;
        private ArrayList<WikiExtract> mWikiExtracts;
        public MyPreloadModelProvider(ArrayList<WikiExtract> wikiExtracts, Context context) {
            mContext = context;
            mWikiExtracts = wikiExtracts;
        }

        /**
         * Gets the preload items from the urls
         */
        @NonNull
        @Override
        public List<String> getPreloadItems(int position) {
            // Get the current extract.
            int currentExtract = position + 1;
            // Check if position is within the extract positions.
            if (currentExtract < mWikiExtracts.size() + 1) {
                WikiExtract extract = mWikiExtracts.get(position);
                // Grab the thumbnail url of the current extract.
                String url = extract.thumbnail.source;
                if (url != null) {
                    if (url.isEmpty()) {
                        return new ArrayList<>();
                    } else {
                        ArrayList<String> list = new ArrayList<>();
                        list.add(url);
                        return list;
                    }
                }
            }
            // Return an empty list if not an extract.
            return new ArrayList<>();
        }

        /**
         * Returns a request builder that preload views will use to fill with image.
         */
        @Nullable
        @Override
        public RequestBuilder getPreloadRequestBuilder(String item) {
            return GlideApp.with(mContext)
                    .load(item)
                    .placeholder(R.color.color500Grey)
                    .error(R.drawable.ground_astronautmhdpi)
                    .transition(withCrossFade());
        }
    }
}
