#!/usr/bin/env python3
"""Builds the Aerix Launcher website into docs/.

Run it from the repository root:  python3 tools/build_site.py

Every page is written from the templates below, so the header, footer and
navigation can never drift apart. The output is plain static files - no runtime,
no build step on the server, which is all GitHub Pages needs.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / "docs"


def current_version() -> str:
    """The version the site advertises, straight from the file the APKs are built from."""
    for line in (ROOT / "version.properties").read_text().splitlines():
        if line.startswith("versionName="):
            return line.split("=", 1)[1].strip()
    raise SystemExit("version.properties has no versionName")


VERSION = current_version()
RELEASE_DATE = "11 Nov 2026"

NAV = [
    ("index.html", "Home"),
    ("features.html", "Features"),
    ("renderers.html", "Renderers"),
    ("screenshots.html", "Gallery"),
    ("install.html", "Install"),
    ("faq.html", "FAQ"),
]

DOWNLOAD_URL = "https://github.com/entitybrian69-bit/Aerix-launcher/releases/latest/download/Aerix-Launcher.apk"
REPO_URL = "https://github.com/entitybrian69-bit/Aerix-launcher"
DISCORD_URL = "https://discord.gg/RS7q9KaCm6"
SITE_URL = "https://entitybrian69-bit.github.io/Aerix-launcher/"
OWNER_NAME = "entitybrian"
OWNER_ROLE = "Owner · Core developer · Tester · Maintainer"
OWNER_GITHUB = "https://github.com/entitybrian69-bit"
OWNER_AVATAR = "assets/creator-entitybrian.jpg"

THEMES = [
    ("teapots", "Tea Party", "drifts, ripples"),
    ("rockets", "Rocket Ride", "climbs, lifts"),
    ("nebula", "Nebula", "drifts, glows"),
    ("stardust", "Stardust", "twinkles"),
    ("aurora", "Aurora", "slides, sweeps"),
    ("galaxy", "Galaxy", "turns, glows"),
    ("moonrise", "Moonrise", "breathes"),
    ("horizon", "Event Horizon", "ring turns"),
    ("static", "Static", "jitters"),
    ("starcross", "Star Cross", "twinkles"),
    ("comet", "Comet Grid", "comet crosses"),
]

HD_CLIPS = [
    ("autumn-mountains", "Autumn Mountains", "4K"),
    ("butterfly-garden", "Butterfly Garden", "1440p"),
    ("sakura-field", "Sakura Field", "1440p"),
    ("flower-forest-train", "Flower Forest Train", "1440p"),
    ("night-sky", "Night Sky", "1080p"),
    ("wisteria-lake", "Wisteria Lake", "1080p"),
]

SHOTS = [
    ("shot-home", "Home", "The dashboard, with your world behind it"),
    ("shot-instances", "New instance", "Pick a version and a loader, and go"),
    ("shot-renderer", "Renderers", "Twelve backends, one picker"),
    ("shot-discover", "Discover", "Mods, packs, shaders and resource packs"),
    ("shot-accounts", "Skins", "Accounts, skins and capes in one place"),
    ("shot-wallpapers", "Wallpapers", "25 photographs and 6 HD clips"),
]


def head(title, description, page, extra=""):
    canonical = SITE_URL + ("" if page == "index.html" else page)
    image = SITE_URL + "assets/screenshots/shot-home.jpg"
    return f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<title>{title}</title>
<meta name="description" content="{description}">
<meta name="theme-color" content="#c8102e">
<meta name="color-scheme" content="light">
<link rel="icon" href="assets/aerix-logo.png">
<link rel="apple-touch-icon" href="assets/aerix-logo.png">
<link rel="canonical" href="{canonical}">
<meta property="og:type" content="website">
<meta property="og:site_name" content="Aerix Launcher">
<meta property="og:title" content="{title}">
<meta property="og:description" content="{description}">
<meta property="og:url" content="{canonical}">
<meta property="og:image" content="{image}">
<meta property="og:image:alt" content="The Aerix Launcher home screen">
<meta name="twitter:card" content="summary_large_image">
<link rel="stylesheet" href="site.css">
{extra}</head>
<body>
<div class="bg" aria-hidden="true">
  <span class="bg-grid"></span>
  <span class="bg-ribbon r1"></span>
  <span class="bg-ribbon r2"></span>
  <span class="bg-ribbon r3"></span>
  <span class="bg-dots"></span>
  <span class="bg-arc a1"></span>
  <span class="bg-arc a2"></span>
</div>
<a class="skip" href="#main">Skip to content</a>
"""


def nav(page):
    links = []
    for href, label in NAV:
        current = ' aria-current="page"' if href == page else ""
        links.append(f'      <a href="{href}"{current}>{label}</a>')
    joined = "\n".join(links)
    return f"""
<header class="nav">
  <div class="wrap nav-inner">
    <a class="brand" href="index.html">
      <img src="assets/aerix-logo.png" alt="" width="34" height="34">
      <span>AERIX<small>LAUNCHER</small></span>
    </a>
    <nav class="nav-links" id="navLinks" aria-label="Pages">
{joined}
    </nav>
    <a class="nav-cta" href="index.html#download">Download</a>
    <button class="nav-toggle" id="navToggle" type="button" aria-expanded="false" aria-controls="navLinks" aria-label="Open menu">
      <span></span>
    </button>
  </div>
</header>
"""


FOOTER = """
<footer class="footer">
  <div class="wrap">
    <div class="footer-grid">
      <div>
        <a class="brand" href="index.html">
          <img src="assets/aerix-logo.png" alt="" width="34" height="34">
          <span>AERIX<small>LAUNCHER</small></span>
        </a>
        <p class="small" style="max-width:32ch">
          Minecraft: Java Edition on Android — instances, renderers, accounts and a
          content browser in one launcher.
        </p>
      </div>
      <div>
        <h4>The launcher</h4>
        <ul>
          <li><a href="features.html">Features</a></li>
          <li><a href="renderers.html">Renderers</a></li>
          <li><a href="screenshots.html">Gallery</a></li>
          <li><a href="install.html">Install guide</a></li>
        </ul>
      </div>
      <div>
        <h4>Get it</h4>
        <ul>
          <li><a href="index.html#download">Download</a></li>
          <li><a href="https://github.com/entitybrian69-bit/Aerix-launcher/releases" target="_blank" rel="noopener">All releases</a></li>
          <li><a href="https://github.com/entitybrian69-bit/Aerix-launcher/releases/latest/download/SHA256SUMS" target="_blank" rel="noopener">Checksums</a></li>
          <li><a href="faq.html">FAQ</a></li>
        </ul>
      </div>
      <div>
        <h4>Project</h4>
        <ul>
          <li><a href="https://github.com/entitybrian69-bit/Aerix-launcher" target="_blank" rel="noopener">GitHub</a></li>
          <li><a href="https://discord.gg/RS7q9KaCm6" target="_blank" rel="noopener">Discord</a></li>
          <li><a href="https://github.com/entitybrian69-bit/Aerix-launcher/issues" target="_blank" rel="noopener">Report a bug</a></li>
          <li><a href="https://github.com/entitybrian69-bit/Aerix-launcher/blob/v3_openjdk/PRIVACY_POLICY.md" target="_blank" rel="noopener">Privacy</a></li>
        </ul>
      </div>
    </div>
    <div class="footer-credit">
      <img src="assets/creator-entitybrian.jpg" alt="" width="48" height="48" loading="lazy">
      <p>
        <b>entitybrian</b> — Owner · Core developer · Tester · Maintainer<br>
        <a href="index.html#creator">Who builds Aerix</a> · <a href="https://github.com/entitybrian69-bit" target="_blank" rel="noopener">GitHub profile</a>
      </p>
    </div>
    <div class="footer-bottom">
      <span>© <span id="year">2026</span> Aerix Launcher. Not affiliated with Mojang or Microsoft.</span>
      <span>Minecraft is a trademark of Mojang Synergies AB.</span>
    </div>
  </div>
</footer>
<script src="site.js" defer></script>
</body>
</html>
"""


