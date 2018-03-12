package utils;


import android.support.v4.util.Pair;

import java.util.ArrayList;

/**
 * Helper util to make extract text spannable, scan the text for kanji and extract them
 * for api calls, and set onClicks on the different words extracted.r
 */
public class WordScanner {

    /**
     * Companion object to expose utils to other classes.
     */

    // function to get an instance of the utils
    public static WordScanner getUtils() {
        return new WordScanner();
    }

    /**
     * Helper function to scan the text extract and retrieve the character sequences
     * that represent unicode code-points within the CJK Unified Ideographs block of code
     * points, aka the Kanji in the text. Return a list of these strings.
     */
    ArrayList<Pair<Pair<Integer, Integer>, String>> scanText(String extract) {
        // init an empty pair list of the index range and the word.
        ArrayList<Pair<Pair<Integer, Integer>, String>> pairsOfIndexWord = new ArrayList<>();

        // Iterate through the chars in the extract string
        int i = 0;
        if (extract != null) {
            while (i < extract.length()) {
                //Grab the code-point of the character
                int codePoint = Character.codePointAt(String.valueOf(extract.charAt(i)), 0);

                // Check the Unicode range and join the kanji words
                // Put into the map the index of the kanji and the word as well.
                pairsOfIndexWord.addAll(checkUnicodeRange(i, codePoint, extract));
                i++;
            }
        }
        // Return the list of kanji strings
        return pairsOfIndexWord;
    }

    /**
     * Private helper function to check what Unicode block from the above ranges
     * the chars code-point falls in.
     */
    private ArrayList<Pair<Pair<Integer, Integer>, String>>
    checkUnicodeRange(Integer index, Integer codePoint, String string) {
        /*
       The Unicode code-point number range for UTF-16 encoding (see https://codepoints.net/)
       for the 'CJK Unified Ideographs', aka the Chinese and Japanese characters. This range
       helps identifies whether a Character object is a Kanji in our wiki extract text
       */
        // Start of the range
        final int startCodePoint = 19968; // Represents the character 一 "one"
        // End of the range
        final int endCodePoint =  40917; // Almost the last one represents 鿕.
        // Special iteration mark code point
        final int iterationCodePoint = 12293;

        // For possible use in future.
    /*    // Unicode code-point range for punctuation of CJK unicode chars
        val punctuationRange = object {
            // Start of the range
            val startCodePoint = 12288
            // End of the range
            val endCodePoint = 12351
        }

        // Unicode code-point range for the Hiragana unicode chars
        val hiraganaRange = object {
            // Start of the range
            val startCodePoint = 12353
            // End of the range
            val endCodePoint = 12447
        }

        // Unicode code-point range for the Katakana unicode chars
        val katakanaRange = object {
            // Start of the range
            val startCodePoint = 12448
            // End of the range
            val endCodePoint = 12543
        }

        // Unicode code-point range for the Katakana extensions unicode chars
        val katakanaExtensionRange = object {
            // Start of the range
            val startCodePoint = 12784
            // End of the range
            val endCodePoint = 12799
        }*/

        // init an empty pair list
        ArrayList<Pair<Pair<Integer, Integer>, String>> pairIndexWordList = new ArrayList<>();

        /*
        * Range control flow to check what the chars code-points are (Kanji, hiragana,
        * katakana, punctuation or other.
        */
        if (startCodePoint <= codePoint && codePoint <= endCodePoint
                || codePoint == iterationCodePoint) {
            // Init an empty string to contain the full Kanji word extracted below.
            ArrayList<Pair<Pair<Integer, Integer>, String>> pairIndexWordListOneEntry =
                    joinKanji(index, string);

            // Add the pair given by joinKanji() that is not empty.
            if (pairIndexWordListOneEntry.size() != 0) {
                pairIndexWordList.addAll(pairIndexWordListOneEntry);
            }
        }

        /*
        if (codePoint in hiraganaRange.startCodePoint..hiraganaRange.endCodePoint) {
            // The code-point falls in the hiragana block.
        }

        if (codePoint in katakanaRange.startCodePoint..katakanaRange.endCodePoint) {
            // The code-point falls in the katakana block.
        }

        if (codePoint in katakanaExtensionRange.startCodePoint..katakanaExtensionRange.endCodePoint) {
            // The code-point falls in the katakana extension block.
        }

        if (codePoint in punctuationRange.startCodePoint..punctuationRange.endCodePoint) {
            // The code-point falls in the punctuation block.
        }
        */

        // Return the map of values of the index of the kanji and the word itself.
        return pairIndexWordList;
    }

