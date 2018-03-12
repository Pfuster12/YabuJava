package viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;
import android.content.Context;

import java.util.ArrayList;

import jsondataclasses.WikiExtract;
import repository.WikiExtractRepository;

/**
 * ViewModel class to keep all data related to the Wiki extracts separating the function from
 * UI controllers. The data is loaded and prepared for the UI here. The ViewModel class is
 * lifecycle aware, as well as persistent to config changes of fragments and activities.
 */
public class WikiExtractsViewModel extends ViewModel {

    public WikiExtractsViewModel() {
        // Default constructor
    }

    // LiveData holds the app data in a lifecycle conscious manner.
    public LiveData<ArrayList<WikiExtract>> extracts;

    private WikiExtractRepository wikiRepo = WikiExtractRepository.getInstance();

    /**
     * Private method handling the async load to expose data to the public getter function.
     */
    public void loadExtracts(Context context) {
        // Handle an async for the Wiki Extracts here. Set the data returned to our LiveData object.
        // Instance of the wiki repo to grab the function that launches the retrofit async.
        extracts = wikiRepo.getDailyExtracts(context);
    }

    /**
     * Method to find if the wiki extract has been read for ui purposes
     */
    public boolean isRead(Context context, WikiExtract wikiExtract) {
        return wikiRepo.isRead(context, wikiExtract);
    }

    /**
     * Method to find if the wiki extract has been read for ui purposes
     */
    public void setRead(Context context, WikiExtract wikiExtract, boolean isRead) {
        wikiRepo.setRead(context, wikiExtract, isRead);
    }

    public void refreshExecutor() {
        wikiRepo = WikiExtractRepository.getInstance();
    }
}
