package org.ui;

import com.api.igdb.request.IGDBWrapper;
import com.api.igdb.request.TwitchAuthenticator;
import com.api.igdb.utils.TwitchToken;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;

import java.io.File;
import java.nio.file.Paths;

public class GameMetadata {
    private TwitchAuthenticator tAuth = TwitchAuthenticator.INSTANCE;
    private IGDBWrapper wrapper = IGDBWrapper.INSTANCE;
    private TwitchToken token;

    public GameMetadata() {
    }

    public String getClosestArtFile(final String gameName, final String dir) {
        File artDirectory = new File(dir);
        double bestMatchScore = 0;
        String bestMatch = null;


        if (artDirectory.exists()) {
            String[] artFiles = artDirectory.list();
            JaroWinklerSimilarity similarity = new JaroWinklerSimilarity();

            for (String file : artFiles) {
                double match = similarity.apply(file, gameName);

                if (match > bestMatchScore) {
                    bestMatchScore = match;
                    bestMatch = file;
                }

                if (match > 0.975) {
                    break;
                }
            }
        }

        return Paths.get(dir, bestMatch).toString();
    }

}
