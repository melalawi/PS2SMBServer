package org.ui;

import com.api.igdb.apicalypse.APICalypse;
import com.api.igdb.apicalypse.Sort;
import com.api.igdb.exceptions.RequestException;
import com.api.igdb.request.IGDBWrapper;
import com.api.igdb.request.JsonRequestKt;
import com.api.igdb.request.ProtoRequestKt;
import com.api.igdb.request.TwitchAuthenticator;
import com.api.igdb.utils.TwitchToken;
import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import proto.Search;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

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

                if (match > 0.9) {
                    break;
                }
            }
        }

        return Paths.get(dir, bestMatch).toString();
    }

    /*
    public String searchGame(final String gameName) {
        APICalypse apicalypse = new APICalypse()
                .fields("*")
                .search("tekken");


        try {
            List<Search> searchResult = ProtoRequestKt.search(wrapper, apicalypse);

            if (searchResult.isEmpty()) {
                return null;
            }

            return searchResult.get(0).getGame().getName();
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }
    */

}
