package jsondataclasses;

import org.parceler.Parcel;
import org.parceler.ParcelConstructor;

import java.util.ArrayList;
import java.util.List;


/**
 * An object composed of the kanji characters and the hiragana/katakana reading
 */

@Parcel
public class Kanji {

    public String mWord;
    public String mReading;
    public String mPartsOfSpeech = "";
    public List<String> mDefinitions = new ArrayList<>();
    public boolean mIsCommon = false;
    public int mJlptTag = 5;
    public String mUrl = "";
    public boolean mIsReview = false;

    @ParcelConstructor
    public Kanji(String mWord, String mReading) {
        this.mWord = mWord;
        this.mReading = mReading;
    }

    public Kanji(String mWord, String mReading, String mPartsOfSpeech) {
        this.mWord = mWord;
        this.mReading = mReading;
        this.mPartsOfSpeech = mPartsOfSpeech;
    }

    public Kanji(String mWord, String mReading, String mPartsOfSpeech,
                 List<String> mDefinitions, boolean mIsCommon, int mJlptTag,
                 String mUrl, boolean mIsReview) {
        this.mWord = mWord;
        this.mReading = mReading;
        this.mPartsOfSpeech = mPartsOfSpeech;
        this.mDefinitions = mDefinitions;
        this.mIsCommon = mIsCommon;
        this.mJlptTag = mJlptTag;
        this.mUrl = mUrl;
        this.mIsReview = mIsReview;
    }
}