def page_head_block(kicker, title, lede):
    return f"""
  <section class="page-head">
    <div class="wrap">
      <span class="kicker">{kicker}</span>
      <h1>{title}</h1>
      <p>{lede}</p>
    </div>
  </section>
"""


def theme_strip(limit=None):
    items = THEMES if limit is None else THEMES[:limit]
    cards = []
    for slug, name, note in items:
        cards.append(
            f'      <figure class="theme" title="{name} — {note}">\n'
            f'        <img src="assets/themes/{slug}.jpg" alt="{name} control theme" loading="lazy" width="420" height="420">\n'
            f'        <span>{name}</span>\n'
            f'      </figure>'
        )
    return '<div class="theme-strip">\n' + "\n".join(cards) + "\n    </div>"


def clip_grid():
    cards = []
    for slug, title, res in HD_CLIPS:
        cards.append(
            f'      <figure class="clip">\n'
            f'        <img src="assets/hd/{slug}.jpg" alt="{title} animation" loading="lazy" width="1200" height="675">\n'
            f'        <em>{res}</em>\n'
            f'        <span>{title}</span>\n'
            f'      </figure>'
        )
    return '<div class="clips">\n' + "\n".join(cards) + "\n    </div>"


def shot_grid(items):
    cells = []
    for slug, name, caption in items:
        cells.append(
            f'      <figure class="shot">\n'
            f'        <a href="assets/screenshots/{slug}.jpg" data-lightbox aria-label="Open {name} full size">\n'
            f'          <img src="assets/screenshots/{slug}.jpg" alt="{caption}" loading="lazy" width="1200" height="537">\n'
            f'        </a>\n'
            f'        <figcaption><b>{name}</b><span>{caption}</span></figcaption>\n'
            f'      </figure>'
        )
    return '<div class="shots">\n' + "\n".join(cells) + "\n    </div>"


