# Converts bin/cue roms to iso format
# Ensures all roms are compressed using latest 7z algorithm

import glob
import logging
import os
import subprocess
import sys
import time

logging.basicConfig(level=logging.DEBUG, filename="D:/ROMS/output.txt", filemode="a+", format="%(asctime)-15s %(levelname)-8s %(message)s")
logging.getLogger().addHandler(logging.StreamHandler(sys.stdout))

root_dir = 'D:/ROMS/PS2'
directory = os.listdir(root_dir)
completed = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C']


def run():
    for file in directory:
        if not file[0] in completed and file.endswith(".7z"):
            path = os.path.join(root_dir, file)
            logging.info("Next file: " + path)
            unzip(path)

            bin_count = len(glob.glob1(root_dir,  "*.bin"))

            if bin_count == 0:
                logging.info(path + " contains ISO file. Recompressing...")
                os.rename(path, os.path.join(root_dir, os.path.splitext(file)[0] + "-backup.7z"))
                time.sleep(5)
                zip(root_dir, file)
            elif bin_count == 1:
                logging.info(path + " contains single bin/cue.")
                os.rename(path, os.path.join(root_dir, os.path.splitext(file)[0] + "-backup.7z"))
                time.sleep(5)
                cue_path = glob.glob1(root_dir, "*.cue")[0]
                convertToISO(root_dir, cue_path)
                time.sleep(5)
                zip(root_dir, cue_path)
            else:
                logging.info(path + " contains multiple bin & single cue. Skipping...")
                if not file.endswith("-bad.7z"):
                    os.rename(path, os.path.join(root_dir, os.path.splitext(file)[0] + "-bad.7z"))
                
            time.sleep(5)
            logging.info("Cleaning up results of " + path)
            deleteJunkFiles()


def unzip(path):
    logging.info("Unzipping " + path)
    subprocess.run(["C:/Program Files/7-Zip/7z.exe", "e", path, "-y"], stdout=subprocess.DEVNULL)
    logging.info("Unzip complete")

def zip(sub_dir, file):
    logging.info("Zipping " + os.path.join(sub_dir, file))

    zipFile = os.path.join(sub_dir, os.path.splitext(file)[0] + ".7z")
    isoFile = os.path.join(sub_dir, os.path.splitext(file)[0] + ".iso")

    subprocess.run(["C:/Program Files/7-Zip/7z.exe", "a", "-t7z", zipFile, isoFile], stdout=subprocess.DEVNULL)
    logging.info("Zip complete")

def convertToISO(sub_dir, cue_path):
    logging.info("Converting " + cue_path + " to ISO...")
    subprocess.run(["C:/Program Files (x86)/AnyToISO/anytoiso.exe", "/convert", os.path.join(sub_dir, cue_path), os.path.join(sub_dir, os.path.splitext(cue_path)[0] + ".iso")], stdout=subprocess.DEVNULL)
    logging.info("Conversion complete")

def deleteJunkFiles():
    new_dir = os.listdir(root_dir)
    for item in new_dir:
        if item.endswith(".cue") or item.endswith(".bin") or item.endswith("-backup.7z") or item.endswith(".iso"):
            logging.info("Deleting " + os.path.join(root_dir, item))
            os.remove(os.path.join(root_dir, item))

run()