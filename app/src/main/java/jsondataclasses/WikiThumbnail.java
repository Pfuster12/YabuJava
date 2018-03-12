package jsondataclasses;

import org.parceler.Parcel;
import org.parceler.ParcelConstructor;

/**
 * Data class for the thumbnail object in the wiki Extract. Contains the url
 * of the image in the source keyword.
 */
@Parcel
public class WikiThumbnail {

    public String source;

    @ParcelConstructor
    public WikiThumbnail(String source) {
         this.source = source;
    }
}