    /**
     * Helper function to find what kanji to join or not as delimited by hiragana and katakana
     * returning a string of the word kanji.
     */
    private ArrayList<Pair<Pair<Integer, Integer>, String>> joinKanji(Integer index, String string) {
        // init an empty pair list
        ArrayList<Pair<Pair<Integer, Integer>, String>> pairIndexWordList = new ArrayList<>();

        // The code-point falls in the CJK ideograph block.
        // Check previous char
        boolean isPrevCJK = isPreviousIndexCJK(index, string);
        // Check after char
        boolean isAfterCJK = isAfterIndexCJK(index, string);

        if (isAfterCJK) {
            // Do nothing and let the loop continue as it is not the end of a kanji word.
        } else {
            // The char after is not a CJK therefore we know this is the end of a kanji word.
            // Get the current kanji
            String kanjiReversed = String.valueOf(string.charAt(index));

            // Get the current index
            int currentIndex = index;

            // init an index list to hold all the indexes of the word we are combining here. Start
            // with the first one (In reverse).
            ArrayList<Integer> indexList = new ArrayList<>();
            indexList.add(currentIndex);

            while (isPrevCJK) {
                // While the previous char is a CJK char, join the previous char
                // with the current one which we know is the last of the word.
                Character prevKanji = string.charAt(currentIndex - 1);

                // This will return a reversed kanji word.
                kanjiReversed = kanjiReversed.concat(String.valueOf(prevKanji));

                // Decrement the index to check the next char down the line.
                currentIndex = currentIndex - 1;

                // Add the next index
                indexList.add(currentIndex);

                // Check if it is CJK and iterate while loop
                isPrevCJK = isPreviousIndexCJK(currentIndex, string);
            }

            // Create an int range to store the index range of the kanji in the text string.
            Pair<Integer, Integer> indexRange = new Pair<>(indexList.get(0), indexList.get(indexList.size() - 1));

            // Create a new pair value with the correct word orientation.
            StringBuilder sb = new StringBuilder(kanjiReversed);
            String normalKanji = sb.reverse().toString();
            Pair<Pair<Integer, Integer>, String> pairIndexWord = new Pair<>(indexRange, normalKanji);
            pairIndexWordList.add(pairIndexWord);
        }
        return pairIndexWordList;
    }

    /**
     * Helper fun to return whether code point of char
     * BEFORE the current one is within the CJK block
     */
    private boolean isPreviousIndexCJK(Integer currentIndex, String string) {
        // Get the previous index
        int previousIndex = currentIndex - 1;
        boolean isCJK = false;

        // Start of the range
        int startCodePoint = 19968; // Represents the character 一 "one"
        // End of the range
        int endCodePoint =  40917; // Almost the last one represents 鿕. (?)
        // Iteration mark code point
        int iterationCodePoint = 12293;

        // Grab the code-point of the previous character when the index is not 0
        if (currentIndex != 0) {
            int codePoint = Character.codePointAt(String.valueOf(string.charAt(previousIndex)), 0);
            if (startCodePoint <= codePoint && codePoint <= endCodePoint
                    || codePoint == iterationCodePoint) {
                // The code-point falls in the CJK ideograph block.
                // Add char to the kanji list
                isCJK = true;
            }
        }

        // Return boolean
        return isCJK;
    }

    /**
     * Helper fun to return whether code point of char
     * AFTER the current one is within the CJK block
     */
    private boolean isAfterIndexCJK(Integer currentIndex, String string) {
        // Get the index after
        int afterIndex = currentIndex + 1;
        boolean isCJK = false;

        // Get cjkRange
        // Start of the range
        int startCodePoint = 19968; // Represents the character 一 "one"
        // End of the range
        int endCodePoint =  40917; // Almost the last one represents 鿕. (?)
        // Iteration mark code point
        int iterationCodePoint = 12293;


        // Grab the code-point of the after character when it is not the last.
        if (currentIndex < string.length() - 1) {
            int codePoint = Character.codePointAt(String.valueOf(string.charAt(afterIndex)), 0);
            if (startCodePoint <= codePoint && codePoint <= endCodePoint
                    || codePoint == iterationCodePoint) {
                // The code-point falls in the CJK ideograph block.
                // Add char to the kanji list
                isCJK = true;
            }
        }

        // Return boolean
        return isCJK;
    }

    public String buildJishoQuery(String extract) {
        // Scan the text to extract each kanji into a pair with its index range.
        ArrayList<Pair<Pair<Integer, Integer>, String>> pairs = scanText(extract);

        // init a build string for the query
        String jishoQuery = "";

        // Iterate through the list of pairs.
        for (Pair<Pair<Integer, Integer>, String> pair : pairs) {
            // Grab the kanji word from the pair.
            String kanjiChars = pair.second;
            // Add the word to the query string and a comma to separate.
            jishoQuery = jishoQuery.concat(kanjiChars + ",");
        }

        // Return the concatenated string
        return jishoQuery;
    }
}
