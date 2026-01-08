use jni::objects::{JClass, JObject, JString};
use jni::sys::{jboolean, jstring};
use jni::JNIEnv;
use log::LevelFilter;
use serde::{Deserialize, Serialize};
use std::fs;

/// Initialize the C2PA FFI library and logging
#[no_mangle]
pub extern "C" fn Java_net_opendasharchive_openarchive_util_C2paFfi_nativeInit(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    // Initialize Android logger
    android_logger::init_once(
        android_logger::Config::default()
            .with_max_level(LevelFilter::Debug)
            .with_tag("C2PA-FFI"),
    );

    log::info!("C2PA FFI initialized");
    1 // true
}

/// C2PA Manifest structure for JSON serialization
#[derive(Serialize, Deserialize, Debug)]
struct C2paManifest {
    claim_generator: String,
    assertions: Vec<Assertion>,
    signature: SignatureInfo,
}

#[derive(Serialize, Deserialize, Debug)]
struct Assertion {
    label: String,
    data: serde_json::Value,
}

#[derive(Serialize, Deserialize, Debug)]
struct SignatureInfo {
    algorithm: String,
    value: String,
}

/// Generate a C2PA manifest for a media file
///
/// # Arguments
/// * `file_path` - Path to the media file
/// * `metadata_json` - JSON string containing metadata (title, description, etc.)
///
/// # Returns
/// JSON string containing the C2PA manifest, or null on error
#[no_mangle]
pub extern "C" fn Java_net_opendasharchive_openarchive_util_C2paFfi_nativeGenerateManifest(
    mut env: JNIEnv,
    _class: JClass,
    file_path: JString,
    metadata_json: JString,
) -> jstring {
    // Convert JString to Rust String
    let file_path_str: String = match env.get_string(&file_path) {
        Ok(s) => s.into(),
        Err(e) => {
            log::error!("Failed to get file path string: {:?}", e);
            return JObject::null().into_raw();
        }
    };

    let metadata_str: String = match env.get_string(&metadata_json) {
        Ok(s) => s.into(),
        Err(e) => {
            log::error!("Failed to get metadata JSON string: {:?}", e);
            return JObject::null().into_raw();
        }
    };

    log::info!("Generating C2PA manifest for: {}", file_path_str);

    // Parse metadata JSON
    let metadata: serde_json::Value = match serde_json::from_str(&metadata_str) {
        Ok(m) => m,
        Err(e) => {
            log::error!("Failed to parse metadata JSON: {:?}", e);
            return JObject::null().into_raw();
        }
    };

    // TODO: Implement actual C2PA manifest generation using c2pa crate
    // For now, create a stub manifest structure
    let manifest = create_stub_manifest(&file_path_str, metadata);

    // Serialize manifest to JSON
    let manifest_json = match serde_json::to_string_pretty(&manifest) {
        Ok(json) => json,
        Err(e) => {
            log::error!("Failed to serialize manifest: {:?}", e);
            return JObject::null().into_raw();
        }
    };

    // Convert Rust String to JString
    match env.new_string(manifest_json) {
        Ok(s) => s.into_raw(),
        Err(e) => {
            log::error!("Failed to create JString: {:?}", e);
            JObject::null().into_raw()
        }
    }
}

/// Create a stub C2PA manifest
/// TODO: Replace with actual c2pa library implementation
fn create_stub_manifest(file_path: &str, metadata: serde_json::Value) -> C2paManifest {
    // Calculate file hash for signature
    let file_hash = calculate_file_hash(file_path).unwrap_or_else(|_| "unknown".to_string());

    C2paManifest {
        claim_generator: "OpenArchive Save/Rust FFI".to_string(),
        assertions: vec![
            Assertion {
                label: "stds.schema-org.CreativeWork".to_string(),
                data: metadata,
            },
            Assertion {
                label: "c2pa.hash.data".to_string(),
                data: serde_json::json!({
                    "alg": "sha256",
                    "hash": file_hash,
                    "name": "jumbf manifest"
                }),
            },
        ],
        signature: SignatureInfo {
            algorithm: "es256".to_string(),
            value: format!("stub_signature_{}", &file_hash[..16]),
        },
    }
}

/// Calculate SHA-256 hash of a file
fn calculate_file_hash(file_path: &str) -> Result<String, std::io::Error> {
    use sha2::{Digest, Sha256};

    let contents = fs::read(file_path)?;
    let hash = Sha256::digest(&contents);
    Ok(format!("{:x}", hash))
}

/// Verify a C2PA manifest
///
/// # Arguments
/// * `manifest_json` - JSON string containing the C2PA manifest
///
/// # Returns
/// true if manifest is valid, false otherwise
#[no_mangle]
pub extern "C" fn Java_net_opendasharchive_openarchive_util_C2paFfi_nativeVerifyManifest(
    mut env: JNIEnv,
    _class: JClass,
    manifest_json: JString,
) -> jboolean {
    let manifest_str: String = match env.get_string(&manifest_json) {
        Ok(s) => s.into(),
        Err(e) => {
            log::error!("Failed to get manifest JSON string: {:?}", e);
            return 0; // false
        }
    };

    // Parse manifest JSON
    match serde_json::from_str::<C2paManifest>(&manifest_str) {
        Ok(_manifest) => {
            log::info!("Manifest verification: valid structure");
            // TODO: Implement actual signature verification using c2pa crate
            1 // true
        }
        Err(e) => {
            log::error!("Manifest verification failed: {:?}", e);
            0 // false
        }
    }
}

// Add sha2 dependency for hashing
// This will be added to Cargo.toml
