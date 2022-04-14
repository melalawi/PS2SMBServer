package org.ps2.rom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RomMetadata {
    private static final float MAX_MATCH_CLOSENESS = 0.985f;

    private final ObjectMapper objectMapper;
    private final ObjectNode gameLibrary;
    private final File artDirectory;
    private final JaroWinklerSimilarity jaroWinklerSimilarity;

    public RomMetadata(final String libraryFilePath, final String artDirectoryPath) {
        artDirectory = new File(artDirectoryPath);
        objectMapper = new ObjectMapper();
        jaroWinklerSimilarity = new JaroWinklerSimilarity();

        try (InputStream inputStream = new FileInputStream(libraryFilePath)) {
            gameLibrary = objectMapper.readValue(inputStream, ObjectNode.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getClosestGameName(String gameName) {
        Iterator<Map.Entry<String, JsonNode>> nodes = gameLibrary.fields();
        double bestMatchScore = 0;
        String bestMatch = null;

        if (gameName.contains("(")) {
            gameName = gameName.split("\\(")[0].trim();
        }

        while (nodes.hasNext()) {
            String nextGame = nodes.next().getKey();
            double match = jaroWinklerSimilarity.apply(gameName, nextGame);

            if (match > bestMatchScore) {
                bestMatchScore = match;
                bestMatch = nextGame;
            }

            if (match >= MAX_MATCH_CLOSENESS) {
                break;
            }
        }

        return bestMatch;
    }

    public List<String> getGameIds(final String gameName) {
        if (!gameLibrary.has(gameName)) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.readerForListOf(String.class).readValue(gameLibrary.get(gameName));
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public Map<String, List<File>> getArtFiles(final List<String> gameIDs) {
        Map<String, List<File>> results = new HashMap<>();

        List<File> front = new ArrayList<>();
        List<File> side = new ArrayList<>();
        List<File> back = new ArrayList<>();
        List<File> title = new ArrayList<>();
        List<File> icon = new ArrayList<>();
        List<File> screenshot = new ArrayList<>();
        List<File> screenshot2 = new ArrayList<>();

        for (String gameId : gameIDs) {
            front.add(Paths.get(artDirectory.toString(), gameId + "_COV.png").toFile());
            side.add(Paths.get(artDirectory.toString(), gameId + "_LAB.png").toFile());
            back.add(Paths.get(artDirectory.toString(), gameId + "_COV2.png").toFile());
            title.add(Paths.get(artDirectory.toString(), gameId + "_LGO.png").toFile());
            icon.add(Paths.get(artDirectory.toString(), gameId + "_ICO.png").toFile());
            screenshot.add(Paths.get(artDirectory.toString(), gameId + "_SCR.png").toFile());
            screenshot2.add(Paths.get(artDirectory.toString(), gameId + "_SCR2.png").toFile());
        }

        screenshot.addAll(screenshot2);

        Collections.shuffle(front);
        Collections.shuffle(side);
        Collections.shuffle(back);
        Collections.shuffle(title);
        Collections.shuffle(icon);
        Collections.shuffle(screenshot);

        results.put("FRONT", front);
        results.put("SIDE", side);
        results.put("BACK", back);
        results.put("TITLE", title);
        results.put("ICON", icon);
        results.put("SCREENSHOT", screenshot);

        return results;
    }

}
