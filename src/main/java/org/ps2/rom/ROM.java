package org.ps2.rom;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ROM {
    private final String cleanName;
    private final List<String> gameIDs;

    private final String fileName;
    private String archiveName = null;

    private AtomicInteger loadProgress;
    public ROM(final String fileName, final String cleanName, final List<String> gameIDs) {
        this.fileName = fileName;
        this.cleanName = cleanName;
        this.gameIDs = gameIDs;

        this.loadProgress = new AtomicInteger(0);
    }

    public String getCleanName() {
        return cleanName;
    }

    public List<String> getGameIDs() {
        return gameIDs;
    }

    public String getFileName() {
        return fileName;
    }

    public int getLoadProgress() {
        return loadProgress.get();
    }

    public void setLoadProgress(int loadProgress) {
        this.loadProgress.set(loadProgress);
    }

    public String getArchiveName() {
        return archiveName;
    }

    public void setArchiveName(String archiveName) {
        this.archiveName = archiveName;
    }
}
