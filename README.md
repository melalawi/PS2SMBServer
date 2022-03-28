# PS2SMBServer
An open-source SMBv1 application for vending PlayStation 2 ROMs to an ethernet-enabled PS2 via LAN.

![Demo](Demo.mp4)

## Introduction
Loading PS2 ROMs over ethernet is made possible by the custom PS2 software known as [Open-PS2-Loader](https://github.com/ps2homebrew/Open-PS2-Loader). However, in setting up the process I found quite a few limitations:

* The selection of SMBv1 software on the PC side of things is limited. I personally did not want to enable SMBv1 at the OS level, what with all the security risks.
* Open-PS2-Loader required that ROMs exist as uncompressed .iso files.
* No Plex-like capabilities. If I wanted to travel with my PS2, I'd have to either bring all of my PS2 disks, or all of my bulky HDDs.

I spent a few days ironing out these grievances.

## Description
* One-click setup of an SMBv1 server
* Double-click unzipping and copy-pasting of 7z-compressed ROMs from an arbitrary directory to the PS2 SMB directory.
  * You can set up a network drive and VPN on my home machine containing all of my compressed ROMs
* Art asset delivery 
* UI based ROM management.

## Notes
* PS2SMBServer attempts to also copy over art assets to the PS2 SMB directory. The art assets are not included in this repository.
* PS2SMBServer can also be used as a basic SMB server for the PS2. No need to place files outside of the SMB directory.
* This was a hackathon-style project, and as a result I have accumulated quite a bit of technical debt that I may or may not iron out over time (pull requests, anyone? 👀).

## TODOs
* Bug fix and tech debt reduction.
* Cleaner integration with JFileServer.
* NetBIOS/DHCP integration. Unfortunately even now my understanding of these relatively older protocols is fuzzy.
* Support file deletion and cleanup.
* Better documentation.
* General UI refactoring.

## Dependencies
* [JFileServer](https://github.com/FileSysOrg/jfileserver)
* [JavaFX](https://github.com/openjdk/jfx)
* [Apache Common libraries](https://github.com/apache/commons-collections)
* [Open-PS2-Loader](https://github.com/ps2homebrew/Open-PS2-Loader) - indirect dependency.

## License

GNU General Public License

## Contact
* Create an issue here.
* Email me at [mo@itsmo.me](mailto:mo@itsmo.me).