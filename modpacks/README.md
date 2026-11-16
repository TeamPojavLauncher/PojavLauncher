# Our modpacks — the catalogue behind the "Our modpacks" tab

The launcher reads one small JSON file over the network; the pack archives stay on GitHub Releases,
so **a modpack never increases the APK size**.

## Where the files live

`modpacks.json` is read from the first of two mirrors that answers:

1. `https://github.com/.../releases/download/modpacks/modpacks.json` — a **dedicated tag named
   `modpacks`**, deliberately not `releases/latest`. "Latest" on this repository is the launcher's
   own APK release: the website's primary download buttons and the in-app updater both point at it,
   and `pages.yml` asserts that they do. Publishing a modpack release as latest would break both.
2. `https://raw.githubusercontent.com/.../v3_openjdk/modpacks/modpacks.json` — the copy committed in
   this folder.

Pack files *and pack artwork* resolve against the same two roots, in the same order, so
`"file": "packs/x.zip"` means "the release asset `x.zip` in the `packs/` folder, else the committed
one", and `"icon": "icons/x.png"` means the same for the logo. The mirrors exist because
release assets can only be uploaded by a token with the upload permission, while a committed file
needs nothing but a push — the tab works either way.

**The archives still never enter the APK.** They are committed under `modpacks/packs/` (or attached
to the release) and downloaded on demand, which is the only thing that matters for app size.

## Adding or updating a pack

1. Build the archive (see *Archive format* below).
2. Put it in `modpacks/packs/`.
3. Add or edit its entry in `modpacks/modpacks.json`.
4. Commit and push.

Every launcher picks the change up on the next visit to the tab. **No app release, no CI rebuild.**

Optionally also attach both to the release, which serves them from a CDN instead of the raw host:

```bash
gh release upload modpacks modpacks/modpacks/packs/x.zip
gh release upload modpacks modpacks/modpacks.json --clobber
```

Create the release once, if it does not exist yet (it does, as of the first pack):

```bash
gh release create modpacks --title "Modpacks" --notes "The Team Aerix modpack catalogue."
```

## How the tab is laid out

The tab is a **grid of folders**, one per pack, plus a reserved tile for the packs that are still
coming. Tapping a folder opens that pack's page, which is where the decision to install happens:

1. the pack's artwork, name and summary;
2. **every Minecraft version the pack offers**, as a list you pick one from;
3. one "Download & play" button that installs the build you picked.

A pack with a single build shows that build as the only row rather than pretending there is a
choice - the page still names it, so the button is never a guess.

## Entry fields

| Field | Required | Notes |
|---|---|---|
| `id` | | Stable identity; defaults to `file`. Used for the download cache name. |
| `name` | | Folder title, also the instance name. Defaults to the file name. |
| `summary` | | One or two lines shown on the pack page. |
| `file` | **yes** | Archive name, relative to the catalogue (`packs/x.zip`). Entries without it are skipped. Also the default build. |
| `mcVersion` | | Shown on the folder. The installer reads the real version from the archive. |
| `loader` | | Shown beside the version, e.g. `fabric`. |
| `icon` | | Optional artwork. A full URL, or a path relative to this folder (`icons/x.png`), which is served by the same two mirrors as the archives. |
| `size` | | Optional byte count; shown as "12.3 MB". |
| `versions` | | Optional list of builds. Each entry takes `file` (required), `mcVersion`, `loader`, `size` and `note`. |

A pack with no `versions` array offers exactly one build, described by its top-level `file`,
`mcVersion` and `size` - so a one-build pack stays a one-line entry. A pack with `versions` offers
one row per entry, in the order they are written, and the archive name keeps each build's download
cache separate.

A bare JSON array is also accepted, so the file can lose the `packs` wrapper without breaking older
launchers. Unknown fields are ignored.

### Shipping a second Minecraft version of a pack

```json
"versions": [
  { "mcVersion": "1.20.1", "file": "packs/aetherium-optimized.zip", "size": 4078430,
    "note": "The build the team plays on. Recommended." },
  { "mcVersion": "1.21.1", "file": "packs/aetherium-optimized-1.21.1.zip", "size": 4210000 }
]
```

Build the archive for the new version, commit it under `packs/`, add the entry, push. The folder
starts reading "2 versions" and the pack page offers both - **no app release, no CI rebuild.**

The installer refuses a mismatch: if the chosen row says 1.20.1 and the archive's own
`mojo_instance.json` says otherwise, it stops with a readable message instead of installing the
wrong game version.

## The pack's mark in the app

A pack installed from the tab must never appear in the library wearing the **Fabric** logo. Two
things guarantee that, in this order:

1. **The mark bundled in the APK.** `res/drawable/aerix_modpack_<id>.xml` (dashes become
   underscores) is drawn onto the instance the moment it is created — offline, and before any
   download could finish. `AerixModpackIcons.drawableFor()` maps a catalogue id to it.
2. **The artwork the catalogue publishes.** `"icon": "icons/x.png"` is only fetched when this build
   ships no bundled mark for that pack.

Instances created *before* the launcher recorded their pack are repaired automatically: an instance
whose `modpackSource` is `aerix` is matched to a catalogue entry by name, remembered under
`aerixPackId`, and given its bundled mark on the next library scan.

Adding a pack therefore means adding **two** things that share an id: its entry in `modpacks.json`,
and `res/drawable/aerix_modpack_<id>.xml`. `tests/modpacks_smoke.py` asserts the pairing, so a pack
cannot ship without its mark.

## Archive format

Two layouts install in one press:

* **A Team Aerix instance export** — how these packs are built. A folder containing
  `mojo_instance.json`, `mods/`, `config/`, `shaderpacks/` and `icon.webp`. The installer reads the
  game version and Fabric loader version out of `mojo_instance.json`
  (`"versionId": "fabric-loader-0.14.25-1.20.1"`), copies the whole folder into a new instance, and
  re-encodes `icon.webp` to the PNG the launcher's instance icons actually read.
* **`.mrpack` (Modrinth) or a CurseForge zip** — handed to the existing modpack installer, which
  already understood these.

Anything else is rejected with a readable message instead of failing silently. Note that
`CommonApi.checkModpack()` only recognises Modrinth and CurseForge; the instance-export layout is
detected by `AerixModpackInstaller` before that check runs, which is why these packs install at all.

## What an install does

1. Downloads the archive to the app cache, with a progress bar on the card.
2. Reads `mojo_instance.json` for the pack name, Minecraft version and loader version.
3. Downloads that exact Fabric loader (falling back to the newest stable build for the same game
   version only if the named one is gone).
4. Creates an instance with `sharedData = false` — a pack owns its game directory, so one pack's
   mods can never leak into another instance.
5. Copies the pack's files in, writes the pack artwork as the instance icon, selects the instance
   and refreshes the library.

Install failures are reported on the card, not as a toast that disappears.
