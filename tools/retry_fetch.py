import urllib.request
import urllib.parse
import json
import os
import time

API_KEY = "6000bb206da212a605394fa230fb3a20"
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
OUT_DIR = os.path.join(BASE_DIR, "..", "free-my-tv", "app", "assets", "tv-assets")
POSTERS_DIR = os.path.join(OUT_DIR, "posters")
BACKDROPS_DIR = os.path.join(OUT_DIR, "backdrops")

MISSING = [
    "The Big Bang Theory",
    "How I Met Your Mother",
    "Spider-Man: No Way Home",
    "Avengers: Infinity War",
    "Deadpool",
    "The Dark Knight Rises",
    "Ratsasan",
    "Scam 1992: The Harshad Mehta Story",
    "Sunflower",
    "Inception"
]

def search_tmdb(title):
    query = urllib.parse.quote(title)
    url = f"https://api.themoviedb.org/3/search/multi?api_key={API_KEY}&query={query}"
    for attempt in range(5):
        try:
            req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 FreeMyTV/1.0'})
            with urllib.request.urlopen(req, timeout=15) as resp:
                data = json.loads(resp.read().decode('utf-8'))
                results = data.get('results', [])
                if results:
                    return results[0]
        except Exception as e:
            time.sleep(1.0 + attempt)
    return None

def download_file(url, out_path):
    for attempt in range(5):
        try:
            req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 FreeMyTV/1.0'})
            with urllib.request.urlopen(req, timeout=20) as resp, open(out_path, 'wb') as f:
                f.write(resp.read())
            return
        except Exception as e:
            time.sleep(1.0 + attempt)

def sanitize(name):
    return "".join(c if c.isalnum() or c in " _-" else "_" for c in name).strip()

for title in MISSING:
    safe_name = sanitize(title)
    poster_file = os.path.join(POSTERS_DIR, f"{safe_name}.jpg")
    backdrop_file = os.path.join(BACKDROPS_DIR, f"{safe_name}.jpg")
    
    res = search_tmdb(title)
    time.sleep(0.5)
    if not res:
        print(f"Skipping {title}: not found on TMDB")
        continue
    
    p_path = res.get('poster_path')
    b_path = res.get('backdrop_path')
    print(f"[{title}] -> Poster: {p_path}, Backdrop: {b_path}")
    
    if p_path and not os.path.exists(poster_file):
        download_file(f"https://image.tmdb.org/t/p/w500{p_path}", poster_file)
        time.sleep(0.3)
    if b_path and not os.path.exists(backdrop_file):
        download_file(f"https://image.tmdb.org/t/p/w1280{b_path}", backdrop_file)
        time.sleep(0.3)

print("Remaining downloads complete!")
