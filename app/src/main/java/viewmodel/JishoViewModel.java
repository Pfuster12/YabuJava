package viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;
import android.content.Context;
import android.support.v4.util.Pair;

import java.util.ArrayList;

import jsondataclasses.Kanji;
import jsondataclasses.WikiExtract;
import repository.JishoRepository;

/**
 * ViewModel class to keep all data related to the Jisho words separating the function from
 * UI controllers. The data is loaded and prepared for the UI here. The ViewModel class is
 * lifecycle aware, as well as persistent to config changes of fragments and activities.
 */
public class JishoViewModel extends ViewModel {

    public JishoViewModel() {

    }

    // LiveData holds the app data in a lifecycle conscious manner.
    public LiveData<ArrayList<Pair<Pair<Integer, Integer>, Kanji>>> kanjis;

    public LiveData<Pair<Integer, Kanji>> kanji;

    private JishoRepository jishoRepo = JishoRepository.getInstance();

    /**
     * Method handling the async load to expose kanji and furigana data to the ui that calls it.
     */
    public void loadKanjis(Context context, WikiExtract wikiExtract) {
        // Set the live data object
        kanjis = jishoRepo.getWords(context, wikiExtract);
    }

    /**
     * Method handling the async load to expose kanji definitions and tags to the ui that calls it.
     */
    public void getDefinitions(Context context, Kanji protoKanji) {
        // check if coming from a point where executor was shutdown
        if (JishoRepository.executor.isShutdown() || JishoRepository.executor.isTerminated()) {
            jishoRepo = JishoRepository.getInstance();
        }
        kanji = jishoRepo.getDefinitions(context, protoKanji);
    }
}
