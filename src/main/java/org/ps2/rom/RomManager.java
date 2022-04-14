package org.ps2.rom;

import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.io.FileUtils;
import org.ps2.rom.zip.BackgroundUnzipper;
import org.ps2.ui.ClientApplication;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class RomManager {
    public static final Comparator<ROM> ROM_SORTER = Comparator
            .comparing(ROM::getLoadProgress).reversed()
            .thenComparing(ROM::getCleanName);
    private static final List<String> SMB_DIRECTORIES = Arrays.asList("APPS", "ART", "CD", "CFG", "CHT", "DVD", "LNG", "THM", "VMC");

    private AtomicBoolean loading;

    // Clean name to ROM class
    private final ConcurrentHashMap<String, ROM> roms;

    private final List<ROM> activeUnzips;
    private final ThreadPoolExecutor unzipThreadPool;

    private final RomMetadata romMetadata;

    private final String remoteDirectory;
    private final String smbDirectory;

    public RomManager(final RomMetadata romMetadata, final String remoteDirectory, final String smbDirectory) {
        roms = new ConcurrentHashMap<>();
        unzipThreadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);
        activeUnzips = new ArrayList<>();
                
        this.romMetadata = romMetadata;
        this.remoteDirectory = remoteDirectory;
        this.smbDirectory = smbDirectory;

        loading = new AtomicBoolean(false);

        createSMBDirectories();
    }

    private void createSMBDirectories() {
        for (String subDirectory : SMB_DIRECTORIES) {
            Path fullPath = Paths.get(smbDirectory, subDirectory);

            fullPath.toFile().mkdirs();
        }
    }

    public void refresh() {
        loading.set(true);

        roms.clear();
        auditSMBROMs();
        auditRemoteROMs();

        loading.set(false);
    }

    public List<ROM> getCurrentUnzipJobs() {
        activeUnzips.removeIf(rom -> rom.getLoadProgress() == 100);

        return activeUnzips;
    }

    public boolean isLoading() {
        return loading.get();
    }

    public void stop() {
        unzipThreadPool.shutdownNow();
    }

    public List<ROM> getROMList() {
        return roms.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(ROM_SORTER))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    public void unpack(final ROM rom) {
        if (rom.getArchiveName() != null) {
            BackgroundUnzipper backgroundUnzipper = new BackgroundUnzipper(
                    rom,
                    remoteDirectory,
                    Paths.get(smbDirectory, "DVD").toString());

            rom.setLoadProgress(1);

            unzipThreadPool.execute(backgroundUnzipper);
            activeUnzips.add(rom);
            copyArtFiles(rom);
        }

        ClientApplication.refresh(true);
    }

    private void copyArtFiles(final ROM rom) {
        romMetadata.getArtFiles(rom.getGameIDs()).entrySet().stream()
                .map(Map.Entry::getValue)
                .flatMap(Collection::stream)
                .forEach(file -> {
                    try {
                        if (file.exists()) {
                            FileUtils.copyFile(file, Paths.get(smbDirectory, "ART", file.getName()).toFile());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    public Map<String, List<File>> getRomArtFiles(final ROM rom) {
        if (rom == null) {
            throw new IllegalArgumentException("Cannot get art files of null ROM");
        }

        return romMetadata.getArtFiles(rom.getGameIDs());
    }

    private void auditRemoteROMs() {
        File remoteDirectoryFile = new File(remoteDirectory);

        if (remoteDirectoryFile.exists()) {
            String[] remoteROMFileList = remoteDirectoryFile.list();

            if (remoteROMFileList == null) {
                return;
            }

            for (String romZip : remoteROMFileList) {
                if (romZip.endsWith(".7z")) {
                    final String extensionLessName = FileNameUtils.getBaseName(romZip);
                    final String cleanName = romMetadata.getClosestGameName(extensionLessName);

                    if (roms.containsKey(cleanName)) {
                        roms.get(cleanName).setArchiveName(romZip);
                    } else {
                        ROM newRom = addROM(romZip);
                        newRom.setArchiveName(romZip);
                    }
                }
            }
        }
        auditSMBROMs();
    }

    private void auditSMBROMs() {
        File smbDirectoryFile = Paths.get(smbDirectory, "DVD").toFile();

        if (smbDirectoryFile.exists()) {
            String[] smbROMFileList = smbDirectoryFile.list();

            if (smbROMFileList == null) {
                return;
            }

            for (String game : smbROMFileList) {
                if (game.endsWith(".iso")) {
                    final String extensionLessName = FileNameUtils.getBaseName(game);
                    final String cleanName = romMetadata.getClosestGameName(extensionLessName);

                    if (roms.containsKey(cleanName)) {
                        roms.get(cleanName).setLoadProgress(100);
                    } else {
                        ROM newRom = addROM(game);
                        newRom.setLoadProgress(100);
                    }
                }
            }
        }
    }

    private ROM addROM(final String fileNameWithExtension) {
        final String extensionLessName = FileNameUtils.getBaseName(fileNameWithExtension);
        final String cleanName = romMetadata.getClosestGameName(extensionLessName);
        final List<String> gameIds = romMetadata.getGameIds(cleanName);

        final ROM newROM = new ROM(extensionLessName, cleanName, gameIds);

        roms.put(cleanName, newROM);

        return newROM;
    }

}
