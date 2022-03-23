package org.ui;

import org.filesys.app.SMBFileServer;

public class Server {
    public static void main(String[] args) {
        start();
    }
    
    public static void start() {
        SMBFileServer.main(new String[0]);
    }
}
