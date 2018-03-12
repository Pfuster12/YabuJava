package jsondataclasses;

import org.parceler.Parcel;
import org.parceler.ParcelConstructor;

/**
 * Data class for the Wiki extract returned by the API call. Stores page id, title, and the extract
 * text into an object.
 */
@Parcel
public class WikiExtract {

    public int pageId;
    public String title;
    public String extract;
    public WikiThumbnail thumbnail;

    @ParcelConstructor
    public WikiExtract(int pageId, String title, String extract, WikiThumbnail thumbnail) {
        this.pageId = pageId;
        this.title = title;
        this.extract = extract;
        this.thumbnail = thumbnail;
    }
}
