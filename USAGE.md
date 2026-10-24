# Free My TV Setup & Control Guide

Your Sony Bravia TV (`192.168.1.12`) is now running **Free My TV** as its default home screen and screensaver.

---

## What Is Configured
- **City:** Pune, Maharashtra, India
- **Day/Night Theme:** Automatic, tracks Pune's actual sunset (6:06 PM) and sunrise (6:31 AM)
- **Local Weather:** Live temperature, hourly conditions, and high/low
- **Hardware Integration:** HDMI ports and TataSKY passthrough input directly accessible
- **Debloat:** Stock recommendation carousels, backdrop ads, and background trackers disabled (nothing uninstalled)

---

## Screenshots

### 1. New Home Screen
![Free My TV Home Screen](shots/03-after-switch.png)

### 2. Drawer (Inputs, Shortcuts, Hourly Forecast)
![Free My TV Drawer](shots/04-dpad-down.png)

---

## How to Customize

### 1. Change Shelf Apps or Their Order
You can change the apps directly on your TV without a computer:
1. Press **Down** on your remote to open the drawer.
2. Select **Home Settings** (or open the **Free My TV** app from All Apps).
3. Select **Shelf**:
   - Choose which apps appear on the dock shelf.
   - Reorder them by pressing Up/Down on the list.

### 2. Remote Buttons
Under **Free My TV** → **Remote Buttons**, you can configure what your remote's dedicated app buttons (Netflix, YouTube, Prime Video, etc.) launch.

### 3. Adding Trending Movie/Show Recommendations (TMDB Key)
If you want hourly trending recommendations and movie backdrop wallpapers:
1. Get a free API key at [themoviedb.org/settings/api](https://www.themoviedb.org/settings/api).
2. Open `free-my-tv/config.properties` on your computer.
3. Paste the key after `tmdb_key=`:
   ```properties
   tmdb_key=your_api_key_here
   ```
4. Run the update script from your terminal:
   ```bash
   ./build-and-install.sh 192.168.1.12
   ```

### 4. Private DNS (Ad-Blocking)
Encrypted Private DNS is active using AdGuard DNS (`dns.adguard-dns.com`).
To toggle or disable it:
- **Disable / Return to default:**
  ```bash
  TV_HOST=192.168.1.12 tools/venv/bin/python tools/tv.py "settings put global private_dns_mode opportunistic"
  ```
- **Re-enable:**
  ```bash
  TV_HOST=192.168.1.12 tools/venv/bin/python tools/tv.py "settings put global private_dns_specifier dns.adguard-dns.com && settings put global private_dns_mode hostname"
  ```

---

## How to Restore Stock Factory Launcher

### Method 1: Directly on the TV
1. Open the **Free My TV** app.
2. Under **Home Screen**, change the selection from **Free My TV** back to **Original**.
3. All stock settings, screensavers, and original factory apps are restored instantly.

### Method 2: From Your Computer
Run the included restore script:
```bash
./restore.sh 192.168.1.12
```
