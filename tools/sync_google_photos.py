#!/usr/bin/env python3
"""Downloads photos from a shared Google Photos album and pushes them to Free My TV on the TV."""
import concurrent.futures
import os
import re
import sys
import urllib.request

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "tools"))
import tv

ALBUM_URL = "https://photos.app.goo.gl/LFvsdnvLY5qQjmgt7"
LOCAL_DIR = os.path.join(os.path.dirname(__file__), "..", "work", "google_photos")
REMOTE_INBOX = "/sdcard/Android/data/io.freemytv/files/photos"
MAX_PHOTOS = 95  # Free My TV supports up to 100 photos


def get_photo_urls(album_url):
    print(f"Fetching album: {album_url} ...")
    req = urllib.request.Request(album_url, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req) as resp:
        html = resp.read().decode("utf-8")

    items = re.findall(r'\[\"(https://lh3\.googleusercontent\.com/pw/[^\"]+)\",(\d+),(\d+)', html)
    seen = set()
    photos = []
    for url, w, h in items:
        base = url.split("=")[0]
        if base not in seen:
            seen.add(base)
            photos.append(base)
            if len(photos) >= MAX_PHOTOS:
                break
    print(f"Extracted {len(photos)} photos from album.")
    return photos


def download_one(args):
    idx, base_url = args
    filename = f"{idx:03d}_photo.jpg"
    target_path = os.path.join(LOCAL_DIR, filename)
    # If already downloaded, skip
    if os.path.exists(target_path) and os.path.getsize(target_path) > 10000:
        return target_path

    # Request at 2112x1188 (screensaver resolution)
    download_url = base_url + "=w2112-h1188"
    req = urllib.request.Request(download_url, headers={"User-Agent": "Mozilla/5.0"})
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            data = resp.read()
            with open(target_path, "wb") as f:
                f.write(data)
        return target_path
    except Exception as e:
        print(f"Failed to download photo {idx}: {e}")
        return None


def main():
    os.makedirs(LOCAL_DIR, exist_ok=True)
    photos = get_photo_urls(ALBUM_URL)
    if not photos:
        print("No photos found in album!")
        return 1

    print(f"Downloading {len(photos)} photos in parallel to {LOCAL_DIR} ...")
    tasks = list(enumerate(photos))
    downloaded_files = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as executor:
        for res in executor.map(download_one, tasks):
            if res:
                downloaded_files.append(res)
                print(f"\rDownloaded {len(downloaded_files)}/{len(photos)} photos", end="", flush=True)

    print(f"\nAll {len(downloaded_files)} photos downloaded successfully.")

    # Connect to TV and push
    print(f"Connecting to TV ({os.environ.get('TV_HOST', '192.168.1.12')}) ...")
    dev = tv.connect()
    try:
        dev.shell(f"mkdir -p '{REMOTE_INBOX}'")
        print(f"Pushing photos to TV at {REMOTE_INBOX} ...")
        for i, local_path in enumerate(downloaded_files):
            fname = os.path.basename(local_path)
            remote_path = f"{REMOTE_INBOX}/{fname}"
            dev.push(local_path, remote_path)
            print(f"\rPushed {i + 1}/{len(downloaded_files)} to TV", end="", flush=True)
        print("\nAll photos pushed to TV!")
    finally:
        dev.close()

    print("Done! Photos will be imported into Free My TV screensaver upon next wake/home.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
