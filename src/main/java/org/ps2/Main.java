package org.ps2;

import org.filesys.app.SMBFileServer;
import org.ps2.rom.RomManager;
import org.ps2.rom.RomMetadata;
import org.ps2.ui.ClientApplication;
import org.ps2.ui.ClientLogger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    private static ExecutorService executorService;

    private static Configuration configuration;
    private static RomManager romManager;
    private static RomMetadata romMetadata;
    private static ClientLogger clientLogger;


    public static void main(String[] args) throws Exception {
        executorService = Executors.newFixedThreadPool(3);
        configuration = new Configuration();
        clientLogger = new ClientLogger();
        romMetadata = new RomMetadata(configuration.ps2ROMLibraryFilePath, configuration.ps2ROMArtDirectory);
        romManager = new RomManager(romMetadata, configuration.pcROMPath, configuration.ps2SMBPath);

        startClientApplication();
        startRomManager();
    }

    public static void close() {
        romManager.stop();
        executorService.shutdown();
    }

    private static void startRomManager() {
        Thread smbThread = new Thread(romManager::refresh);

        executorService.execute(smbThread);
    }

    private static void startClientApplication() {
        Thread smbThread = new Thread(() -> ClientApplication.run(clientLogger, romManager));

        executorService.execute(smbThread);
    }

    public static void startPS2Server() {
        Thread smbThread = new Thread(() -> SMBFileServer.main(clientLogger));

        executorService.execute(smbThread);
    }
}
