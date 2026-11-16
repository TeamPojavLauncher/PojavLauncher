# Our modpacks — the catalogue behind the "Our modpacks" tab

The launcher reads one small JSON file over the network; the pack archives stay on GitHub Releases,
so **a modpack never increases the APK size**.

## Where the files live

The tab reads:

```
https://github.com/entitybrian69-bit/Aerix-launcher/releases/download/modpacks/modpacks.json
```

That is a **dedicated tag named `modpacks`**, deliberately not `releases/latest`. "Latest" on this
repository is the launcher's own APK release: the website's primary download buttons and the in-app
updater both point at it, and `pages.yml` asserts that they do. Publishing a modpack release as
latest would break both. A fixed tag never competes for that slot.

Pack files named in `modpacks.json` are resolved against the same release, so `"file": "pack.zip"`
means "download it from beside `modpacks.json`".

## Adding or updating a pack

1. Build the archive (see *Archive format* below).
2. Upload it to the `modpacks` release:

   ```bash
   gh release upload modpacks aetherium-optimized.zip
   ```

3. Add or edit its entry in `modpacks.json`, then:

   ```bash
   gh release upload modpacks modpacks.json --clobber
   ```

Every launcher picks the change up on the next visit to the tab. **No app release, no CI rebuild.**

Create the release once, if it does not exist yet:

```bash
gh release create modpacks --title "Modpacks" --notes "The Team Aerix modpack catalogue."
```

## Entry fields

| Field | Required | Notes |
|---|---|---|
| `id` | | Stable identity; defaults to `file`. Used for the download cache name. |
| `name` | | Card title, also the instance name. Defaults to the file name. |
| `summary` | | One or two lines shown on the card. |
| `file` | **yes** | Archive name inside the release. Entries without it are skipped. |
| `mcVersion` | | Shown on the card. The installer reads the real version from the archive. |
| `loader` | | Shown on the card, e.g. `fabric`. |
| `icon` | | Optional image URL. |
| `size` | | Optional byte count; shown on the card as "12.3 MB". |

A bare JSON array is also accepted, so the file can lose the `packs` wrapper without breaking older
launchers. Unknown fields are ignored.

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
