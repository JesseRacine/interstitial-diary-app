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
    let mut window = tauri::WebviewWindowBuilder::from_config(app, &config)?;
    if let Some(dir) = portable_data_dir() {
        window = window.data_directory(dir);
    }
    window.build()?;
    Ok(())
}

// `journal-data` beside the program. None when it cannot be created (a read-only place such as Program Files): the
// app then still opens, with its data in the usual AppData folder, rather than not starting at all.
#[cfg(windows)]
fn portable_data_dir() -> Option<std::path::PathBuf> {
    let dir = std::env::current_exe().ok()?.parent()?.join("journal-data");
    std::fs::create_dir_all(&dir).ok()?;
    Some(dir)
}
