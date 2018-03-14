package com.yabu.android.yabujava.ui;

import android.content.Context;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.yabu.android.yabujava.R;

import java.util.ArrayList;

import jsondataclasses.WikiExtract;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

/**
 * Recycler View custom adapter with a header, footer and item view holder. Passes the adapter
 * data, the activity context and the listener callback to the activity.
 */
public class ReadingRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ReadingOnClick mListener;
    private Context mContext;
    private static ArrayList<WikiExtract> mWikiExtracts;
    /*
    Types of view holders to return for the function itemViewType
     */
    private static final int headerType = 100;
    private static final int itemType = 101;
    private static final int footerType = 102;
    private static final int adType = 103;

    interface ReadingOnClick {
        void readingOnClick(WikiExtract extract);
    }

    public ReadingRecyclerViewAdapter(ArrayList<WikiExtract> wikiExtracts,
                                      Context context, ReadingOnClick onClick) {
        mListener = onClick;
        mContext = context;
        mWikiExtracts = wikiExtracts;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case headerType: return new HeaderViewHolder(inflateViewHolder(R.layout.reading_heading, parent));
            case itemType: return new ReadingItemViewHolder(inflateViewHolder(R.layout.reading_list_item, parent),
                    mWikiExtracts, mListener);
            case footerType: return new FooterViewHolder(inflateViewHolder(R.layout.reading_footer, parent));
            case adType: return new AdItemViewHolder(inflateViewHolder(R.layout.reading_ad_item, parent));
            default: return new ReadingItemViewHolder(inflateViewHolder(R.layout.reading_list_item, parent),
                    mWikiExtracts, mListener);
        }
    }

    /**
     * Helper function to inflate a layout resource.
     */
    private View inflateViewHolder(int layoutId,ViewGroup parent) {
        return LayoutInflater.from(mContext).inflate(layoutId, parent, false);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        // Start a when condition with the view type int.
        switch (getItemViewType(position)) {
            // If header type then bind header vectors
            case headerType:  {}// Do bind operations here
            break;
            // If item type then bind texts and images with wikiExtracts pojo.
            case itemType: {
                // Cast the holder to a reading item.
                ReadingItemViewHolder itemViewHolder = (ReadingItemViewHolder) holder;
                // Bind the views.
                bindListItemHolder(itemViewHolder, position);
                break;
            }
            case adType: {
                // Cast the holder as an ad item holder
                AdItemViewHolder adViewHolder = (AdItemViewHolder) holder;
                // Load ad and bind
                bindAdHolder(adViewHolder);
                break;
            }
            // If footer type bind footer texts and vectors.
            case footerType: {
                break;
            }// Do bind operations here
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return headerType;
        } else if (position >= 1 && position <= 5) {
            return itemType;
        } else if (position == 6) {
            return adType;
        } else if (position >= 7 && position <= mWikiExtracts.size() + 1) {
            return itemType;
        } else if(position == mWikiExtracts.size() + 1 + 1) {
            return footerType;
        } else {
            return itemType;
        }
    }

    /**
     * Helper function to bind extract properties to the view holder.
     */
    private void bindListItemHolder(ReadingItemViewHolder itemViewHolder, int position) {
        // Position is minus 1 because of the header
        int currentPosition = position - 1;
        if (0 <= position && position <= 5) {
            currentPosition = position - 1;
        } else if (7 <= position && position <= mWikiExtracts.size() + 1) {
            currentPosition = position - 2;
        }
        if (mWikiExtracts.size() != 0) {
            // Extract current extract
            WikiExtract currentExtract = mWikiExtracts.get(currentPosition);
            // Set title.
            itemViewHolder.title.setText(currentExtract.title);
            // Set text.
            itemViewHolder.extract.setText(currentExtract.extract);
            // Set the image views to gray scale
            ColorMatrix matrix = new ColorMatrix();
            matrix.setSaturation(0f);
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
            if (currentExtract.thumbnail != null) {
                itemViewHolder.thumbnail.setColorFilter(filter);
                // Set thumbnail with Glide.
                GlideApp.with(mContext)
                        .load(currentExtract.thumbnail.source)
                        .transition(withCrossFade())
                        .placeholder(R.color.color500Grey)
                        .error(R.drawable.ground_astronautmhdpi)
                        .into((TopCropImageView) itemViewHolder.thumbnail);
            }
        }
    }

    private void bindAdHolder(AdItemViewHolder adViewHolder) {
        AdRequest adRequest = new AdRequest.Builder().build();
        adViewHolder.ad.loadAd(adRequest);
    }

    @Override
    public int getItemCount() {
        if (mWikiExtracts != null) {
            return mWikiExtracts.size() + 1 + 1 + 1;
        } else
            return 0;
    }

    /**
     * In-class list item view holder implementation with on click function.
     */
    private class ReadingItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        ReadingOnClick mListener;
        ArrayList<WikiExtract> mWikiExtracts;

        TextView title;
        TextView extract;
        TopCropImageView thumbnail;

        public ReadingItemViewHolder(View itemView, ArrayList<WikiExtract> wikiExtracts, ReadingOnClick listener) {
            super(itemView);
            mListener = listener;
            mWikiExtracts = wikiExtracts;

            title = itemView.findViewById(R.id.list_item_title);
            extract = itemView.findViewById(R.id.list_item_extract);
            thumbnail = itemView.findViewById(R.id.list_item_thumbnail);
            // Set the view onClick listener to the function passed to the adapter's
            // constructor, i.e. defined in the fragment. Make sure the adapter position
            // is within the extracts and not a header or footer.
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {

            if (getAdapterPosition() >= 1 && getAdapterPosition() <= 5) {
                mListener.readingOnClick(mWikiExtracts.get(getAdapterPosition() - 1));
            } else if (getAdapterPosition() >= 7 && getAdapterPosition() <= mWikiExtracts.size() + 1) {
                mListener.readingOnClick(mWikiExtracts.get(getAdapterPosition() - 2));
            }
        }
    }


    /**
     * In-class header view holder implementation.
     */
    private class AdItemViewHolder extends RecyclerView.ViewHolder {

        AdView ad = itemView.findViewById(R.id.adView);

        public AdItemViewHolder(View itemView) {
            super(itemView);
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
