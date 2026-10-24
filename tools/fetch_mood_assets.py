import urllib.request
import urllib.parse
import json
import os
import sys

API_KEY = "6000bb206da212a605394fa230fb3a20"
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
OUT_DIR = os.path.join(BASE_DIR, "..", "free-my-tv", "app", "assets", "tv-assets")
POSTERS_DIR = os.path.join(OUT_DIR, "posters")
BACKDROPS_DIR = os.path.join(OUT_DIR, "backdrops")

os.makedirs(POSTERS_DIR, exist_ok=True)
os.makedirs(BACKDROPS_DIR, exist_ok=True)

TITLES = [
    # Comfort sitcom
    "The Big Bang Theory",
    "The Office",
    "Friends",
    "Brooklyn Nine-Nine",
    "How I Met Your Mother",
    "Modern Family",
    # Capes and chaos
    "Spider-Man: No Way Home",
    "Avengers: Endgame",
    "Avengers: Infinity War",
    "Deadpool",
    "The Boys",
    "Zack Snyder's Justice League",
    # Gotham after dark
    "The Dark Knight",
    "The Batman",
    "Joker",
    "Batman Begins",
    "The Dark Knight Rises",
    # Thrillers
    "Drishyam",
    "Drishyam 2",
    "Andhadhun",
    "Ratsasan",
    "Kantara",
    "Scam 1992: The Harshad Mehta Story",
    "Sunflower",
    # Mind games
    "Inception",
    "Shutter Island",
    "Interstellar",
    "The Prestige",
    "Fight Club"
]

def search_tmdb(title):
    query = urllib.parse.quote(title)
    url = f"https://api.themoviedb.org/3/search/multi?api_key={API_KEY}&query={query}"
    req = urllib.request.Request(url, headers={'User-Agent': 'FreeMyTV/1.0'})
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            data = json.loads(resp.read().decode('utf-8'))
            results = data.get('results', [])
            if results:
                return results[0]
    except Exception as e:
        print(f"Error searching {title}: {e}")
    return None

def download_file(url, out_path):
    req = urllib.request.Request(url, headers={'User-Agent': 'FreeMyTV/1.0'})
    with urllib.request.urlopen(req, timeout=15) as resp, open(out_path, 'wb') as f:
        f.write(resp.read())

print(f"Fetching assets for {len(TITLES)} titles...")

def sanitize(name):
    return "".join(c if c.isalnum() or c in " _-" else "_" for c in name).strip()

metadata = []

for title in TITLES:
    safe_name = sanitize(title)
    poster_file = os.path.join(POSTERS_DIR, f"{safe_name}.jpg")
    backdrop_file = os.path.join(BACKDROPS_DIR, f"{safe_name}.jpg")
    
    res = search_tmdb(title)
    if not res:
        print(f"Skipping {title}: not found on TMDB")
        continue
    
    p_path = res.get('poster_path')
    b_path = res.get('backdrop_path')
    
    print(f"[{title}] -> Poster: {p_path}, Backdrop: {b_path}")
    
    if p_path and not os.path.exists(poster_file):
        download_file(f"https://image.tmdb.org/t/p/w500{p_path}", poster_file)
    if b_path and not os.path.exists(backdrop_file):
        download_file(f"https://image.tmdb.org/t/p/w1280{b_path}", backdrop_file)
        
    metadata.append({
        "title": title,
        "key": safe_name,
        "overview": res.get("overview", "")
    })

print(f"Successfully fetched assets for {len(metadata)} titles!")
