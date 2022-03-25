package org.ui;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.utils.IOUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Unzipper implements Runnable {
    private static final int MAX_CHUNK_SIZE = 209_715_200 / 4;
    static final List<Unzipper> ACTIVE_UNZIPS = Collections.synchronizedList(new ArrayList<>());

    private String sourcePath;
    private String fileName;
    private String destination;
    public AtomicInteger progress;

    public Unzipper(final String sourcePath, final String fileName, final String destination) {
        this.sourcePath = sourcePath;
        this.fileName = fileName;
        this.destination = destination;
        this.progress = new AtomicInteger(0);
    }

    public final String getFileName() {
        return fileName;
    }

    @Override
    public void run() {
        ACTIVE_UNZIPS.add(this);
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        SevenZFile sevenZFile = null;

        Path finalPath = Paths.get(sourcePath, fileName);

        try {
            sevenZFile = new SevenZFile(finalPath.toFile());
            SevenZArchiveEntry entry;

            while ((entry = sevenZFile.getNextEntry()) != null) {
                if (entry.getName().endsWith(".iso")) {
                    File unzippedIso = Paths.get(destination, entry.getName()).toFile();
                    fos = new FileOutputStream(unzippedIso);
                    bos = new BufferedOutputStream(fos);

                    double remainingEntrySize = entry.getSize();
                    System.out.println("Un seven zipping - " + unzippedIso);

                    while (remainingEntrySize > 0) {
                        int nextChunk = (int) Math.min(MAX_CHUNK_SIZE, remainingEntrySize);
                        remainingEntrySize -= nextChunk;

                        progress.set((int) (100 - (remainingEntrySize / entry.getSize() * 100)));

                        System.out.println(fileName + " at " + progress.get() + "%");

                        byte[] content = new byte[nextChunk];
                        sevenZFile.read(content);
                        bos.write(content);
                        bos.flush();
                        fos.flush();
                    }
                    progress.set(100);
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            ACTIVE_UNZIPS.remove(this);
            IOUtils.closeQuietly(fos);
            IOUtils.closeQuietly(sevenZFile);
            IOUtils.closeQuietly(bos);
        }

        if (!Server.ps2LoadedROMList.contains(fileName)) {
            Server.ps2LoadedROMList.add(fileName);
        }
    }
}