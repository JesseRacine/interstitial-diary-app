# Interstitial Diary: Windows and Android apps

The portable Windows program and the Android APK of [Interstitial Diary](https://github.com/JesseRacine/interstitial-diary).
This repo holds only a thin [Tauri](https://tauri.app) shell and the build; the journal itself is the web app in the
other repo, built unchanged and shown in a native window. Storage is the web app's own (Dropbox for syncing).

Nothing is built on your own computer: GitHub Actions does it all.

## Getting a build

- **Every push to `main`** (or Actions > Build apps > Run workflow) builds both apps. Open the run and download
  `windows-portable` and `android-apk` under **Artifacts**.
- **A version tag** (`v0.1.0`) also creates a GitHub Release with both files attached.

Which version of the web app goes in: the branch, tag or commit named in [`web.ref`](web.ref) (`main` = the newest).
Put a commit id there to pin a version. Its tests run first; a failing test stops the build.

## Releasing a new version

1. Set the same version in `src-tauri/tauri.conf.json`, `src-tauri/Cargo.toml` and `package.json`.
   Android only installs an update whose version is higher than the installed one.
2. Commit, then tag and push: `git tag v0.2.0 && git push origin main v0.2.0`.

## One-time setup

**Android signing key** (without it each build is signed with a throwaway key: it installs, but the next build will
not install over it). Make a key once with the script (needs `openssl` and a signed-in `gh`); it asks for a password,
writes `~/interstitial-diary-keys/interstitial.p12` and sets the three repository secrets:

```bash
bash scripts/make-android-key.sh
```

Keep that file and the password somewhere safe (lose either and updates stop working), and never commit it. By hand
instead: Settings > Secrets and variables > Actions, add `ANDROID_KEYSTORE_BASE64` (the output of `base64 -w0` on a
PKCS12 or JKS keystore), `ANDROID_KEYSTORE_PASSWORD` and `ANDROID_KEY_ALIAS`.

**Dropbox sign-in.** Inside the apps the web app's address is `https://tauri.localhost/`, so that address has to be
added to the Dropbox app's redirect URIs (Dropbox App Console > the app > OAuth 2 > Redirect URIs).

**Windows.** There is no installer: unzip `windows-portable` anywhere you can write to and run
`Interstitial Diary.exe`. Its data goes in a `journal-data` folder beside the program, nothing in AppData
(`src-tauri/src/lib.rs`; details for the person using it in `src-tauri/windows/Read me.txt`, which ships in the
folder). The program is not code-signed, so Windows SmartScreen warns the first time ("More info" > "Run anyway").

## Good to know

- The apps keep their own data, separate from the browser and from the installed web app (PWA). Connect Dropbox in
  the app to get your journal.
- App id: `io.github.jesseracine.interstitialdiary`. Changing it later makes Android treat it as a different app.
- The Android project (`src-tauri/gen/`) and `Cargo.lock` are generated during the build, not committed.

## Layout

| Path | What |
|---|---|
| `.github/workflows/build.yml` | The build: web app, then Windows and Android, then a Release for tags |
| `web.ref` | Which version of the web app to build |
| `src-tauri/tauri.conf.json` | App name, id, version, window |
| `src-tauri/src/` | The native side: on Windows it opens the window with its data folder beside the program, and first deletes any old saved copy of the site (`journal-data\EBWebView\Default\Service Worker`) |
| `src-tauri/tauri.windows.conf.json` | Windows only: the same window, but opened by `lib.rs` instead of by Tauri |
| `src-tauri/windows/Read me.txt` | The note that ships in the Windows folder |
| `src-tauri/android/MainActivity.kt` | Android: keeps the app clear of the status bar, camera cutout and keyboard; deletes any old saved copy of the site at startup |
| `src-tauri/icons/` | Icons, made with `npx tauri icon src-tauri/app-icon.svg` |

**Why the old saved copy is deleted by the app.** The web app is also a PWA, and a browser saves a copy of a PWA (a
"service worker" and its cache) to work offline. Inside these apps that only hides new versions: the old copy is served
first, and it can never be replaced, because the web view's update check for it goes to the real network (`tauri.localhost`
does not exist there) instead of through the app. So the web app no longer creates one here (`src/lib/nativeApp.ts` in the
web repo) and these two files remove one left by an older build, before the web view starts. Only that folder: the
journal's database stays.
