package viewmodel;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.content.Context;
import android.support.v4.util.Pair;

import java.util.ArrayList;

import jsondataclasses.Kanji;
import sql.KanjisSQLDao;

/**
 * Review Words fragment view model. Takes care of querying the database for review words
 * from the Kanjis DAO object.
 */
public class ReviewViewModel extends ViewModel {

    public ReviewViewModel() {

    }

    // LiveData holds the app data in a lifecycle conscious manner.
    public MutableLiveData<ArrayList<Pair<Integer, Kanji>>> reviewKanjis = new MutableLiveData<>();

    private KanjisSQLDao kanjisDao = KanjisSQLDao.getInstance();

    /**
     * Method handling the async load to expose kanji and furigana data to the ui that calls it.
     */
    public void loadReviewKanjis(Context context) {
        // Set the live data object
        reviewKanjis.setValue(kanjisDao.getReviewKanjis(context));
    }
}