def build_index():
    return head(
        "Aerix Launcher — Minecraft: Java Edition on Android",
        "Aerix Launcher runs Minecraft: Java Edition on Android. Instances, twelve renderers, "
        "accounts and skin wardrobe, eleven animated control themes, 25 wallpapers and a content "
        "browser — private by design, tested on real phones.",
        "index.html",
    ) + nav("index.html") + f"""
<main id="main">

  <section class="hero">
    <div class="wrap hero-grid">
      <div>
        <p class="eyebrow"><span class="dot" aria-hidden="true"></span>Version <span data-release-version>{VERSION}</span> is out</p>
        <h1>Minecraft: Java&nbsp;Edition, <span class="accent">on your phone</span>.</h1>
        <p class="lede">
          Instances, twelve renderers, accounts and a full skin wardrobe, animated control
          themes and a content browser — in one launcher that stays out of the way once the
          game is running.
        </p>
        <div class="btn-row">
          <a class="btn btn-primary" href="{DOWNLOAD_URL}" download data-primary-download>
            <span class="nowrap">Download for Android</span> <span class="btn-size">89 MB</span>
          </a>
          <a class="btn btn-ghost" href="#whats-new">What's new</a>
        </div>
        <p class="hero-meta">
          <span>Android 6.0+</span>
          <span>arm64, armv7 and universal builds</span>
          <span id="downloadFacts">v{VERSION} · {RELEASE_DATE}</span>
        </p>
      </div>
      <div class="device">
        <span class="glow" aria-hidden="true"></span>
        <img src="assets/screenshots/shot-home.jpg" alt="The Aerix Launcher home screen, with the dashboard over a Minecraft wallpaper" width="1200" height="537">
      </div>
    </div>
  </section>

  <section class="section" aria-label="At a glance" style="padding-top:clamp(28px,4vw,44px)">
    <div class="wrap">
      <div class="stats reveal">
        <div><strong data-count="12">12</strong><span>renderers, one picker</span></div>
        <div><strong data-count="11">11</strong><span>animated control themes</span></div>
        <div><strong data-count="31">31</strong><span>wallpapers, photos and clips</span></div>
        <div><strong>1.7–1.21+</strong><span>every Minecraft version</span></div>
      </div>
    </div>
  </section>

  <div class="ticker" aria-hidden="true">
    <div class="ticker-track">
      <span>Instances</span><span>Twelve renderers</span><span>Control themes</span><span>Accounts and skins</span><span>Content browser</span><span>HD clips</span><span>No telemetry</span><span>Windows, worlds, mods</span>
      <span>Instances</span><span>Twelve renderers</span><span>Control themes</span><span>Accounts and skins</span><span>Content browser</span><span>HD clips</span><span>No telemetry</span><span>Windows, worlds, mods</span>
    </div>
  </div>

  <section class="section" id="whats-new" aria-labelledby="log-title">
    <div class="wrap">
      <div class="section-head reveal">
        <span class="kicker">The latest game log</span>
        <h2 id="log-title">What <span style="color:var(--crimson)">{VERSION}</span> includes</h2>
        <p>
          Every release writes its own log, and this is the current one - the change you can
          feel first, then the details that make it hold. The full history is in the release notes.
        </p>
      </div>

      <div class="log reveal">
        <div class="log-bar">
          <span class="lights" aria-hidden="true"><i></i><i></i><i></i></span>
          <code>aerix@launcher:~$ release-notes --current</code>
          <span class="log-tag">v{VERSION}</span>
        </div>
        <ol class="log-rows">
          <li>
            <span class="n">01</span>
            <span><b>The launcher is its own app now</b>
            Aerix installs as <code>net.al.aerixlauncher</code> instead of borrowing the upstream package. Your instances, settings and game data are imported on first launch, the old install is left alone as a rollback, and an update can never overwrite the app it copied from.</span>
          </li>
          <li>
            <span class="n">02</span>
            <span><b>One memory ceiling, not two</b>
            The launcher heap and the game heap are budgeted from the same device reading, so a phone that was never able to grant 6 GB stops offering it and says so. Devices that can still take it keep a "Try 6 GB anyway" override.</span>
          </li>
          <li>
            <span class="n">03</span>
            <span><b>The A menu stays where you left it</b>
            Releasing the A button inside the overlay keeps the menu in place. A hide now needs the gesture to start below the system strip and a 240 ms hold, a swipe past the top edge no longer blanks the controls mid-game, and an overlay the launcher did not hide says why on its own pill.</span>
          </li>
          <li>
            <span class="n">04</span>
            <span><b>Mouse aim and cursors behave</b>
            The sensitivity that scales both axes and the per-axis aim sensitivity are separate settings again, cursor skins apply to the overlay, the custom cursor size slider works, and a cursor recorded through the overlay plays back at the size it was drawn at.</span>
          </li>
          <li>
            <span class="n">05</span>
            <span><b>Recordings you can find again</b>
            Clips land in a library you import, share and delete from inside the launcher, with a recents row on Discover - and a take interrupted before it was finalised is kept as <code>-unfinished.mp4</code> instead of thrown away.</span>
          </li>
          <li>
            <span class="n">06</span>
            <span><b>2.3.1: the two crashes players reported</b>
            A display question asked from the wrong kind of Context was killing the launcher on start on Android 11+, and the runtime dialog crashed while trying to explain a failed install. Both are fixed, and the surface rate read that went with them is guarded.</span>
          </li>
          <li>
            <span class="n">07</span>
            <span><b>2.3.2: sorry - the missing Java was ours, not your internet</b>
            That same crash skipped the two startup lines after it, which were the CPU read and the bundled runtime unpack, so the launcher went looking for a runtime named after no architecture at all and blamed the connection when it could not find one. Startup steps are independent now, the architecture is worked out on demand, an install failure carries the reason behind "Show more", and the crash screen keeps the trace without asking for a permission.</span>
          </li>
        </ol>
        <div class="log-foot">
          <span>Minor release · published {RELEASE_DATE} · Android 6.0+ · arm64-v8a, armeabi-v7a and universal</span>
          <a class="btn btn-sm" href="https://github.com/entitybrian69-bit/Aerix-launcher/releases/latest" target="_blank" rel="noopener" data-release-link>
            Read the full notes
          </a>
        </div>
      </div>
    </div>
  </section>

  <section class="section" aria-labelledby="themes-title">
    <div class="wrap">
      <div class="section-head reveal">
        <span class="kicker">The stand-out feature</span>
        <h2 id="themes-title">Controls that look like something</h2>
        <p>
          Open the control editor and scroll to <b>Control themes</b>. The strip is the artwork
          itself: tap one and the control wears it, with its own press animation, haptic tick and
          key sound. Off puts the plain control back, and one button applies the theme you picked
          to every control in the layout.
        </p>
      </div>
      <div class="reveal">
        {theme_strip()}
      </div>
      <p class="small muted mt-32">
        <b>Move it</b> freezes the motion if you would rather have a still themed control;
        <b>Feel it</b> and <b>Hear it</b> switch the haptics and the click. Themes only ever change
        paint — a key press is still sent on the frame your finger lands, so nothing is delayed.
      </p>
    </div>
  </section>

  <section class="section" aria-labelledby="features-title">
    <div class="wrap">
      <div class="section-head reveal">
        <span class="kicker">Everything in the box</span>
        <h2 id="features-title">Built to be used, not configured</h2>
        <p>No first-run wizard, no hidden paywall, no account you have to make for the launcher itself.</p>
      </div>
      <div class="grid grid-3 reveal">
        <article class="card">
          <div class="icon" aria-hidden="true">◆</div>
          <h3>Instances</h3>
          <p>Vanilla, Fabric, Forge, NeoForge, Quilt and OptiFine — each instance keeps its own
             version, mods, renderer and settings, and shows its loader's own mark.</p>
          <div class="tag-row"><span class="tag">per-instance settings</span></div>
        </article>
        <article class="card">
          <div class="icon" aria-hidden="true">◇</div>
          <h3>Twelve renderers</h3>
          <p>Emesis is bundled and works almost everywhere. Zink, LTW, GL4ES, MobileGlues and
             the rest are one tap away when a device wants something else.</p>
          <div class="tag-row"><span class="tag">auto fallback</span></div>
        </article>
        <article class="card">
          <div class="icon" aria-hidden="true">○</div>
          <h3>Accounts &amp; skins</h3>
          <p>Microsoft, Ely.by and offline accounts, each with its own skin, cape and
             Wide/Slim model, previewed on a character you can spin.</p>
          <div class="tag-row"><span class="tag">skin wardrobe</span></div>
        </article>
        <article class="card">
          <div class="icon" aria-hidden="true">◈</div>
          <h3>Content browser</h3>
          <p>Search CurseForge and Modrinth for mods, modpacks, shaders and resource packs,
             and install them straight into the instance you are building.</p>
          <div class="tag-row"><span class="tag">publisher artwork</span></div>
        </article>
        <article class="card">
          <div class="icon" aria-hidden="true">▣</div>
          <h3>Custom controls</h3>
          <p>Move, resize and restyle every button, add drawers and joysticks, import a
             <code>.json</code> layout from elsewhere — or start from the built-in pad.</p>
          <div class="tag-row"><span class="tag">editor built in</span></div>
        </article>
        <article class="card">
          <div class="icon" aria-hidden="true">◐</div>
          <h3>Look and feel</h3>
          <p>25 photographs and six recorded HD clips, an accent that follows what you pick,
             and an in-game menu built around your character.</p>
          <div class="tag-row"><span class="tag">no seams</span></div>
        </article>
      </div>
      <p class="mt-32"><a href="features.html">The full feature list →</a></p>
    </div>
  </section>

  <section class="section" aria-labelledby="gallery-title">
    <div class="wrap">
      <div class="section-head reveal">
        <span class="kicker">Gallery</span>
        <h2 id="gallery-title">What it actually looks like</h2>
        <p>Real screenshots from the released build — no mockups.</p>
      </div>
      <div class="reveal">
        {shot_grid(SHOTS[:3])}
      </div>
      <p class="mt-32"><a href="screenshots.html">All six screens →</a></p>
    </div>
  </section>

  <section class="section" aria-labelledby="clips-title">
    <div class="wrap">
      <div class="section-head reveal">
        <span class="kicker">Wallpapers</span>
        <h2 id="clips-title">Six animations, recorded in game</h2>
        <p>
          Full quality, downloaded on demand, looping over one or two seconds. Nothing is
          decoded while you play — they only run on the launcher screens.
        </p>
      </div>
      <div class="reveal">
        {clip_grid()}
      </div>
    </div>
  </section>

  <section class="section" id="creator" aria-labelledby="creator-title">
    <div class="wrap">
      <div class="section-head reveal">
        <span class="kicker">Who builds this</span>
        <h2 id="creator-title">One person, on purpose</h2>
        <p>
          Aerix is not a studio product. It is designed, coded, tested on real phones and shipped
          by its owner — so the person answering your issue is the person who wrote the screen you
          are asking about.
        </p>
      </div>

      <div class="creator-grid">
        <article class="creator-card reveal">
          <img class="creator-logo" src="{OWNER_AVATAR}" alt="{OWNER_NAME}, owner and core developer of Aerix Launcher"
               width="736" height="736" loading="lazy">
          <div class="creator-body">
            <h3>{OWNER_NAME}</h3>
            <p class="creator-role">{OWNER_ROLE}</p>
            <p class="creator-bio">
              Writes the launcher, the controls and the interface, cuts every release, and keeps the
              issue tracker. No outsourced screens and no generated layouts — the same hands build it
              and test it on another phone before it ships.
            </p>
            <div class="creator-links">
              <a class="btn btn-sm btn-ghost" href="{OWNER_GITHUB}" target="_blank" rel="noopener">GitHub profile</a>
              <a class="btn btn-sm btn-ghost" href="{DISCORD_URL}" target="_blank" rel="noopener">Discord</a>
              <a class="btn btn-sm btn-ghost" href="{REPO_URL}/issues" target="_blank" rel="noopener">Report a bug</a>
            </div>
          </div>
        </article>

        <aside class="counter-card reveal" aria-labelledby="counter-title">
          <span class="kicker" id="counter-title">Live from GitHub</span>
          <p class="counter-number" data-downloads-box>
            <span data-downloads>—</span>
          </p>
          <p class="counter-label">APK downloads, counted across every release</p>
          <dl class="counter-meta">
            <div><dt>Latest release</dt><dd>v<span data-release-version>{VERSION}</span></dd></div>
            <div><dt>Published</dt><dd><span data-release-date>{RELEASE_DATE}</span></dd></div>
            <div><dt>Source</dt><dd><a href="{REPO_URL}/releases" target="_blank" rel="noopener">github.com · releases</a></dd></div>
          </dl>
          <p class="counter-note">
            Read from the GitHub API when this page loads: it is the sum of the download counts the
            release files report, not a number kept by hand.
          </p>
        </aside>
      </div>
    </div>
  </section>

  <section class="section" id="download">
    <div class="wrap">
      <div class="download reveal">
        <span class="kicker">Download</span>
        <h2>Get Aerix Launcher <span class="muted" style="font-size:0.55em;font-weight:600">v<span data-release-version>{VERSION}</span></span></h2>
        <p class="muted" style="max-width:56ch">
          One APK per architecture, so nothing you cannot use is in the file you download. Not
          sure which one? Take the first — it covers almost every phone made since 2016.
        </p>

        <div class="dl-grid">
          <a class="dl-card" href="{DOWNLOAD_URL}" download data-dl="arm64">
            <strong>arm64-v8a <span class="tag hot">recommended</span></strong>
            <small class="asset">Aerix-Launcher-{VERSION}-arm64-v8a.apk</small>
            <span class="size">88.8 MB</span>
            <small>64-bit phones — nearly all of them</small>
          </a>
          <a class="dl-card" href="https://github.com/entitybrian69-bit/Aerix-launcher/releases/latest" target="_blank" rel="noopener" data-dl="armv7">
            <strong>armeabi-v7a</strong>
            <small class="asset">Aerix-Launcher-{VERSION}-armeabi-v7a.apk</small>
            <span class="size">89.0 MB</span>
            <small>Older 32-bit phones</small>
          </a>
          <a class="dl-card" href="{DOWNLOAD_URL}" download data-dl="universal">
            <strong>Universal</strong>
            <small class="asset">Aerix-Launcher-{VERSION}.apk</small>
            <span class="size">144.0 MB</span>
            <small>Every architecture, including x86 — the fallback</small>
          </a>
        </div>

        <div class="dl-foot">
          <span>Also on the release: <a href="https://github.com/entitybrian69-bit/Aerix-launcher/releases/latest/download/Aerix-Launcher-noruntime.apk" download>no-runtime build</a> (116.2 MB) — same launcher, Java fetched on first launch.</span>
          <span><a href="https://github.com/entitybrian69-bit/Aerix-launcher/releases/latest/download/SHA256SUMS" target="_blank" rel="noopener">SHA-256 checksums</a></span>
          <span>Downloaded <b data-downloads>—</b> times from GitHub</span>
          <span>Signed with the project key · updates in place</span>
        </div>
      </div>
    </div>
  </section>

  <section class="section">
    <div class="wrap">
      <div class="grid grid-3 reveal">
        <article class="card spotlight">
          <h3>Free, and no account to make</h3>
          <p>Aerix has no sign-up of its own. Bring a Microsoft account, an Ely.by account, or
             play offline — nothing about you is sent anywhere but the login you chose.</p>
        </article>
        <article class="card">
          <h3>Your data stays on your phone</h3>
          <p>Instances, worlds, accounts and settings all live in the launcher's own storage.
             No telemetry, no analytics, no ads.</p>
        </article>
        <article class="card">
          <h3>Updates itself</h3>
          <p>The in-app updater reads the newest release, picks the build that matches your
             device, and installs over the old one without touching your data.</p>
        </article>
      </div>
    </div>
  </section>

</main>
""" + FOOTER

