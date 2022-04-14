package org.ps2.rom.zip;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.utils.IOUtils;
import org.ps2.rom.ROM;
import org.ps2.rom.RomManager;
import org.ps2.ui.ClientApplication;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BackgroundUnzipper implements Runnable {
    private static final int MAX_CHUNK_SIZE = 209_715_200 / 4;

    private final ROM rom;

    private final String sourcePath;
    private final String destination;

    public BackgroundUnzipper(final ROM rom, final String sourcePath, final String destination) {
        this.rom = rom;
        this.sourcePath = sourcePath;
        this.destination = destination;
    }

    @Override
    public void run() {
        FileOutputStream fileOutputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
        SevenZFile sevenZFile = null;
        File unzippedIso = null;

        Path finalPath = Paths.get(sourcePath, rom.getArchiveName());
        ClientApplication.refresh(false);

        try {
            sevenZFile = new SevenZFile(finalPath.toFile());
            SevenZArchiveEntry entry;

            while ((entry = sevenZFile.getNextEntry()) != null) {
                if (entry.getName().endsWith(".iso")) {
                    unzippedIso = Paths.get(destination, entry.getName()).toFile();
                    fileOutputStream = new FileOutputStream(unzippedIso);
                    bufferedOutputStream = new BufferedOutputStream(fileOutputStream);

                    double remainingEntrySize = entry.getSize();

                    while (remainingEntrySize > 0) {
                        int nextChunk = (int) Math.min(MAX_CHUNK_SIZE, remainingEntrySize);
                        remainingEntrySize -= nextChunk;

                        rom.setLoadProgress((int) (100 - (remainingEntrySize / entry.getSize() * 100)));

                        byte[] content = new byte[nextChunk];
                        sevenZFile.read(content);
                        bufferedOutputStream.write(content);
                        bufferedOutputStream.flush();
                        fileOutputStream.flush();
                    }

                    rom.setLoadProgress(100);
                    break;
                }
            }
        } catch (Exception e) {
            if (!(e instanceof ClosedByInterruptException)) {
                e.printStackTrace();
                rom.setLoadProgress(0);
            }
            if (unzippedIso != null) {
                unzippedIso.delete();
            }
        } finally {
            IOUtils.closeQuietly(fileOutputStream);
            IOUtils.closeQuietly(sevenZFile);
            IOUtils.closeQuietly(bufferedOutputStream);
        }

        ClientApplication.refresh(false);
    }
}