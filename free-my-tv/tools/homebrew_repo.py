#!/usr/bin/env python3
"""Writes docs/webos/apps.json, the repository the webOS Homebrew Channel reads (https://freemytv.io/webos/apps.json).

    python3 tools/homebrew_repo.py FreeMyTV-webos.ipk

Run it at release time, on the exact .ipk uploaded to the GitHub release (built with `webos/build.py --generic`), and
commit the JSON with the release: the Channel downloads `ipkUrl`, checks this SHA-256 and size, and installs it with
the same Developer Mode installer webos/install.py uses. A JSON that no longer matches only fails the check.
The manifest goes inline, so the TV never has to fetch it from github.com.
"""
import hashlib
import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, 'docs', 'webos', 'apps.json')
ICON = 'https://freemytv.io/assets/icon-512.png'
SOURCE = 'https://github.com/simbasjungles/free-my-tv'


def write(sha256, size):
    """The repository for the version in webos/appinfo.json, whose release .ipk has this SHA-256 and size."""
    app = json.load(open(os.path.join(ROOT, 'webos', 'appinfo.json'), encoding='utf-8'))
    version = app['version']
    manifest = {
        'id': app['id'],
        'version': version,
        'type': 'web',
        'title': app['title'],
        'appDescription': 'A fast, ad-free home screen: your apps on a glass shelf, the weather and prices, day and night.',
        'iconUri': ICON,
        'sourceUrl': SOURCE,
        'rootRequired': False,
        'ipkUrl': f'{SOURCE}/releases/download/v{version}/FreeMyTV-webos.ipk',
        'ipkHash': {'sha256': sha256},
        'ipkSize': size,
    }
    repo = {
        'paging': {'page': 1, 'count': 1, 'maxPage': 1, 'itemsTotal': 1, 'prevUrl': None, 'nextUrl': None},
        'packages': [{
            'id': app['id'],
            'title': app['title'],
            'iconUri': ICON,
            'shortDescription': manifest['appDescription'],
            'pool': 'main',
            'manifest': manifest,
        }],
    }
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, 'w', encoding='utf-8') as f:
        json.dump(repo, f, indent=2)
        f.write('\n')
    print(f'Wrote {os.path.relpath(OUT, ROOT)}: {app["id"]} {version}, {size} bytes, sha256 {sha256[:12]}…')


def main():
    if len(sys.argv) != 2 or not sys.argv[1].endswith('.ipk'):
        print(__doc__.strip())
        return 2
    data = open(sys.argv[1], 'rb').read()
    write(hashlib.sha256(data).hexdigest(), len(data))
    return 0


if __name__ == '__main__':
    sys.exit(main())