def build_features():
    return head(
        "Features — Aerix Launcher",
        "Everything in Aerix Launcher: instances for every modloader, twelve renderers, accounts "
        "and skin wardrobe, animated control themes, custom controls, 31 wallpapers, a content "
        "browser and an in-game menu built around your character.",
        "features.html",
    ) + nav("features.html") + page_head_block(
        "Features",
        "Everything in the box",
        "Aerix is one app: it installs Minecraft, runs it, and gets out of the way. Here is what "
        "is actually in it — no roadmap, no promises, just the shipped build.",
    ) + f"""
<main id="main">

  <section class="section" id="instances">
    <div class="wrap">
      <div class="section-head reveal">
        <span class="kicker">Instances</span>
        <h2>One per world, one per setup</h2>
        <p>Every instance is a sealed box: its own game version, loader, mods, configs, renderer
           and memory. Break one and the others do not care.</p>
      </div>
      <div class="grid grid-3 reveal">
        <article class="card"><h3>Every loader</h3><p>Vanilla, Fabric, Forge, NeoForge, Quilt and OptiFine, with the loader version chosen for you when there is a sensible default.</p></article>
        <article class="card"><h3>Every version</h3><p>Releases, snapshots and old betas, from the newest 1.21+ back to the versions people still build on.</p></article>
        <article class="card"><h3>Its own renderer</h3><p>An old phone can run one instance on GL4ES and a newer one on Zink, without changing anything globally.</p></article>
        <article class="card"><h3>Its own memory</h3><p>Allocate what the mods need per instance instead of one global number that suits none of them.</p></article>
        <article class="card"><h3>Its own controls</h3><p>Different layouts for different games: a PvP pad on one instance, a modded keyboard-heavy layout on another.</p></article>
        <article class="card"><h3>Loader's own mark</h3><p>Instance cards show the loader's logo, and packs installed from Discover carry the publisher's artwork.</p></article>
      </div>
    </div>
  </section>

  <section class="section" id="renderers">
    <div class="wrap">
      <div class="section-head reveal">
        <span class="kicker">Renderers</span>
        <h2>Twelve backends, one picker</h2>
        <p>
          Android GPUs and drivers vary wildly, so no single renderer suits every phone. Aerix
          ships several and lets you change them per instance — and in 1.5.5 a renderer whose
          library is missing is replaced instead of taking the launch down with it.
        </p>
      </div>
      <div class="grid grid-2 reveal">
        <article class="card spotlight">
          <h3>Emesis, bundled</h3>
          <p>The default. A GL-to-GLES translation layer that works on the widest range of
             Adreno and Mali devices, and needs no plugin APK to be installed.</p>
        </article>
        <article class="card">
          <h3>Then, if it does not run</h3>
          <p>Try <b>Zink</b> on a phone with a solid Vulkan driver, <b>LTW</b> for modern
             versions, or <b>GL4ES</b> for older ones. The full lineup and what each is for is
             on the renderer page.</p>
          <p class="mt-32"><a href="renderers.html">Compare the renderers →</a></p>
        </article>
      </div>
    </div>
  </section>

  <section class="section" id="controls">
    <div class="wrap">
      <div class="section-head reveal">
        <span class="kicker">Controls</span>
        <h2>Eleven themes, and an editor that respects them</h2>
        <p>
          Every control can wear a piece of animated pixel art, with its own press effect, haptic
          and key sound. The theme travels with the layout file, so a setup you share arrives
          looking the way you made it.
        </p>
      </div>
      <div class="reveal">{theme_strip()}</div>
      <div class="grid grid-3 reveal mt-32">
        <article class="card"><h3>Theme, or not</h3><p><b>Off</b> is a chip like any other, and <b>Move it</b> freezes the animation entirely if you want a still control.</p></article>
        <article class="card"><h3>Feel and sound</h3><p>Each theme carries a haptic pattern and a system key click, both switchable, neither needing a permission or an asset.</p></article>
        <article class="card"><h3>Apply to all</h3><p>Pick one for the control you are editing, then push it across the whole layout in one tap.</p></article>
      </div>
    </div>
  </section>

  <section class="section" id="controls-editor">
    <div class="wrap">
      <div class="section-head reveal">
        <span class="kicker">Custom controls</span>
        <h2>Every button is yours</h2>
      </div>
      <div class="grid grid-3 reveal">
        <article class="card"><h3>Place and size</h3><p>Drag to move, drag the handle to resize, with snapping to edges and to other controls.</p></article>
        <article class="card"><h3>Keys and specials</h3><p>Up to four keycodes per button, plus mouse buttons, scroll, keyboard toggles, GUI and menu.</p></article>
        <article class="card"><h3>Drawers and sticks</h3><p>Sub-buttons in pull-out drawers, and joysticks with deadzone, size and absolute tracking.</p></article>
        <article class="card"><h3>Pictures</h3><p>Crop an image into any button as its background, or style it with a colour, stroke and corner radius.</p></article>
        <article class="card"><h3>Sharing</h3><p>Layouts are plain JSON, so they import and export cleanly between devices and other launchers.</p></article>
        <article class="card"><h3>Per-instance</h3><p>Keep a keyboard-heavy layout for modded packs and a thumb-friendly pad for vanilla survival.</p></article>
      </div>
    </div>
  </section>

  <section class="section" id="accounts">
    <div class="wrap">
      <div class="section-head reveal">
        <span class="kicker">Accounts</span>
        <h2>Sign in once, switch whenever</h2>
        <p>Microsoft, Ely.by and offline accounts live side by side, each with its own skin,
           cape and model. In game, the account panel can switch: confirm the restart and the
           launcher comes back as the account you picked, with the game already starting.</p>
      </div>
      <div class="grid grid-3 reveal">
        <article class="card"><h3>Microsoft</h3><p>The official sign-in, in the browser or in-app, with the token stored on your device only.</p></article>
        <article class="card"><h3>Ely.by</h3><p>For the skins and capes people use outside the official service.</p></article>
        <article class="card"><h3>Offline</h3><p>Any username, for testing modpacks and for playing without a network.</p></article>
      </div>
    </div>
  </section>

  <section class="section" id="skins">
    <div class="wrap">
      <div class="section-head reveal">
        <span class="kicker">Skin wardrobe</span>
        <h2>Skins, capes and a character that turns</h2>
        <p>Equip a skin from storage or from the skin search, change Wide/Slim, add a cape, and
           keep saved presets per account. The character on the right of the in-game menu is that
           same skin — drag it and it spins under your thumb.</p>
      </div>
      <div class="grid grid-2 reveal">
        <article class="card"><h3>Presets per account</h3><p>Keep several looks and switch between them without re-importing the file.</p></article>
        <article class="card"><h3>It reaches the game</h3><p>The skin, cape and model are written where the game reads them, so what you pick is what you see in game.</p></article>
      </div>
    </div>
  </section>

  <section class="section" id="look">
    <div class="wrap">
      <div class="section-head reveal">
        <span class="kicker">Look</span>
        <h2>31 wallpapers, no seams</h2>
        <p>25 photographs and six recorded HD clips, plus any clip you import yourself. The
           accent colour follows what you choose, and nothing is stretched over the interface
           with a strip of blur down the middle.</p>
      </div>
      <div class="reveal">{clip_grid()}</div>
    </div>
  </section>

  <section class="section" id="browser">
    <div class="wrap">
      <div class="section-head reveal">
        <span class="kicker">Content</span>
        <h2>A browser that installs</h2>
        <p>Search CurseForge and Modrinth from inside the launcher, filter by version, loader and
           category, then install straight into the instance you are building.</p>
      </div>
      <div class="grid grid-4 reveal">
        <article class="card"><h3>Mods</h3><p>With the loader it needs and its dependencies.</p></article>
        <article class="card"><h3>Modpacks</h3><p>Publisher artwork, resolved loader and version.</p></article>
        <article class="card"><h3>Shaders</h3><p>For the versions and renderers that can use them.</p></article>
        <article class="card"><h3>Resource packs</h3><p>Installed into the selected instance.</p></article>
      </div>
    </div>
  </section>

  <section class="section" id="ingame">
    <div class="wrap">
      <div class="section-head reveal">
        <span class="kicker">In game</span>
        <h2>A menu you can actually use</h2>
        <p>Two halves, nothing in the way: actions down the left, and the account you are playing
           as down the right, on a character you can spin. The FPS readout hides while it is open.</p>
      </div>
      <div class="grid grid-3 reveal">
        <article class="card"><h3>Resume first</h3><p>The way out is the first, largest target — not buried under nine other buttons.</p></article>
        <article class="card"><h3>Log output, in game</h3><p>Read the log without leaving the game, which is what you want when a mod misbehaves.</p></article>
        <article class="card"><h3>Screen recorder</h3><p>Capture a clip of the game from the same menu.</p></article>
        <article class="card"><h3>Custom keycode</h3><p>Send a key the layout does not have, without editing anything.</p></article>
        <article class="card"><h3>Quick settings</h3><p>The handful of things worth changing mid-session.</p></article>
        <article class="card"><h3>Force close</h3><p>When a world will not let go, close the game cleanly instead of fighting the task switcher.</p></article>
      </div>
    </div>
  </section>

  <section class="section">
    <div class="wrap">
      <div class="download reveal">
        <span class="kicker">Ready?</span>
        <h2>Install it in about a minute</h2>
        <p class="muted" style="max-width:52ch">Download the APK, allow the install, pick an account, make an instance. The guide walks through each step.</p>
        <div class="btn-row">
          <a class="btn btn-primary" href="index.html#download">Download Aerix Launcher</a>
          <a class="btn btn-ghost" href="install.html">Install guide</a>
        </div>
      </div>
    </div>
  </section>

</main>
""" + FOOTER


