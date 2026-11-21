// The whole app is the web app shown in a window.
//
// Windows is portable: the journal's data lives in a `journal-data` folder beside the program, not in the user's
// AppData, so the folder can be moved or copied as one piece. For that the window is opened here instead of by Tauri
// (tauri.windows.conf.json says `"create": false`), because the data folder has to be chosen before the window exists.
// Android is untouched: Tauri opens the window itself there.
#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .setup(|app| {
            #[cfg(windows)]
            open_portable_window(app.handle())?;
            let _ = &app;
            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("error while running Interstitial Diary");
}

#[cfg(windows)]
fn open_portable_window(app: &tauri::AppHandle) -> tauri::Result<()> {
    let config = app
        .config()
        .app
        .windows
        .first()
        .expect("tauri.windows.conf.json defines the main window")
        .clone();
    // No browser-style autofill pop-ups (saved names, addresses, contact details) on the entry and search boxes: this is an
    // app, not a web page. It does not touch password or card autofill, which the app has no fields for.
    let mut window = tauri::WebviewWindowBuilder::from_config(app, &config)?.general_autofill_enabled(false);
    if let Some(dir) = portable_data_dir() {
        remove_saved_site(&dir);
        window = window.data_directory(dir);
    }
    window.build()?;
    Ok(())
}

// Earlier builds of the web app saved a copy of the site in the web view (a "service worker" and its cache) so that it
// could work offline. Here the whole site is already inside the program, so that copy only hides new versions: the old
// one is served first, and it can never be replaced, because the web view's update check for it goes to the real
// network instead of through this program. The web app no longer creates one (src/lib/nativeApp.ts); this deletes any
// left behind by an older build, before the window exists (so nothing has it open). It is only the saved copy: the
// journal's own database (IndexedDB) and everything else in `journal-data` stay as they are. Errors are ignored: a copy
// that cannot be removed is no worse than before, and a missing folder is the normal case.
#[cfg(windows)]
fn remove_saved_site(data_dir: &std::path::Path) {
    let _ = std::fs::remove_dir_all(data_dir.join("EBWebView").join("Default").join("Service Worker"));
}

// `journal-data` beside the program. None when it cannot be created (a read-only place such as Program Files): the
// app then still opens, with its data in the usual AppData folder, rather than not starting at all.
#[cfg(windows)]
fn portable_data_dir() -> Option<std::path::PathBuf> {
    let dir = std::env::current_exe().ok()?.parent()?.join("journal-data");
    std::fs::create_dir_all(&dir).ok()?;
    Some(dir)
}
