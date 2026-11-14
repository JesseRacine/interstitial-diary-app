// The whole app is the web app shown in a window; nothing native is added yet.
#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .run(tauri::generate_context!())
        .expect("error while running Interstitial Diary");
}
