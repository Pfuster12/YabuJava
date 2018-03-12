package com.yabu.android.yabujava.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.yabu.android.yabujava.R;
import java.util.ArrayList;
import jsondataclasses.Kanji;
import sql.KanjisSQLDao;


/**
 * Recycler View custom adapter for review words with a header,
 * footer and item view holder. Passes the adapter data, the activity context
 * and the listener callback to the activity.
 */
public class ReviewRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ArrayList<Pair<Integer, Kanji>> mReviewKanjis;
    private Context mContext;
    private OnEmptyListener mNotifyListener;
    private View mRootView;

    /*
    Types of view holders to return for the function itemViewType
    */
    private static final int headerType = 100;
    private static final int itemType = 101;
    private static final int footerType = 102;

    // init a Kanji Dao
    KanjisSQLDao kanjiDao = KanjisSQLDao.getInstance();

    interface OnEmptyListener {
        void onEmptyNotify(boolean isEmpty);
    }

    public ReviewRecyclerViewAdapter(ArrayList<Pair<Integer, Kanji>> reviewKanjis, Context context,
                                     OnEmptyListener notifyListener, View fragmentRootView) {
        mReviewKanjis = reviewKanjis;
        mContext = context;
        mNotifyListener = notifyListener;
        mRootView = fragmentRootView;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Start a return when condition with the type int.
        switch (viewType) {
            // If header type inflate the header resource.
            case headerType: return new ReviewRecyclerViewAdapter.HeaderViewHolder(
                    inflateViewHolder(R.layout.reading_heading, parent));
            // If item type inflate the item resource.
            case itemType: return new ReviewRecyclerViewAdapter.ReviewWordItemViewHolder(
                    inflateViewHolder(R.layout.review_list_item, parent), mReviewKanjis);
            // If footer type inflate the footer resource.
            case footerType: return new ReviewRecyclerViewAdapter.FooterViewHolder(
                    inflateViewHolder(R.layout.review_footer, parent));
            // Else return the item layout as a default.
            default: return new ReviewRecyclerViewAdapter.ReviewWordItemViewHolder(
                    inflateViewHolder(R.layout.reading_list_item, parent), mReviewKanjis);
        }
    }

    /**
     * Helper function to inflate a layout resource.
     */
    private View inflateViewHolder(int layoutId, ViewGroup parent) {
        return LayoutInflater.from(mContext).inflate(layoutId, parent, false);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        // Start a when condition with the view type int.
        switch (getItemViewType(position)) {
            // If header type then bind header vectors
            case headerType: {
                break;
            } // Do bind operations here
            // If item type then bind texts and images with wikiExtracts pojo.
            case itemType: {
                // Cast the holder to a reading item.
                ReviewRecyclerViewAdapter.ReviewWordItemViewHolder itemViewHolder = ((ReviewWordItemViewHolder) holder);
                // Bind the views.
                bindListItemHolder(itemViewHolder, position);
                break;
            }
            // If footer type bind footer texts and vectors.
            case footerType: {
                break;
            }// Do bind operations here
        }
    }

    /**
     * Helper function to bind extract properties to the view holder.
     */
    private void bindListItemHolder(final ReviewRecyclerViewAdapter.ReviewWordItemViewHolder itemViewHolder,
                                    final Integer position) {
        // Position is minus 1 because of the header
        int currentPosition = position - 1;
        // Extract current extract
        final Pair<Integer, Kanji> currentReviewKanjiPair = mReviewKanjis.get(currentPosition);
        // Set the divider
        itemViewHolder.divider.setVisibility(View.VISIBLE);
        // Set title.
        itemViewHolder.word.setText(currentReviewKanjiPair.second.mWord);
        // Set text.
        itemViewHolder.reading.setText(currentReviewKanjiPair.second.mReading);
        // set the definitions
        switch (currentReviewKanjiPair.second.mDefinitions.size()) {
            case 0: {
                itemViewHolder.definition1.setText(mContext.getString(R.string.no_definition));
                itemViewHolder.definition2.setVisibility(View.GONE);
                break;
            }
            case 1: {
                itemViewHolder.definition1.setText(mContext.getString(R.string.definition_1_placeholder, currentReviewKanjiPair.second.mDefinitions.get(0)));
                itemViewHolder.definition2.setVisibility(View.GONE);
                break;
            }
            case 2: {
                itemViewHolder.definition2.setVisibility(View.VISIBLE);
                itemViewHolder.definition1.setText(
                        mContext.getString(R.string.definition_1_placeholder, currentReviewKanjiPair.second.mDefinitions.get(0)));
                itemViewHolder.definition2.setText(
                        mContext.getString(R.string.definition_2_placeholder, currentReviewKanjiPair.second.mDefinitions.get(1)));
                break;
            }
        }

        // Check for the common tag
        if (currentReviewKanjiPair.second.mIsCommon) {
            itemViewHolder.commonTag.setVisibility(View.VISIBLE);
        } else {
            itemViewHolder.commonTag.setVisibility(View.GONE);
        }

        // Check the jlpt level and set color and text
        switch (currentReviewKanjiPair.second.mJlptTag) {
            case -1: itemViewHolder.jlptTag.setVisibility(View.GONE);
            case 1: {
                itemViewHolder.jlptTag.setVisibility(View.VISIBLE);
                itemViewHolder.jlptTag.setText(mContext.getString(R.string.JLPTN1));
                itemViewHolder.jlptTag.setBackgroundTintList(new ColorStateList(new int[][]{new int[1]},
                        new int[]{ContextCompat.getColor(mContext, R.color.colorJLPTN1)}));
                break;
            }
            case 2: {
                itemViewHolder.jlptTag.setVisibility(View.VISIBLE);
                itemViewHolder.jlptTag.setText(mContext.getString(R.string.JLPTN2));
                itemViewHolder.jlptTag.setBackgroundTintList(new ColorStateList(new int[][]{new int[1]},
                        new int[]{ContextCompat.getColor(mContext, R.color.colorJLPTN2)}));
                break;
            }
            case 3: {
                itemViewHolder.jlptTag.setVisibility(View.VISIBLE);
                itemViewHolder.jlptTag.setText(mContext.getString(R.string.JLPTN3));
                itemViewHolder.jlptTag.setBackgroundTintList(new ColorStateList(new int[][]{new int[1]},
                        new int[]{ContextCompat.getColor(mContext, R.color.colorJLPTN3)}));
                break;
            }
            case 4: {
                itemViewHolder.jlptTag.setVisibility(View.VISIBLE);
                itemViewHolder.jlptTag.setText(mContext.getString(R.string.JLPTN4));
                itemViewHolder.jlptTag.setBackgroundTintList(new ColorStateList(new int[][]{new int[1]},
                        new int[]{ContextCompat.getColor(mContext, R.color.colorJLPTN4)}));
                break;
            }
            case 5: {
                itemViewHolder.jlptTag.setVisibility(View.VISIBLE);
                itemViewHolder.jlptTag.setText(mContext.getString(R.string.JLPTN5));
                itemViewHolder.jlptTag.setBackgroundTintList(new ColorStateList(new int[][]{new int[1]},
                        new int[]{ContextCompat.getColor(mContext, R.color.colorJLPTN5)}));
                break;
            }
        }

        if (!currentReviewKanjiPair.second.mUrl.isEmpty()) {
            // if there is jisho data show details link
            itemViewHolder.detailsLink.setVisibility(View.VISIBLE);
            // Set an onclick to open the jisho url, and add an animation for the alpha
            itemViewHolder.detailsLink.setAlpha(1.0f);
            // Set a listener to know when the alpha ends to return to 1.0f alpha
            itemViewHolder.detailsLink.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // set animation
                    itemViewHolder.detailsLink.animate().alpha(0.2f)
                            .setDuration(500).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            itemViewHolder.detailsLink.animate().alpha(1.0f)
                                    .setDuration(500).start();
                        }
                    }).start();
                    // Set the url with an intent.
                    Uri webpage = Uri.parse(currentReviewKanjiPair.second.mUrl);
                    Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
                    if (intent.resolveActivity(mContext.getPackageManager()) != null) {
                        mContext.startActivity(intent);
                    }
                }
            });
        }

        if (position == mReviewKanjis.size()) {
            itemViewHolder.divider.setVisibility(View.INVISIBLE);
        }

        itemViewHolder.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeItem(itemViewHolder.getAdapterPosition(), currentReviewKanjiPair);

                Snackbar.make(mRootView.findViewById(R.id.review_recycler_parent),
                        mContext.getString(R.string.snackbar_word_placeholder,
                                currentReviewKanjiPair.second.mWord), Snackbar.LENGTH_LONG)
                        .setAction(mContext.getString(R.string.undo_snackbar), new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                restoreItem(currentReviewKanjiPair, position);
                            }
                        }).show();
            }
        });
    }

    /**
     * Helper fun to remove item from the recycler view.
     */
    private void removeItem(Integer position, Pair<Integer, Kanji> currentKanji) {
        kanjiDao.updateReviewKanji(mContext, currentKanji.first, false);
        // delete the item at the position swiped
        mReviewKanjis.remove(position - 1);
        // notify the removal for animation purposes + 1 considering header
        notifyItemRemoved(position);
        if (mReviewKanjis.size() == 0) {
            mNotifyListener.onEmptyNotify(true);
        }
    }

    /**
     * Helper fun to add item to the recycler view.
     */
    void restoreItem(Pair<Integer, Kanji> currentKanji, Integer position) {
        kanjiDao.updateReviewKanji(mContext, currentKanji.first, true);
        mReviewKanjis.add(position - 1, currentKanji);
        // notify item added by position
        notifyItemInserted(position);
        mNotifyListener.onEmptyNotify(false);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return headerType;
        } else if (1 <= position && position <= mReviewKanjis.size()) {
            return itemType;
        } else if (position == mReviewKanjis.size() + 1) {
            return footerType;
        } else {
            return itemType;
        }
    }

    @Override
    public int getItemCount() {
        // Return the lists size with header and footer.
        if (mReviewKanjis != null) {
            return mReviewKanjis.size() + 1 + 1;
        } else {
            return 0;
        }
    }


    /**
     * In-class list item view holder implementation with on click function.
     */
    class ReviewWordItemViewHolder extends RecyclerView.ViewHolder {

        TextView word;
        TextView reading;
        TextView definition1;
        TextView definition2;
        TextView detailsLink;
        TextView jlptTag;
        TextView commonTag;
        View divider;
        ImageView delete;


        public ReviewWordItemViewHolder(View itemView, ArrayList<Pair<Integer, Kanji>> reviewKanjis) {
            super(itemView);
            word = itemView.findViewById(R.id.review_title);
            reading = itemView.findViewById(R.id.review_reading);
            definition1 = itemView.findViewById(R.id.review_definition_1);
            definition2 = itemView.findViewById(R.id.review_definition_2);
            detailsLink = itemView.findViewById(R.id.review_details_link);
            jlptTag = itemView.findViewById(R.id.review_jlpt_tag);
            commonTag = itemView.findViewById(R.id.review_common_tag);
            divider = itemView.findViewById(R.id.grey_line_divider);
            delete = itemView.findViewById(R.id.review_delete_button);
        }
    }


    /**
     * In-class header view holder implementation.
     */
    private class HeaderViewHolder extends RecyclerView.ViewHolder {

        public HeaderViewHolder(View itemView) {
            super(itemView);
        }
    }

    /**
     * In-class footer view holder implementation.
     */
    private class FooterViewHolder extends RecyclerView.ViewHolder {

        public FooterViewHolder(View itemView) {
            super(itemView);
        }
    }
}