def build_renderers():
    rows = [
        ("Emesis", "Bundled, default below 1.17", "GL → GLES translation. The widest compatibility across Adreno and Mali, and needs no plugin APK. Every instance under 1.17 starts on it.", "Start here"),
        ("Zink", "Mesa on Vulkan", "Desktop OpenGL implemented on top of Vulkan. Fast on phones with a good Vulkan driver; poor on bad ones.", "New phones"),
        ("Zink (Legacy)", "Mesa on Vulkan", "The older Zink build, aimed at 1.8 – 1.16 era games and mods.", "Old versions"),
        ("OpenLTW", "Bundled, default at 1.17+", "A modern translation path that suits current versions and modern mod loaders. Every instance at 1.17 and newer starts on it.", "Modern versions"),
        ("GL4ES", "Classic GLES1/2 shim", "The long-standing fallback. Broadest device support, lowest feature level; used automatically if nothing else can load.", "Last resort"),
        ("NG-GL4ES", "Newer GL4ES", "A newer GL4ES build with fixes the original never got.", "GL4ES with fixes"),
        ("MG-ES", "MobileGlues", "A GL 4.x translation layer for modern versions, 1.17+ only. Wants a capable GPU, and the launcher warns you before you pick it: it is experimental and we do not recommend it.", "Not recommended"),
        ("SFPEW", "Fixed-pipeline wrapper", "Simple FPE Wrapper: translates the fixed-function GL that old mods and Angelica-style mods rely on.", "Old mods"),
        ("Mesa", "Desktop GL via Mesa", "Mesa's software/hardware desktop GL path, for the cases that need real desktop GL semantics.", "Experimental"),
        ("Mesa (external)", "External Mesa build", "The same, using a Mesa build supplied outside the launcher.", "Experimental"),
        ("Freedreno", "Adreno Mesa driver", "Mesa's open Adreno driver. Device-specific, and only useful on hardware it supports.", "Adreno, advanced"),
        ("Proton", "32-bit only", "For 32-bit setups that need a Proton-style GL path.", "32-bit only"),
    ]
    body = "\n".join(
        f"            <tr><td><b>{name}</b></td><td>{kind}</td><td>{what}</td><td><span class=\"tag\">{when}</span></td></tr>"
        for name, kind, what, when in rows
    )
    return head(
        "Renderers — Aerix Launcher",
        "Aerix Launcher ships twelve renderers: Emesis, Zink, OpenLTW, GL4ES, MobileGlues, SFPEW and more. "
        "What each one is for, which to try first, and how the automatic fallback works.",
        "renderers.html",
    ) + nav("renderers.html") + page_head_block(
        "Renderers",
        "Twelve ways to draw the same world",
        "Android GPUs, drivers and game versions disagree about almost everything. Aerix ships "
        "several translation layers so a phone that will not run one renderer can still run the game.",
    ) + f"""
<main id="main">

  <section class="section">
    <div class="wrap">
      <div class="callout reveal">
        <span aria-hidden="true">◆</span>
        <p><strong>Nothing to choose.</strong> The automatic pick follows the game version: <strong>1.17
        and newer run on OpenLTW</strong>, and <strong>anything below 1.17 runs on Emesis</strong>. Both are
        bundled, need no plugin APK, and are set per instance. MobileGlues, Zink and the rest stay
        available in the renderer list for the cases that need them - change the renderer per
        instance, not globally, so a game that needs Zink does not force everything else onto it.</p>
      </div>

      <div class="table-wrap reveal mt-32">
        <table>
          <caption class="small muted" style="text-align:left;padding:14px 18px">The full lineup, as it ships in {VERSION}.</caption>
          <thead>
            <tr><th scope="col">Renderer</th><th scope="col">What it is</th><th scope="col">What it does</th><th scope="col">Reach for it when</th></tr>
          </thead>
          <tbody>
{body}
          </tbody>
        </table>
      </div>
    </div>
  </section>

  <section class="section">
    <div class="wrap">
      <div class="section-head reveal">
        <span class="kicker">New in 1.5.5</span>
        <h2>A missing library no longer kills the launch</h2>
        <p>
          A renderer names the OpenGL library LWJGL should load. When that library is not on
          disk — a plugin APK pointing at a path that does not exist, most often — the game used
          to die during startup, before the renderer picker was reachable.
        </p>
      </div>
      <div class="grid grid-3 reveal">
        <article class="card"><h3>Checked first</h3><p>The library is resolved <b>before</b> the JVM starts, so a bad path is caught while there is still a launcher to say so.</p></article>
        <article class="card"><h3>Bundled Emesis</h3><p>The replacement is the Emesis that ships with the launcher — the same translation layer most plugins provide — so you stay on the renderer you meant to use.</p></article>
        <article class="card"><h3>Then GL4ES</h3><p>If Emesis is the one that failed, GL4ES takes over rather than the game closing.</p></article>
      </div>
      <div class="callout warn mt-32">
        <span aria-hidden="true">!</span>
        <p><strong>If a game still refuses to start:</strong> open the instance settings, try another
        renderer, and lower the resolution. If you have an Emesis plugin APK installed, remove it —
        the launcher already carries its own.</p>
      </div>
    </div>
  </section>

  <section class="section">
    <div class="wrap">
      <div class="section-head reveal">
        <span class="kicker">Choosing</span>
        <h2>A short decision tree</h2>
      </div>
      <ol class="steps reveal">
        <li><h3>Does it start on the renderer it picked?</h3><p>Stop there. A 1.17+ instance starts on OpenLTW and an older one starts on Emesis, and both are the default because they work on the most hardware.</p></li>
        <li><h3>Black screen, crash on launch, or unreadable graphics?</h3><p>Try <b>Zink</b> if your phone has a recent Vulkan driver, otherwise <b>OpenLTW</b> for modern versions.</p></li>
        <li><h3>Running something old — 1.8 to 1.16?</h3><p><b>Zink (Legacy)</b> or <b>GL4ES</b>, and expect to lower the resolution.</p></li>
        <li><h3>Modded, with graphics mods?</h3><p><b>MG-ES</b> or <b>Zink</b> if the GPU is capable; shaders and Sodium-style mods are where renderer choice matters most.</p></li>
        <li><h3>Nothing works?</h3><p>Turn the resolution down before changing anything else. It fixes more launches than any renderer swap.</p></li>
      </ol>
    </div>
  </section>

</main>
""" + FOOTER


