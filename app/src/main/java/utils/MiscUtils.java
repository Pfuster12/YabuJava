package utils;

import android.content.Context;

/**
 * Miscellaneous utils to aid in code.
 */
public class MiscUtils {

    public MiscUtils() {

    }

        public static MiscUtils getUtils() {
            return new MiscUtils();
        }

    public Float pxFromDp(Context context, Float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }
}
