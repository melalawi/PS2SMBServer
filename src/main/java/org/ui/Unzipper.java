package org.ui;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.tukaani.xz.SeekableFileInputStream;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Unzipper {
    private static final int MAX_CHUNK_SIZE = 209_715_200;

    public static void unSevenZipFile(final Path source, final String destination) throws Exception {
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        SevenZFile sevenZFile = null;

        try {
            sevenZFile = new SevenZFile(source.toFile());
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

                        System.out.println("Next chunk size - " + nextChunk);

                        byte[] content = new byte[nextChunk];
                        sevenZFile.read(content);
                        bos.write(content);
                        bos.flush();
                        fos.flush();
                    }
                    System.out.println("Done!");
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                fos.close();
            }
            if (bos != null) {
                bos.close();
            }
            if (sevenZFile != null) {
                sevenZFile.close();
            }
        }
    }
}