def build_screenshots():
    return head(
        "Gallery — Aerix Launcher",
        "Real screenshots of Aerix Launcher: the dashboard, instance creation, the renderer picker, "
        "Discover, the skin wardrobe and the wallpaper picker.",
        "screenshots.html",
    ) + nav("screenshots.html") + page_head_block(
        "Gallery",
        "The launcher, as it ships",
        "Screenshots from the released build, on a phone, with the wallpaper system doing its job. "
        "Tap any of them for the full-size image.",
    ) + f"""
<main id="main">
  <section class="section">
    <div class="wrap">
      <div class="reveal">{shot_grid(SHOTS)}</div>
      <p class="small muted mt-32">Tap a screen to open it full size; press Escape or tap outside to close.</p>
    </div>
  </section>

  <section class="section">
    <div class="wrap">
      <div class="section-head reveal">
        <span class="kicker">Animations</span>
        <h2>Six HD clips, on demand</h2>
        <p>Recorded in game at up to 4K, looping over one or two seconds, downloaded only when
           you pick one. A real frame from each clip stands in for it in the picker.</p>
      </div>
      <div class="reveal">{clip_grid()}</div>
    </div>
  </section>

  <section class="section">
    <div class="wrap">
      <div class="section-head reveal">
        <span class="kicker">Photographs</span>
        <h2>25 stills, from everywhere in the world</h2>
        <p>Overworld mornings, the End, the Nether, deep caves and open water — all full
           resolution, all following the accent colour of the interface.</p>
      </div>
      <div class="clips reveal">
        <figure class="clip"><img src="assets/wp/lush-caves.jpg" alt="A lush cave" loading="lazy" width="1400" height="788"><span>Lush glowberry cave</span></figure>
        <figure class="clip"><img src="assets/wp/sakura-sunbeams.jpg" alt="Sakura grove in sunlight" loading="lazy" width="1400" height="788"><span>Sakura sunbeams</span></figure>
        <figure class="clip"><img src="assets/wp/badlands-canyon.jpg" alt="Badlands canyon at dusk" loading="lazy" width="1400" height="788"><span>Badlands canyon</span></figure>
        <figure class="clip"><img src="assets/wp/frozen-glacier.jpg" alt="A frozen glacier" loading="lazy" width="1400" height="788"><span>Frozen glacier</span></figure>
        <figure class="clip"><img src="assets/wp/night-clouds.jpg" alt="Clouds at night" loading="lazy" width="1400" height="788"><span>Night clouds</span></figure>
        <figure class="clip"><img src="assets/wp/the-end.jpg" alt="The End dimension" loading="lazy" width="1400" height="788"><span>The End</span></figure>
      </div>
    </div>
  </section>
</main>
""" + FOOTER


