package com.yabu.android.yabujava.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.Snackbar;
import android.support.transition.TransitionManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.text.Layout;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.yabu.android.yabujava.R;

import org.parceler.Parcels;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import jsondataclasses.Kanji;
import jsondataclasses.WikiExtract;
import repository.JishoRepository;
import sql.KanjisSQLDao;
import utils.MiscUtils;
import viewmodel.JishoViewModel;
import viewmodel.WikiExtractsViewModel;


public class DetailFragment extends BottomSheetDialogFragment {

    // The ViewModel to instantiate it through the provider.
    private JishoViewModel mModel;

    // The ViewModel to instantiate it through the provider.
    private WikiExtractsViewModel mModelWiki;

    // List of pairs for the Kanji pojo and the index range in the extract text
    private ArrayList<Pair<Pair<Integer, Integer>, Kanji>> mKanjis;

    private SharedPreferences mPrefs;

    View mRootView;

    private boolean misRead = false;

    public static final String WIKI_EXTRACTS_BUNDLE = "com.yabu.android.yabujava.WIKI_EXTRACTS_BUNDLE";

    public static final String READ_BOOL_KEY = "com.yabu.android.yabujava.READ_BOOL_KEY";

    public static final String KANJIS_KEY = "com.yabu.android.yabujava.KANJIS_KEY";

    /**
     * Lazy init the parcelable unwrap into the current wikiExtract to have class wide access to it.
     */
    private WikiExtract wikiExtract;

    private SpannableString spannableString;

    private KanjisSQLDao kanjiDao = KanjisSQLDao.getInstance();

    public DetailFragment() {
        // Required empty public constructor
    }

    /**
     * Helper function to create new instance passing the
     * wikiExtract as a bundle in the arguments.
     */
    public static DetailFragment newInstance(Parcelable parcelable) {
        // Get an instance
        DetailFragment fragment = new DetailFragment();
        // Set the arguments to the extract passed in constructor
        Bundle args = new Bundle();
        args.putParcelable(WIKI_EXTRACTS_BUNDLE, parcelable);
        fragment.setArguments(args);

        // return the loaded fragment
        return fragment;
    }

    /**
     * Override onCreate to analyze vocabulary.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // init the parcelable unwrap into the current wikiExtract to have class wide access to it.
        Parcelable parcelable = getArguments().getParcelable(WIKI_EXTRACTS_BUNDLE);
        wikiExtract = Parcels.unwrap(parcelable);

        // Get the ViewModel for the Jisho Detail Fragment
        mModelWiki = ViewModelProviders.of(this)
                .get(WikiExtractsViewModel.class);

        // init the spannable
        if (wikiExtract.extract != null) {
            spannableString = new SpannableString(wikiExtract.extract);
        } else {
            spannableString = new SpannableString("");
            Toast.makeText(this.getContext(), getContext().getString(R.string.no_extract_text),
                    Toast.LENGTH_SHORT)
                    .show();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // save isRead boolean
        outState.putBoolean(DetailFragment.READ_BOOL_KEY, misRead);

        // Change pair list to map for parceler purposes
        HashMap<List<Integer>, Kanji> map = new HashMap<List<Integer>, Kanji>();
        for (Pair<Pair<Integer, Integer>, Kanji> kanji : mKanjis) {
            ArrayList<Integer> rangeList = new ArrayList<>();
            rangeList.add(kanji.first.first);
            rangeList.add(kanji.first.second);
            map.put(rangeList, kanji.second);
        }
        outState.putParcelable(DetailFragment.KANJIS_KEY, Parcels.wrap(map));
        super.onSaveInstanceState(outState);
    }

    /**
     * Override to inflate layout and bind views.
     */
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        mRootView = rootView;

        if (savedInstanceState == null) {
            // init the kanji list
            mKanjis = new ArrayList<>();

            misRead =  mModelWiki.isRead(getContext(), wikiExtract);
        } else {
            mKanjis = new ArrayList<>();
            HashMap<List<Integer>, Kanji> maps = Parcels.
                    unwrap(savedInstanceState.getParcelable(DetailFragment.KANJIS_KEY));
            ArrayList<Pair<Pair<Integer, Integer>, Kanji>> pairKanjisNew = new ArrayList<>();

            Set<List<Integer>> ranges = maps.keySet();

            for (List<Integer> range : ranges) {
                int firstRange = range.get(0);
                int lastRange = range.get(1);

                pairKanjisNew.add(new Pair<>(new Pair<>(firstRange, lastRange), maps.get(range)));
            }
            mKanjis.addAll(pairKanjisNew);

            for (Pair<Pair<Integer, Integer>, Kanji> kanji : mKanjis){
                if (mRootView.findViewWithTag(kanji.first.first.toString()) == null) {
                    // Create the furigana text view
                    TextView furiganaView = new TextView(getContext());
                    furiganaView.setTag(kanji.first.first.toString());
                    furiganaView.setVisibility(View.INVISIBLE);
                    // Set the text to the reading from the kanji pojo
                    furiganaView.setText(kanji.second.mReading);
                    // Set text view properties
                    furiganaView.setMaxLines(1);
                    furiganaView.setTextAppearance(getContext(), R.style.FuriganaStyle);
                    // Add the furigana text view to the constraint layout parent.
                    ((FrameLayout) mRootView.findViewById(R.id.furigana_parent)).addView(furiganaView);
                }
            }

            misRead = savedInstanceState.getBoolean(DetailFragment.READ_BOOL_KEY);
        }

