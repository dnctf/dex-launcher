use jni::objects::{JClass, JString};
use jni::sys::jstring;
use jni::JNIEnv;
use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct AppItem {
    pub label: String,
    pub package_name: String,
}

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct SearchResult {
    pub package_name: String,
    pub score: i32,
}

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct DesktopItemConfig {
    pub package_name: String,
    pub label: String,
    pub custom_label: Option<String>,
    pub grid_x: i32,
    pub grid_y: i32,
}

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct DexSettings {
    pub icon_size_dp: i32,
    pub icon_shape: String, // "default", "circle", "squircle", "rounded_rect", "dex_teardrop"
    pub grid_columns: i32,
    pub show_icon_labels: bool,
    pub taskbar_height_dp: i32,
    pub start_menu_columns: i32,
    pub custom_icon_pack: Option<String>,
}

impl Default for DexSettings {
    fn default() -> Self {
        Self {
            icon_size_dp: 56,
            icon_shape: "squircle".to_string(),
            grid_columns: 7,
            show_icon_labels: true,
            taskbar_height_dp: 48,
            start_menu_columns: 5,
            custom_icon_pack: None,
        }
    }
}

/// Calculate match score between query and app name/package.
/// Higher score = better match.
fn calculate_match_score(query: &str, app: &AppItem) -> Option<i32> {
    let q = query.trim().to_lowercase();
    if q.is_empty() {
        return Some(0);
    }

    let label_lower = app.label.to_lowercase();
    let pkg_lower = app.package_name.to_lowercase();

    // 1. Exact match on label
    if label_lower == q {
        return Some(1000);
    }

    // 2. Prefix match on label
    if label_lower.starts_with(&q) {
        return Some(500 + (100 - (label_lower.len() as i32 - q.len() as i32)).max(0));
    }

    // 3. Word boundary prefix match (e.g. "ws" matches "WhatsApp")
    let words: Vec<&str> = label_lower.split_whitespace().collect();
    for word in words {
        if word.starts_with(&q) {
            return Some(350);
        }
    }

    // 4. Substring match on label
    if let Some(pos) = label_lower.find(&q) {
        return Some(200 - (pos as i32 * 10).min(100));
    }

    // 5. Package name match
    if pkg_lower.contains(&q) {
        return Some(100);
    }

    // 6. Subsequence / fuzzy acronym match (e.g. "tb" for "TikTok Business")
    let mut q_chars = q.chars();
    let mut current_target = q_chars.next();
    let mut matches = 0;

    for c in label_lower.chars() {
        if let Some(target) = current_target {
            if c == target {
                matches += 1;
                current_target = q_chars.next();
            }
        } else {
            break;
        }
    }

    if current_target.is_none() && matches >= 2 {
        Some(50 + matches * 5)
    } else {
        None
    }
}

#[no_mangle]
pub extern "system" fn Java_com_dexlauncher_app_DexNativeBridge_getCoreVersion(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let version = "DexCore v0.1.0-pre (Rust Engine for Android/HarmonyOS)";
    let output = env.new_string(version).unwrap();
    output.into_raw()
}

#[no_mangle]
pub extern "system" fn Java_com_dexlauncher_app_DexNativeBridge_searchAppsNative(
    mut env: JNIEnv,
    _class: JClass,
    query_jstr: JString,
    apps_json_jstr: JString,
) -> jstring {
    let query: String = env
        .get_string(&query_jstr)
        .map(|s| s.into())
        .unwrap_or_default();

    let apps_json: String = env
        .get_string(&apps_json_jstr)
        .map(|s| s.into())
        .unwrap_or_default();

    let apps: Vec<AppItem> = serde_json::from_str(&apps_json).unwrap_or_default();

    let mut results: Vec<SearchResult> = apps
        .into_iter()
        .filter_map(|app| {
            calculate_match_score(&query, &app).map(|score| SearchResult {
                package_name: app.package_name,
                score,
            })
        })
        .collect();

    // Sort descending by score
    results.sort_by(|a, b| b.score.cmp(&a.score));

    let result_json = serde_json::to_string(&results).unwrap_or_else(|_| "[]".to_string());
    let output = env.new_string(result_json).unwrap();
    output.into_raw()
}

#[no_mangle]
pub extern "system" fn Java_com_dexlauncher_app_DexNativeBridge_getDefaultSettingsNative(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let default_settings = DexSettings::default();
    let json = serde_json::to_string(&default_settings).unwrap_or_else(|_| "{}".to_string());
    let output = env.new_string(json).unwrap();
    output.into_raw()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_search_score() {
        let app = AppItem {
            label: "Termux".to_string(),
            package_name: "com.termux".to_string(),
        };

        assert!(calculate_match_score("term", &app).is_some());
        assert_eq!(calculate_match_score("termux", &app), Some(1000));
        assert!(calculate_match_score("xyz", &app).is_none());
    }
}