def build_install():
    return head(
        "Install — Aerix Launcher",
        "How to install Aerix Launcher on Android: which APK to pick, allowing the install, first "
        "launch, adding an account and creating an instance — plus how updates work.",
        "install.html",
    ) + nav("install.html") + page_head_block(
        "Install",
        "About a minute, start to a world",
        "Aerix is a plain APK, installed from outside the Play Store. Android will warn you once; "
        "that warning is normal for any app installed this way.",
    ) + f"""
<main id="main">
  <section class="section">
    <div class="wrap">
      <div class="grid grid-2" style="align-items:start">
        <div>
          <ol class="steps reveal">
            <li><h3>Download the right file</h3><p>Almost every phone made since 2016 wants <b>arm64-v8a</b>. If yours is older, take <b>armeabi-v7a</b>; if you are unsure, the universal build always works.</p></li>
            <li><h3>Allow the install</h3><p>Your browser will ask whether to allow installs from this source. Allow it once, then tap the downloaded file.</p></li>
            <li><h3>Add an account</h3><p>Open the launcher, tap the avatar at the bottom of the rail, and sign in with Microsoft, Ely.by, or make an offline account.</p></li>
            <li><h3>Create an instance</h3><p>Pick a version and a loader. For a first run, vanilla with no mods is the fastest way to find out whether the device and renderer are happy.</p></li>
            <li><h3>Press play</h3><p>The first launch downloads the game and the Java runtime it needs. Later launches start straight into the game.</p></li>
          </ol>
        </div>
        <div class="reveal">
          <div class="download">
            <h3 style="margin-top:0">Which APK?</h3>
            <div class="dl-grid" style="grid-template-columns:1fr;margin-top:14px">
              <a class="dl-card" href="{DOWNLOAD_URL}" download data-dl="arm64">
                <strong>arm64-v8a <span class="tag hot">recommended</span></strong>
                <small class="asset">Aerix-Launcher-{VERSION}-arm64-v8a.apk</small>
                <span class="size">88.8 MB</span>
                <small>64-bit phones — nearly all of them</small>
              </a>
              <a class="dl-card" href="{DOWNLOAD_URL}" download data-dl="armv7">
                <strong>armeabi-v7a</strong>
                <small class="asset">Aerix-Launcher-{VERSION}-armeabi-v7a.apk</small>
                <span class="size">89.0 MB</span>
                <small>Older 32-bit phones</small>
              </a>
              <a class="dl-card" href="{DOWNLOAD_URL}" download data-dl="universal">
                <strong>Universal</strong>
                <small class="asset">Aerix-Launcher-{VERSION}.apk</small>
                <span class="size">144.0 MB</span>
                <small>Every architecture — the safe fallback</small>
              </a>
            </div>
            <p class="small muted mt-32">
              The <a href="https://github.com/entitybrian69-bit/Aerix-launcher/releases/latest/download/Aerix-Launcher-noruntime.apk" download>no-runtime build</a>
              is smaller because it fetches Java on first launch instead of carrying it.
            </p>
          </div>
        </div>
      </div>

      <div class="callout mt-32">
        <span aria-hidden="true">◆</span>
        <p><strong>Updating:</strong> install the new APK over the old one. Do not uninstall first —
        that deletes your instances, worlds, accounts and wallpapers. The in-app updater does this
        for you and picks the build that matches your device.</p>
      </div>
    </div>
  </section>

  <section class="section">
    <div class="wrap">
      <div class="section-head reveal">
        <span class="kicker">Requirements</span>
        <h2>What it needs</h2>
      </div>
      <div class="grid grid-3 reveal">
        <article class="card"><h3>Android 6.0 or newer</h3><p>Both 64-bit and 32-bit devices are supported, with a build for each.</p></article>
        <article class="card"><h3>About 2 GB of storage</h3><p>More for modded instances — a big pack plus its Java runtime grows quickly.</p></article>
        <article class="card"><h3>2 GB of RAM, realistically 3+</h3><p>Vanilla is comfortable on 2 GB; modpacks want more, and the launcher lets you allocate it per instance.</p></article>
      </div>
    </div>
  </section>

  <section class="section">
    <div class="wrap">
      <div class="section-head reveal">
        <span class="kicker">When something goes wrong</span>
        <h2>The four fixes that solve most launches</h2>
      </div>
      <div class="grid grid-2 reveal">
        <article class="card"><h3>1. Change the renderer</h3><p>Instance settings → renderer. Emesis by default; Zink or OpenLTW on newer phones; GL4ES for old versions. <a href="renderers.html">The full lineup →</a></p></article>
        <article class="card"><h3>2. Lower the resolution</h3><p>It fixes more launches than any renderer change, and it is reversible.</p></article>
        <article class="card"><h3>3. Give it more memory</h3><p>A pack that closes itself mid-load is usually short of heap, not broken.</p></article>
        <article class="card"><h3>4. Read the log</h3><p>In-game menu → Log output, or the log in the launcher. The last line before the crash names the mod almost every time.</p></article>
      </div>
    </div>
  </section>

  <section class="section">
    <div class="wrap">
      <div class="download reveal">
        <h2 style="margin-top:0">Ready?</h2>
        <p class="muted">Android 6.0+ · signed with the project key · updates in place.</p>
        <div class="btn-row">
          <a class="btn btn-primary" href="{DOWNLOAD_URL}" download data-primary-download><span class="nowrap">Download for Android</span> <span class="btn-size">89 MB</span></a>
          <a class="btn btn-ghost" href="faq.html">Common questions</a>
        </div>
      </div>
    </div>
  </section>
</main>
""" + FOOTER