        TextView extractText = rootView.findViewById(R.id.detail_extract);
        if (savedInstanceState != null) {
            // Set the clickable spans with the Kanjis
            setSpannable(mKanjis, mRootView);
            // update the spannable string to the text view
            extractText.setText(spannableString);
        }

        // Set the toolbar title to Reading title.
        setToolbarTitle(mRootView);

        // Bind the views with the current extract data
        ((TextView) rootView.findViewById(R.id.detail_title)).setText(wikiExtract.title.trim());

        // Set the link movement for the text view for clickable spans to work.
        extractText.setMovementMethod(LinkMovementMethod.getInstance());

        // Bind the clickable span string as text to the view.
        extractText.setText(spannableString);

        // Set thumbnail with Glide.
        GlideApp.with(getContext())
                .load(wikiExtract.thumbnail.source)
                .placeholder(R.color.color500Grey)
                .into((TopCropImageView) rootView.findViewById(R.id.detail_thumbnail));

        // Prepare the view model when the activity is created. The async api call is sent here
        // and the live data object is set.
        if (!spannableString.toString().isEmpty()) {
            prepareViewModel(rootView);
        }

        // Set an on scroll listener to hide the callout bubble when scrolling.
        ((NestedScrollView) rootView.findViewById(R.id.scroll_parent))
                .getViewTreeObserver().addOnScrollChangedListener(
                        new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                hideCalloutBubble(mRootView);
            }
        });

        final TextView readButton = rootView.findViewById(R.id.read_button);

        if (misRead) {
            readButton.setText(getContext().getString(R.string.read_that_button_false));
        }

        // set an onclick to the read button to set if its read
        readButton.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                  if (misRead) {
                      misRead = false;
                      readButton.animate().alpha(0.2f).setListener(new AnimatorListenerAdapter() {
                          @Override
                          public void onAnimationEnd(Animator animation) {
                              readButton.animate().alpha(1.0f).start();
                              if (getContext() != null) {
                                  readButton.setText(getContext().getString(R.string.read_that_button));
                              }
                          }
                      }).start();
                      setArticlesRead(misRead);
                  } else {
                      misRead = true;
                      readButton.animate().alpha(0.2f).setListener(new AnimatorListenerAdapter() {
                          @Override
                          public void onAnimationEnd(Animator animation) {
                              readButton.animate().alpha(1.0f).start();
                              if (getContext() != null) {
                                  readButton.setText(getContext().getString(R.string.read_that_button_false));
                              }
                          }
                      }).start();
                      setArticlesRead(misRead);

                  }
              }
        });
        // return the inflated view
        return rootView;
    }

    /**
     * Helper fun to set the articles to persist for data graph in user fragment
     */
    private void setArticlesRead(boolean isRead) {
        mModelWiki.setRead(getContext(), wikiExtract, isRead);
        // grab the preferences
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        // get todays day
        Integer today = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK);

        // get the number of articles read
        int articlesRead = mPrefs.getInt(today.toString(), 0);

        SharedPreferences.Editor editor = mPrefs.edit();

        if (isRead) {
            // put the new number
            editor.putInt(today.toString(), articlesRead + 1);
            editor.apply();
        } else {
            if (articlesRead > 0) {
                editor.putInt(today.toString(), articlesRead - 1);
                editor.apply();
            }
        }
    }

    /**
     * Helper function to set toolbar to the Review Words title.
     */
    private void setToolbarTitle(View rootView) {
        // Grab the title of the toolbar.
        TextView toolbarTitle = rootView.findViewById(R.id.toolbar_title);
        // Set the title of the toolbar to the Reading tab.
        if (toolbarTitle != null) {
            toolbarTitle.setText(getString(R.string.bottom_sheet_toolbar_title_prefix, wikiExtract.title));
        }
    }

    /**
     * Helper fun to prepare the view model to observe
     * the live data object of the kanji and int range pair
     */
    private void prepareViewModel(final View rootView) {
        // Get the ViewModel for the Jisho Detail Fragment
        mModel = ViewModelProviders.of(this).get(JishoViewModel.class);

        // Create the observer which updates the UI. Whenever the data changes, the new pojo list
        // is fed through and the UI can be updated.
        Observer<ArrayList<Pair<Pair<Integer, Integer>, Kanji>>> observer =
                new Observer<ArrayList<Pair<Pair<Integer, Integer>, Kanji>>>() {
            @Override
            public void onChanged(@Nullable ArrayList<Pair<Pair<Integer, Integer>, Kanji>> pairs) {
                // Check for null since addAll() accepts non-null
                if (pairs != null && pairs.size() != 0) {
                    // Clear the list of the old pairs
                    mKanjis.clear();

                    // Add the received pairs list to the global list
                    mKanjis.addAll(pairs);

                    for (Pair<Pair<Integer, Integer>, Kanji> kanji : pairs){
                        if (mRootView.findViewWithTag(kanji.first.first.toString()) == null) {
                            // Create the furigana text view
                            TextView furiganaView = new TextView(getContext());
                            furiganaView.setTag(kanji.first.first.toString());
                            furiganaView.setVisibility(View.INVISIBLE);
                            // Set the text to the reading from the kanji pojo
                            furiganaView.setText(kanji.second.mReading);
                            // Set text view properties
                            furiganaView.setMaxLines(1);
                            furiganaView.setTextAppearance(getContext(), R.style.FuriganaStyle);
                            // Add the furigana text view to the constraint layout parent.
                            ((FrameLayout) mRootView.findViewById(R.id.furigana_parent)).addView(furiganaView);
                        }
                    }

                    // Set the clickable spans with the Kanjis
                    setSpannable(mKanjis, mRootView);

                    // update the spannable string to the text view
                    ((TextView) rootView.findViewById(R.id.detail_extract)).setText(spannableString);

                    if (mKanjis.size() == 0) {
                        Snackbar.make( rootView.findViewById(R.id.detail_fragment_parent_layout),
                                "No connection found to get readings.", Snackbar.LENGTH_SHORT).show();
                    }
                } else {
                    if (mKanjis.size() != 0) {
                        for (Pair<Pair<Integer, Integer>, Kanji> kanji : mKanjis){
                            if (mRootView.findViewWithTag(kanji.first.first.toString()) == null) {
                                // Create the furigana text view
                                TextView furiganaView = new TextView(getContext());
                                furiganaView.setTag(kanji.first.first.toString());
                                furiganaView.setVisibility(View.INVISIBLE);
                                // Set the text to the reading from the kanji pojo
                                furiganaView.setText(kanji.second.mReading);
                                // Set text view properties
                                furiganaView.setMaxLines(1);
                                furiganaView.setTextAppearance(getContext(), R.style.FuriganaStyle);
                                // Add the furigana text view to the constraint layout parent.
                                ((FrameLayout) mRootView.findViewById(R.id.furigana_parent)).addView(furiganaView);
                            }
                        }
                    } else {
                        Snackbar.make( rootView.findViewById(R.id.detail_fragment_parent_layout),
                                "No connection found to get readings.", Snackbar.LENGTH_SHORT).show();
                    }
                }
            }
        };

        // Make the api call to grab the furigana for each word in the extract spannable
        // Scan the extract text using the utils WordScanner class. Returns a list of
        // pair values of Kanjis and index range.
        mModel.loadKanjis(getContext(), wikiExtract);

        // Observe the LiveData in the view model which will be set to the kanji index pairs,
        // passing in the fragment as the LifecycleOwner and the observer created above.
        mModel.kanjis.observe(this, observer);
    }

    public static final class IntRef {
        public int element;
    }

    /**
     * Private helper fun to set the clickable spans to the text
     * in order to open the definition pop-up.
     */
    private void setSpannable(ArrayList<Pair<Pair<Integer, Integer>, Kanji>> kanjis, final View rootView) {
        TransitionManager.beginDelayedTransition((FrameLayout) rootView.findViewById(R.id.furigana_parent));
        // Iterate through the pairs to assign a span to the string
        for (final Pair<Pair<Integer, Integer>, Kanji> kanji : kanjis) {
            // Set the boolean object to see when it was clicked
            final int showFuriganaCase = 0;
            final int showExplanationCase = 1;
            final int hideEverythingCase = 2;

           final TextView furiganaView = mRootView.findViewWithTag(kanji.first.first.toString());

            setFuriganaView(rootView, kanji, furiganaView);

            // Set the initial number
            final IntRef clickedNumber = new IntRef();
            clickedNumber.element = showFuriganaCase;
            // Create a clickable span to set the onclick for a pop up
            ClickableSpan clickableSpan = new ClickableSpan() {

                /**
                 * Override onClick to open the pop up.
                 */
                @Override
                public void onClick(View widget) {
                    // Start a when loop to see what state is the clicked kanji in
                    switch (clickedNumber.element) {
                        case showFuriganaCase: {
                            // Kanji was clicked with furigana so we show explanation
                            hideCalloutBubble(rootView);
                            showFurigana(furiganaView);
                            // Set the state to show explanation
                            clickedNumber.element = showExplanationCase;
                            break;
                        }
                        case showExplanationCase: {
                            hideCalloutBubble(rootView);
                            //calculateLoadingCallout(mRootView, furiganaView, kanji);
                            showLoadingCallout(rootView, furiganaView, kanji);
                            // Send an async to get the word definition from jisho
                            setDefinitionViewModel(kanji, rootView, furiganaView);

                            // Set the state to hide everything
                            clickedNumber.element = hideEverythingCase;
                            break;
                        }
                        case hideEverythingCase: {
                            // Kanji was clicked with definition so we need to hide it
                            hideFurigana(furiganaView);
                            hideCalloutBubble(rootView);
                            hideLoadingCallout(rootView);

                            // Set the state back to show furigana
                            clickedNumber.element = showFuriganaCase;
                            break;
                        }
                    }
                }
            };

            // Set the clickable span to the span string word, with inclusive indexes.
            spannableString.setSpan(clickableSpan,
                    kanji.first.first,
                    kanji.first.second + 1,
                    SpannableString.SPAN_INCLUSIVE_INCLUSIVE);
        }
    }

    /**
     * Helper fun to set the view model to the jisho view model and the jisho repo
     * linked to that.
     */
    private void setDefinitionViewModel(final Pair<Pair<Integer, Integer>, Kanji> protoKanji, final View rootView,
                                        final TextView furiganaView) {
        // Create the observer which updates the UI. Whenever the data changes, the new pojo list
        // is fed through and the UI of the callout bubble can be updated.
        Observer<Pair<Integer, Kanji>> observer = new Observer<Pair<Integer, Kanji>>() {
            @Override
            public void onChanged(@Nullable Pair<Integer, Kanji> kanjiIdPair) {
                // Check for null since addAll() accepts non-null
                if (kanjiIdPair != null) {
                    setCalloutBubble(rootView, new Pair<>(protoKanji.first, kanjiIdPair.second),
                            furiganaView, kanjiIdPair.first);
                }
            }
        };

        // Make the api call to grab the furigana for each word in the extract spannable
        // Scan the extract text using the utils WordScanner class. Returns a list of
        // pair values of Kanjis and index range.
        mModel.getDefinitions(getContext(), protoKanji.second);

        // Observe the LiveData in the view model which will be set to the kanji index pairs,
        // passing in the fragment as the LifecycleOwner and the observer created above.
        mModel.kanji.observe(this, observer);
    }

    /**
     * Helper fun to add the furigana text view at the right location above the kanji.
     */
    private void setFuriganaView(final View rootView, final Pair<Pair<Integer, Integer>, Kanji> kanji,
                                 final TextView furiganaView) {
        // Calculate the offsets
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Pair<Float, Float> offsetPair = calculateFuriganaOffsets(mRootView, furiganaView, kanji);
                // Set the x and y coordinates to the furigana text view
                if (furiganaView != null) {
                    furiganaView.setX(offsetPair.first);
                    // Raise the furigana to be above the text. The padding in the extract text view affects
                    // the shift too.
                    furiganaView.setY(offsetPair.second);
                    // Hide it until it is clicked on.
                    hideFuriganaNoDelay(furiganaView);
                }
            }
        }, 1000);
    }

    /**
     * Helper fun to hide the furigana view
     */
    private void hideFurigana(final View furigana) {
        furigana.animate().setListener(new AnimatorListenerAdapter() {
            /**
             * Override animation end to set visibility.
             */
            @Override
            public void onAnimationEnd(Animator animation) {
                furigana.setVisibility(View.GONE);
            }
        }).alpha(0f).setStartDelay(500).start();
    }

    /**
     * Helper fun to hide the furigana view
     */
    private void hideFuriganaNoDelay(View furigana) {
        furigana.setAlpha(0f);
        furigana.setVisibility(View.GONE);
    }

    /**
     * Helper fun to show the furigana view
     */
    private void showFurigana(View furigana) {
        furigana.setVisibility(View.VISIBLE);
        furigana.animate().alpha(1f).setListener(null).start();
    }

    /**
     * Set the callout bubble
     */
    private void setCalloutBubble(final View rootView, final Pair<Pair<Integer, Integer>, Kanji> kanji,
                                  TextView furiganaView, final int kanjiId) {

        final LinearLayout calloutBubble = rootView.findViewById(R.id.callout_bubble);
        // Set the word title.
        ((TextView) calloutBubble.findViewById(R.id.callout_title)).setText(kanji.second.mWord);
        // Set the reading.
        ((TextView) calloutBubble.findViewById(R.id.callout_reading)).setText(kanji.second.mReading);
        // Set the tags
        setCalloutTags(kanji.second, rootView);
        // set the review button according to review boolean
        Kanji kanjiWord = kanji.second;
        if (kanjiWord != null) {
            calloutBubble.findViewById(R.id.callout_review_button).
                    setSelected(kanjiWord.mIsReview);

            if (kanjiWord.mIsReview) {
                ((ImageView) calloutBubble.
                        findViewById(R.id.callout_review_button)).setColorFilter(
                        ContextCompat.getColor(getContext(), R.color.colorAccent), PorterDuff.Mode.SRC_ATOP);
            } else {
                ((ImageView) calloutBubble.
                        findViewById(R.id.callout_review_button)).setColorFilter(
                        ContextCompat.getColor(getContext(), R.color.color500Grey), PorterDuff.Mode.SRC_ATOP);
            }

            final TextView detailsLink = calloutBubble.findViewById(R.id.callout_details_link);
            if (!kanjiWord.mUrl.isEmpty()) {
                // if there is jisho data show details link
                detailsLink.setVisibility(View.VISIBLE);
                // Set an onclick to open the jisho url, and add an animation for the alpha
                detailsLink.setAlpha(1.0f);
                // Set a listener to know when the alpha ends to return to 1.0f alpha
                detailsLink.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // set animation
                        detailsLink.animate().alpha(0.2f).
                                setDuration(500).setListener(new AnimatorListenerAdapter() {

                            @Override
                            public void onAnimationEnd(Animator animation, boolean isReverse) {
                                detailsLink.animate().alpha(1.0f).setDuration(500).start();
                            }
                        }).start();

                        // Set the url with an intent.
                        Uri webpage = Uri.parse(kanji.second.mUrl);
                        Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
                        if (intent.resolveActivity(getContext().getPackageManager()) != null) {
                            startActivity(intent);
                        }
                    }
                });
            } else {
                // if there isnt internet data, hide the link.
                detailsLink.setVisibility(View.INVISIBLE);
            }

            TextView defAlone = calloutBubble.findViewById(R.id.callout_definition_alone);
            TextView def1 = calloutBubble.findViewById(R.id.callout_definition_1);
            TextView def2 = calloutBubble.findViewById(R.id.callout_definition_2);

            // Set the definitions.
            switch (kanjiWord.mDefinitions.size()) {
                case 1: {
                    // There is only one definition so set the first and hide the second.
                    defAlone.setText(getContext().getString(R.string.definition_1_placeholder,
                            kanji.second.mDefinitions.get(0)));
                    // Set visibility
                    showOneDefinition(rootView);
                    break;
                }
                case 2: {
                    // There are more than 1 so add the two definitions.
                    defAlone.setText(getContext().getString(R.string.definition_1_placeholder,
                            kanji.second.mDefinitions.get(0)));
                    def1.setText(getContext().getString(R.string.definition_1_placeholder,
                            kanji.second.mDefinitions.get(0)));
                    def2.setText(getContext().getString(R.string.definition_2_placeholder,
                            kanji.second.mDefinitions.get(1)));
                    // Set visibility
                    showTwoDefinitions(rootView);
                    break;
                }
                default: {
                    // There is no definition available
                    String def = getContext().getString(R.string.no_definition);
                    // If there is no kanji loaded at all,
                    if (kanjiWord.mUrl.isEmpty()) {
                       defAlone.setText(def);
                    }
                    // Set visibility
                    showOneDefinition(rootView);
                    break;
                }
            }
        }

        calloutBubble.findViewById(R.id.callout_review_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!calloutBubble.findViewById(R.id.callout_review_button).isSelected()) {
                    // update the database on worker thread
                    kanjiDao.updateReviewKanji(getContext(), kanjiId, true);
                    // set selected state
                    calloutBubble.findViewById(R.id.callout_review_button).setSelected(true);
                    ((ImageView) calloutBubble.findViewById(R.id.callout_review_button)).setColorFilter(
                            ContextCompat.getColor(getContext(), R.color.colorAccent), PorterDuff.Mode.SRC_ATOP);
                } else {
                    // update the database on worker thread
                    kanjiDao.updateReviewKanji(getContext(), kanjiId, false);
                    // set the drawable to be unselected state
                    calloutBubble.findViewById(R.id.callout_review_button).setSelected(false);
                    ((ImageView) calloutBubble.findViewById(R.id.callout_review_button)).setColorFilter(
                            ContextCompat.getColor(getContext(), R.color.color500Grey), PorterDuff.Mode.SRC_ATOP);
                }
            }
        });

        // Kanji was clicked with furigana so we show explanation
        setCalloutBubblePosition(rootView, kanji, furiganaView);
    }


    /**
     * Set the tags
     */
    private void setCalloutTags(Kanji kanji, View rootView) {
        final LinearLayout calloutBubble = rootView.findViewById(R.id.callout_bubble);
        // Check for the common tag
        if (kanji != null) {
            if (kanji.mIsCommon) {
                calloutBubble.findViewById(R.id.callout_common_tag).setVisibility(View.VISIBLE);
            } else {
                calloutBubble.findViewById(R.id.callout_common_tag).setVisibility(View.GONE);
            }
        }

        TextView jlptTag = calloutBubble.findViewById(R.id.callout_jlpt_tag);
        // Check the jlpt level and set color and text
        if (kanji != null) {
            switch (kanji.mJlptTag) {
                case -1: jlptTag.setVisibility(View.GONE);
                case 1: {
                    jlptTag.setVisibility(View.VISIBLE);
                    jlptTag.setText(getString(R.string.JLPTN1));
                    jlptTag.setBackgroundTintList(new ColorStateList(new int[][]{new int[1]},
                            new int[]{ContextCompat.getColor(getContext(), R.color.colorJLPTN1)}));
                    break;
                }
                case 2: {
                    jlptTag.setVisibility(View.VISIBLE);
                    jlptTag.setText(getString(R.string.JLPTN2));
                    jlptTag.setBackgroundTintList(new ColorStateList(new int[][]{new int[1]},
                            new int[]{ContextCompat.getColor(getContext(), R.color.colorJLPTN2)}));
                    break;
                }
                case 3: {
                    jlptTag.setVisibility(View.VISIBLE);
                    jlptTag.setText(getString(R.string.JLPTN3));
                    jlptTag.setBackgroundTintList(new ColorStateList(new int[][]{new int[1]},
                            new int[]{ContextCompat.getColor(getContext(), R.color.colorJLPTN3)}));
                    break;
                }
                case 4: {
                    jlptTag.setVisibility(View.VISIBLE);
                    jlptTag.setText(getString(R.string.JLPTN4));
                    jlptTag.setBackgroundTintList(new ColorStateList(new int[][]{new int[1]},
                            new int[]{ContextCompat.getColor(getContext(), R.color.colorJLPTN4)}));
                    break;
                }
                case 5: {
                    jlptTag.setVisibility(View.VISIBLE);
                    jlptTag.setText(getString(R.string.JLPTN5));
                    jlptTag.setBackgroundTintList(new ColorStateList(new int[][]{new int[1]},
                            new int[]{ContextCompat.getColor(getContext(), R.color.colorJLPTN5)}));
                    break;
                }
            }
        }
    }

    /**
     * Helper fun to set the text view visibility of callout bubble to one
     */
    private void showOneDefinition(View rootView) {
        final LinearLayout calloutBubble = rootView.findViewById(R.id.callout_bubble);
        TextView defAlone = calloutBubble.findViewById(R.id.callout_definition_alone);
        TextView def1 = calloutBubble.findViewById(R.id.callout_definition_1);
        TextView def2 = calloutBubble.findViewById(R.id.callout_definition_2);

        defAlone.setVisibility(View.VISIBLE);
        def1.setVisibility(View.GONE);
        def2.setVisibility(View.GONE);
    }

    /**
     * Helper fun to set the text view visibility of callout bubble to two
     */
    private void showTwoDefinitions(View rootView) {
        final LinearLayout calloutBubble = rootView.findViewById(R.id.callout_bubble);
        TextView defAlone = calloutBubble.findViewById(R.id.callout_definition_alone);
        TextView def1 = calloutBubble.findViewById(R.id.callout_definition_1);
        TextView def2 = calloutBubble.findViewById(R.id.callout_definition_2);

        defAlone.setVisibility(View.GONE);
        def1.setVisibility(View.VISIBLE);
        def2.setVisibility(View.VISIBLE);
    }

    /**
     * Helper fun to set the callout bubble in the right place
     */
    private void setCalloutBubblePosition(final View rootView, final Pair<Pair<Integer, Integer>, Kanji> kanji,
                                          final TextView furiganaView) {
        // Set the padding bottom
        rootView.findViewById(R.id.detail_padding_bottom).setVisibility(View.GONE);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Calculate the bubble offset
                calculateCalloutBubbleOffset(mRootView, furiganaView, kanji);
            }
        }, 1000);
    }

    /**
     * Helper fun to show the loading callout
     */
    private void showLoadingCallout(View rootView, TextView furiganaView, Pair<Pair<Integer, Integer>, Kanji> kanji) {
        calculateLoadingCallout(rootView, furiganaView, kanji);
        rootView.findViewById(R.id.callout_bubble_loading).setVisibility(View.VISIBLE);
    }

    /**
     * Helper fun to hide the loading callout
     */
    private void hideLoadingCallout(View rootView) {
        rootView.findViewById(R.id.callout_bubble_loading).setVisibility(View.INVISIBLE);
    }

    /**
     * Helper fun to calculate the loading callout bubble.
     */
    private void calculateLoadingCallout(View rootView, TextView furiganaView, Pair<Pair<Integer, Integer>, Kanji> kanji) {
        // Get the text view bounds height.
        Rect bounds = new Rect();
        furiganaView.getPaint().getTextBounds(kanji.second.mReading,
                0, kanji.second.mReading.length(), bounds);
        int furiganaWidth = bounds.width();

        // Calculate the offset of furigana
        Pair<Float, Float> offsetPair = calculateFuriganaOffsets(rootView, furiganaView, kanji);

        // Offset the callout bubble subtracting his height
        rootView.findViewById(R.id.callout_bubble_loading).
                setY(offsetPair.second - rootView.findViewById(R.id.callout_bubble_loading).getHeight());
        rootView.findViewById(R.id.callout_bubble_loading).
                setX(offsetPair.first - (rootView.findViewById(R.id.callout_bubble_loading).getWidth() / 2) +
                        (furiganaWidth / 2));
    }

    /**
     * Private fun to calculate the callout bubble offset in the screen
     */
    private void calculateCalloutBubbleOffset(View rootView, TextView furiganaView,
                                              Pair<Pair<Integer, Integer>, Kanji> kanji) {
        LinearLayout calloutBubble = rootView.findViewById(R.id.callout_bubble);
        // Set the middle bubble svg initially
        calloutBubble.setBackground(getContext().getDrawable(R.drawable.ic_callout_bubble_middle_shadow));
        setPaddingForBottomCallout(rootView, 36f);
        // Set the bottom bubble margin to be gone
        calloutBubble.findViewById(R.id.callout_title_margin).setVisibility(View.GONE);
        setMaxWidth(MiscUtils.getUtils().pxFromDp(getContext(), 220f), rootView);

        Kanji kanjiWord = kanji.second;
        if (kanjiWord != null) {
            // Get the text view bounds height.
            Rect bounds = new Rect();
            furiganaView.getPaint().getTextBounds(kanji.second.mReading, 0, kanjiWord.mReading.length(), bounds);
            int furiganaWidth = bounds.width();

            // Get the layout of the text view
            Layout textViewLayout = ((TextView) mRootView.findViewById(R.id.detail_extract)).getLayout();

            // Calculate the offset of furigana
            Pair<Float, Float> offsetPair = calculateFuriganaOffsets(rootView, furiganaView, kanji);
            // Offset the callout bubble subtracting his height
            calloutBubble.setY(offsetPair.second - calloutBubble.getHeight());
            calloutBubble.setX(offsetPair.first - (calloutBubble.getWidth() / 2) + (furiganaWidth / 2));

            // Grab the width and padding.
            int calloutWidth = calloutBubble.getMeasuredWidth();

            // Get metrics of screen device.
            DisplayMetrics displayMetrics = new DisplayMetrics();
            this.getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int screenWidth = displayMetrics.widthPixels;

            // Check if the callout bubble is out of the screen
            if (calloutBubble.getX() < 0) {
                // If it is out of the start side, change bubble to start svg and move to the edge with padding.
                // Check first if we need the bottom variant
                if (calloutBubble.getY() < 0) {
                    calloutBubble.setBackground(
                            getContext().getDrawable(R.drawable.ic_callout_bubble_start_bottom_shadow));

                    // Lower the callout to be under the character.
                    int lineNumber = textViewLayout.getLineForOffset(kanji.first.first);
                    int lineBottom = textViewLayout.getLineBottom(lineNumber + 1);

                    calloutBubble.setY(lineBottom);
                    calloutBubble.findViewById(R.id.callout_title_margin).setVisibility(View.INVISIBLE);

                    // Set the padding to be less
                    setPaddingForBottomCallout(rootView, 20f);
                } else {
                    calloutBubble.setBackground(
                            getContext().getDrawable(R.drawable.ic_callout_bubble_start_shadow));
                    calloutBubble.findViewById(R.id.callout_title_margin).setVisibility(View.GONE);
                }
                // If it is from the start, set the x to the offset calculated.
                calloutBubble.setX(offsetPair.first);
            } else if ((calloutBubble.getX() + calloutWidth) > screenWidth) {
                // If it is out of the end side, change bubble to end svg and move to the edge with padding.
                // If it is not make end x the last char of the start line plus the one charwidth
                // Span of one character
                float charWidth =
                        textViewLayout.getPrimaryHorizontal(1) - textViewLayout.getPrimaryHorizontal(0);

                if (calloutBubble.getY() < 0) {
                    calloutBubble.setBackground(
                            getContext().getDrawable(R.drawable.ic_callout_bubble_end_bottom_shadow));

                    // Lower the callout to be under the character.
                    int lineNumber = textViewLayout.getLineForOffset(kanji.first.first);
                    int lineBottom = textViewLayout.getLineBottom(lineNumber + 1);

                    calloutBubble.setY(lineBottom);
                    calloutBubble.findViewById(R.id.callout_title_margin).setVisibility(View.INVISIBLE);

                    // Set the padding to be less
                    setPaddingForBottomCallout(rootView, 20f);
                } else {
                    calloutBubble.setBackground(getContext().getDrawable(R.drawable.ic_callout_bubble_end_shadow));
                    calloutBubble.findViewById(R.id.callout_title_margin).setVisibility(View.GONE);
                }
                // Set the callout to be at the end of the text + some char widths to center it.
                calloutBubble.setX(offsetPair.first - calloutWidth + (furiganaWidth / 2) + (charWidth / 1.2f));
            }

            // Check if the bubble goes off the layout on top and is the middle callout
            if (calloutBubble.getY() < 0) {
                // If it does set the bottom variant of the callout.
                calloutBubble.setBackground(getContext().getDrawable(R.drawable.ic_callout_bubble_middle_bottom_shadow));

                // Lower the callout to be under the character.
                int lineNumber = textViewLayout.getLineForOffset(kanji.first.first);
                int lineBottom = textViewLayout.getLineBottom(lineNumber + 1);

                calloutBubble.setY(lineBottom);
                calloutBubble.findViewById(R.id.callout_title_margin).setVisibility(View.INVISIBLE);

                // Set the padding to be less
                setPaddingForBottomCallout(rootView, 20f);
            }

            // Check if callout is overflows at the bottom.
            if (calloutBubble.getY() + calloutBubble.getHeight() > rootView.findViewById(R.id.furigana_parent).getHeight()) {
                float paddingHeight = (calloutBubble.getY() + calloutBubble.getHeight()) -
                        rootView.findViewById(R.id.furigana_parent).getHeight() +
                        MiscUtils.getUtils().pxFromDp(getContext(), 16f);

                ViewGroup.LayoutParams params =
                        rootView.findViewById(R.id.detail_padding_bottom).getLayoutParams();
                params.height = ((int) paddingHeight);
                params.width = ActionBar.LayoutParams.MATCH_PARENT;
                rootView.findViewById(R.id.detail_padding_bottom).setLayoutParams(params);
                rootView.findViewById(R.id.detail_padding_bottom).setVisibility(View.VISIBLE);
            }

            /*
            Calculate whether the new callout bubble is out of the screen width because of the text
            and reduce the max width of the text to change this.
             */
            int padding = rootView.findViewById(R.id.detail_extract).getPaddingStart();
            if (calloutBubble.getX() < 0) {
                // The bubble is out of the start so calculate what the max width should be
                // to be in plus padding 16dp
                float endXCoordinate = calloutBubble.getX() + calloutWidth;

                float maxWidth = (endXCoordinate - padding) - (padding * 2);
                // Set the max width of the text views
                setMaxWidth(maxWidth, rootView);
            } else if ((calloutBubble.getX() + calloutWidth) > screenWidth) {
                // The bubble is out of the end so calculate what the max width should be
                // to be in minus padding 16dp
                int endXCoordinate = screenWidth - padding;

                float maxWidth = (endXCoordinate - calloutBubble.getX()) - (padding * 2);
                // Set the max width of the text views
                setMaxWidth(maxWidth, rootView);
            }
            // Hide the loading spinner
            hideLoadingCallout(rootView);
            // Show the callout
            showCalloutBubble(rootView);

            if (furiganaView.getVisibility() != View.VISIBLE) {
                hideCalloutBubble(rootView);
            }
        }
    }

    /**
     * Helper fun to set the max width of the text views to fit the screen.
     */
    private void setMaxWidth(float maxWidth, View rootView) {
        LinearLayout calloutBubble = rootView.findViewById(R.id.callout_bubble);
        // Set the max width of the text views
        ((TextView) calloutBubble.findViewById(R.id.callout_definition_1)).setMaxWidth(((int) maxWidth));
        ((TextView) calloutBubble.findViewById(R.id.callout_definition_2)).setMaxWidth(((int) maxWidth));
        ((TextView) calloutBubble.findViewById(R.id.callout_definition_alone)).setMaxWidth(((int) maxWidth));

    }

    /**
     * Helper fun to set the padding bottom of the callout if it changes to a bottom bubble.
     */
    private void setPaddingForBottomCallout(View rootView, Float bottomPadding) {
        LinearLayout calloutBubble = rootView.findViewById(R.id.callout_bubble);

        calloutBubble.findViewById(R.id.callout_details_link).setPaddingRelative(
                MiscUtils.getUtils().pxFromDp(getContext(), ((Float) 16f)).intValue(),
                MiscUtils.getUtils().pxFromDp(getContext(), ((Float) 4f)).intValue(),
                MiscUtils.getUtils().pxFromDp(getContext(), ((Float) 16f)).intValue(),
                MiscUtils.getUtils().pxFromDp(getContext(), ((Float) bottomPadding)).intValue());
    }

    /**
     * Helper fun to show the callout bubble
     */
    private void showCalloutBubble(View rootView) {
        LinearLayout calloutBubble = rootView.findViewById(R.id.callout_bubble);

        calloutBubble.animate().alpha(1f).setListener(null).start();
        calloutBubble.setVisibility(View.VISIBLE);
    }

    /**
     * Helper fun to hide the callout bubble
     */
    private void hideCalloutBubble(final View rootView) {
        final LinearLayout calloutBubble = rootView.findViewById(R.id.callout_bubble);

        calloutBubble.animate().setListener(new AnimatorListenerAdapter() {
            /**
             * Override animation end to set visibility.
             */
            @Override
            public void onAnimationEnd(Animator animation) {
                calloutBubble.setVisibility(View.INVISIBLE);
                // Set the padding bottom
                rootView.findViewById(R.id.detail_padding_bottom).setVisibility(View.GONE);
            }
        }).alpha(0f).start();
    }


    private Pair<Float, Float> calculateFuriganaOffsets(View rootView, TextView furiganaView,
                                         Pair<Pair<Integer, Integer>, Kanji> kanji) {
        // Get the text view bounds height
        Rect bounds = new Rect();
        if (furiganaView != null) {
            furiganaView.getPaint().getTextBounds(kanji.second.mReading,
                    0, kanji.second.mReading.length(), bounds);
        }
        Integer furiganaHeight = bounds.height();
        Integer furiganaWidth = bounds.width();

        TextView detailExtract = (TextView) mRootView.findViewById(R.id.detail_extract);
        TextView detailTitle = (TextView) mRootView.findViewById(R.id.detail_title);

        // Get the layout of the text view
        Layout textViewLayout = detailExtract.getLayout();

        Double offsetY = 0D;
        double offsetX = 0D;
        if (textViewLayout != null) {
            // Get the line number of the current kanji
            Integer lineNumber = textViewLayout.getLineForOffset(kanji.first.first);
            // Get the top of the line (y coordinates) + the padding added to the text views to fit
            // top furigana
            Integer startYCoordinates =
                    textViewLayout.getLineTop(lineNumber) + detailExtract.getPaddingTop() +
                            detailTitle.getMeasuredHeight() + detailTitle.getLayout().getTopPadding() +
                            detailTitle.getLayout().getBottomPadding();
        /*
        Calculate the X coordinate to offset the furigana view. We also need to calculate whether
        the kanji spans two lines first, and change the calculation to be only in the first line.
         */
            // Line of the first kanji
            Integer startLine = textViewLayout.getLineForOffset(kanji.first.first);
            // Line of the first kanji + 1
            Integer startLine2 = textViewLayout.getLineForOffset(kanji.first.first + 1);
            // Line of the last kanji
            Integer endLine = textViewLayout.getLineForOffset(kanji.first.second + 1);

            // Span of one character
            Float charWidth = textViewLayout.getPrimaryHorizontal(1) - textViewLayout.getPrimaryHorizontal(0);

            // Init the start and end x coordinates
            Float startXCoordinates;
            Float endXCoordinates;

            // Check if the start line is the same as the kanji + 1 start line
            if (startLine.equals(startLine2)) {
                // If it is the start x will be kanji + 1
                startXCoordinates = textViewLayout.getPrimaryHorizontal(kanji.first.first + 1);
            } else {
                // If not, the start x will be the first kanji
                startXCoordinates = textViewLayout.getPrimaryHorizontal(kanji.first.first);
            }

            // Check if the start line is the same as the end line
            if (startLine.equals(endLine)) {
                // If it is, make end x the last kanji + 1
                endXCoordinates = textViewLayout.getPrimaryHorizontal(kanji.first.second + 1);
            } else {
                // If it is not make end x the last char of the start line plus the one charwidth
                int textOffset = textViewLayout.getLineEnd(startLine) - 1;
                if (kanji.first.first.equals(kanji.first.second)) {
                    charWidth *= 2;
                }

                endXCoordinates = textViewLayout.getPrimaryHorizontal(textOffset) + charWidth;
            }

            // Get the mid coordinates of the kanji and add a
            // factor of mid of the text bound width to center it.
            offsetX = startXCoordinates + ((endXCoordinates - startXCoordinates) / 2) - (furiganaWidth / 2.6);

        /*
         * Check if furigana is off screen.
         */
            // Get metrics of screen device.
            DisplayMetrics displayMetrics = new DisplayMetrics();
            if (getActivity() != null) {
                getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            }
            Integer screenWidth = displayMetrics.widthPixels;

            // If the furigana goes off the end screen
            if (offsetX + furiganaWidth > screenWidth) {
                offsetX =  screenWidth - furiganaWidth - (detailExtract.getPaddingTop() / 2);
            } else if (offsetX < 0) {
                offsetX = screenWidth + (detailExtract.getPaddingTop() / 2);
            }

            // Get the desired Y coordinate, a factor of the bounds height as a margin
            offsetY = startYCoordinates - furiganaHeight - (furiganaHeight / 1.7);
        }

        return new Pair<>(((Double) offsetX).floatValue(), offsetY.floatValue());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        JishoRepository.executor.shutdown();
        try {
            if (!JishoRepository.executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                JishoRepository.executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            JishoRepository.executor.shutdownNow();

        }
    }
}