def build_faq():
    faqs = [
        ("Is Aerix Launcher free?",
         "<p>Yes. There is no paid tier, no subscription and no ad. The launcher has no account of its own — you sign in with the Minecraft account you already have, or play offline.</p>"),
        ("Do I need to own Minecraft?",
         "<p>To play online on a Microsoft account, yes — the launcher signs in to your existing copy, it does not provide the game. Offline accounts work for testing packs and playing alone, exactly as they do in the desktop launcher.</p>"),
        ("Which file do I download?",
         "<p>For nearly every phone made since 2016, <b>arm64-v8a</b>. For an older 32-bit phone, <b>armeabi-v7a</b>. If you are not sure, the <b>universal</b> build works on everything and is bigger because it carries every architecture.</p>"),
        ("Will I lose my instances when I update?",
         "<p>No, as long as you install the new APK over the old one. Uninstalling first deletes the launcher's storage, which includes your instances, worlds, accounts and wallpapers. The in-app updater updates in place.</p>"),
        ("The game will not start. What do I do?",
         "<p>Change the renderer for that instance first — the default, Emesis, works almost everywhere, but a phone with a strong Vulkan driver may prefer Zink, and old versions often want GL4ES. If that does not help, lower the resolution and give the instance more memory. The <a href='renderers.html'>renderer page</a> has a short decision tree.</p>"),
        ("Does it support mods and shaders?",
         "<p>Yes. Choose Fabric, Forge, NeoForge or Quilt when you create the instance, then install mods from the built-in browser or drop them into the instance folder. Shaders need a capable GPU and the right renderer — that is where MG-ES or Zink earn their place.</p>"),
        ("Which Android versions and devices are supported?",
         "<p>Android 6.0 and newer, on both 64-bit and 32-bit devices. There is no device allowlist: the launcher runs where the GPU and drivers allow it, which is why there are so many renderers.</p>"),
        ("Is my Microsoft login safe?",
         "<p>The sign-in happens with Microsoft through the usual flow, and the token is stored on your device, in the launcher's own storage. The launcher sends nothing about you anywhere except the login you choose — there is no telemetry and no analytics.</p>"),
        ("Can I import my existing worlds and controls?",
         "<p>Yes. Worlds go into the instance's <code>saves</code> folder, and control layouts are plain JSON that import from the controls editor, including layouts from other launchers.</p>"),
        ("How do I switch accounts while playing?",
         "<p>Open the in-game menu and tap your name under the character. Pick the account and confirm — the game restarts and comes back signed in as them. A running game keeps the session it launched with, so the restart is the switch.</p>"),
        ("Where do the files live?",
         "<p>In the launcher's private storage on your device: instances, worlds, mods, Java runtimes and accounts. Nothing is uploaded; if you want a copy, the launcher's folders are reachable from its own file browser.</p>"),
        ("Why is the download so large?",
         "<p>The full build carries a Java runtime so the game can start without downloading anything else, plus native libraries for the architectures it supports. The <b>no-runtime</b> build is much smaller and fetches Java on first launch.</p>"),
    ]
    items = []
    for i, (q, a) in enumerate(faqs):
        open_attr = " open" if i == 0 else ""
        items.append(f"        <details{open_attr}>\n          <summary>{q}</summary>\n          <div class=\"body\">{a}</div>\n        </details>")
    joined = "\n".join(items)
    return head(
        "FAQ — Aerix Launcher",
        "Answers about Aerix Launcher: which APK to download, updating without losing instances, "
        "mods and shaders, supported Android versions, account safety, and what to do when a game will not start.",
        "faq.html",
    ) + nav("faq.html") + page_head_block(
        "FAQ",
        "Questions people actually ask",
        "The short answers first; the awkward ones with a link to the page that covers them properly.",
    ) + f"""
<main id="main">
  <section class="section">
    <div class="wrap">
      <div class="faq reveal">
{joined}
      </div>

      <div class="grid grid-2 mt-32">
        <article class="card">
          <h3>Still stuck?</h3>
          <p>Open an issue with your device, the instance's version and loader, the renderer, and the
             last thirty lines of the log. That is enough to answer almost anything.</p>
          <div class="tag-row"><a class="tag" href="https://github.com/entitybrian69-bit/Aerix-launcher/issues" target="_blank" rel="noopener">Open an issue</a></div>
        </article>
        <article class="card">
          <h3>Ask in Discord</h3>
          <p>Faster for "why does this look wrong" questions, where a screenshot answers it in one message.</p>
          <div class="tag-row"><a class="tag" href="{DISCORD_URL}" target="_blank" rel="noopener">Join the server</a></div>
        </article>
      </div>
    </div>
  </section>
</main>
""" + FOOTER


def build_404():
    return head(
        "Page not found — Aerix Launcher",
        "That page does not exist. Head back to the Aerix Launcher home page.",
        "404.html",
    ) + nav("") + """
<main id="main">
  <section class="section">
    <div class="wrap center" style="padding:60px 0">
      <span class="kicker">404</span>
      <h1>That page is not here</h1>
      <p class="lede" style="margin-inline:auto">It may have moved — the site was rebuilt. Everything is one tap away:</p>
      <div class="btn-row" style="justify-content:center">
        <a class="btn btn-primary" href="index.html">Home</a>
        <a class="btn btn-ghost" href="install.html">Install guide</a>
        <a class="btn btn-ghost" href="faq.html">FAQ</a>
      </div>
    </div>
  </section>
</main>
""" + FOOTER


def main():
    pages = {
        "index.html": build_index(),
        "features.html": build_features(),
        "renderers.html": build_renderers(),
        "screenshots.html": build_screenshots(),
        "install.html": build_install(),
        "faq.html": build_faq(),
        "404.html": build_404(),
    }
    for name, html in pages.items():
        (DOCS / name).write_text(html, encoding="utf-8")
        print(f"wrote docs/{name}  ({len(html) // 1024} KB)")


if __name__ == "__main__":
    main()
