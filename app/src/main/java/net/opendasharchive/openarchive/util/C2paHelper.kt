package net.opendasharchive.openarchive.util

import android.content.Context
import net.opendasharchive.openarchive.BuildConfig
import net.opendasharchive.openarchive.core.logger.AppLogger
import java.io.File

/**
 * C2PA Helper for generating cryptographic content provenance manifests
 *
 * This implementation uses a custom sidecar approach:
 * - Generates C2PA manifests
 * - Extracts manifest JSON before embedding
 * - Saves as separate .c2pa.json files
 * - Original files remain COMPLETELY UNTOUCHED (MD5 preserved)
 */
object C2paHelper {

    private var initialized = false

    /**
     * Initialize C2PA library
     */
    fun init(context: Context) {
        if (initialized) return

        try {
            // Try to initialize FFI (FOSS builds with Rust compiled from source)
            if (C2paFfi.initialize()) {
                initialized = true
                AppLogger.i("C2PA Helper initialized with FFI (Rust native)")
            } else {
                // Fall back to stub for GMS builds or if FFI unavailable
                initialized = true
                AppLogger.d("C2PA Helper initialized (stub implementation - FFI unavailable)")
            }
        } catch (e: Exception) {
            AppLogger.e("Failed to initialize C2PA", e)
            // Still mark as initialized to allow stub functionality
            initialized = true
        }
    }

    /**
     * Generate C2PA manifest for a media file
     * Saves as SIDECAR .c2pa.json file (does NOT modify original)
     *
     * @param context Application context
     * @param mediaFile The original media file
     * @param mediaHash MD5 hash string for the media
     * @param metadata Additional metadata to include in manifest
     * @return The sidecar manifest file, or null if disabled/failed
     */
    fun generateManifest(
        context: Context,
        mediaFile: File,
        mediaHash: String,
        metadata: Map<String, String> = emptyMap()
    ): File? {
        AppLogger.d("[C2PA] generateManifest called - mediaFile: ${mediaFile.absolutePath}, mediaHash: $mediaHash")

        if (!Prefs.useC2pa) {
            AppLogger.w("[C2PA] Disabled in preferences")
            return null
        }

        if (!mediaFile.exists()) {
            AppLogger.e("[C2PA] Media file does not exist: ${mediaFile.absolutePath}")
            return null
        }

        AppLogger.d("[C2PA] Media file exists, size: ${mediaFile.length()} bytes")

        try {
            // Try to use FFI implementation (FOSS builds)
            val manifestJson = if (C2paFfi.isAvailable()) {
                AppLogger.d("[C2PA] Using FFI to generate manifest")
                val metadataJson = metadata.toJsonString()
                AppLogger.d("[C2PA] Calling FFI with metadata: $metadataJson")
                val result = C2paFfi.generateManifest(mediaFile.absolutePath, metadataJson)
                if (result == null) {
                    AppLogger.w("[C2PA] FFI returned null, falling back to stub")
                    buildStubManifestJson(mediaFile, metadata)
                } else {
                    AppLogger.d("[C2PA] FFI returned manifest, length: ${result.length}")
                    result
                }
            } else {
                AppLogger.d("[C2PA] FFI unavailable, using stub implementation")
                buildStubManifestJson(mediaFile, metadata)
            }

            // Save as sidecar file
            val sidecarFile = getC2paFile(context, mediaHash)
            AppLogger.d("[C2PA] Sidecar file path: ${sidecarFile.absolutePath}")

            val parentCreated = sidecarFile.parentFile?.mkdirs() ?: false
            AppLogger.d("[C2PA] Parent directory created/exists: $parentCreated, path: ${sidecarFile.parentFile?.absolutePath}")

            sidecarFile.writeText(manifestJson)
            AppLogger.i("[C2PA] Manifest saved successfully: ${sidecarFile.absolutePath}, size: ${sidecarFile.length()} bytes")

            return sidecarFile

        } catch (e: Exception) {
            AppLogger.e("[C2PA] Failed to generate manifest", e)
            return null
        }
    }

    /**
     * Temporary stub implementation - builds a placeholder manifest JSON
     * This will be replaced with actual C2PA library calls
     */
    private fun buildStubManifestJson(
        mediaFile: File,
        metadata: Map<String, String>
    ): String {
        // TODO: Replace with actual C2PA manifest generation using simple-c2pa
        return """
        {
          "claim_generator": "OpenArchive Save/${BuildConfig.VERSION_NAME}",
          "assertions": [
            {
              "label": "stds.schema-org.CreativeWork",
              "data": ${metadata.toJsonString()}
            }
          ],
          "signature": {
            "note": "Placeholder - will be replaced with actual C2PA signature"
          }
        }
        """.trimIndent()
    }

    /**
     * Convert metadata map to JSON string
     */
    private fun Map<String, String>.toJsonString(): String {
        // Properly escape JSON values
        val jsonEntries = this.entries.joinToString(",\n    ") { (key, value) ->
            val escapedValue = value
                .replace("\\", "\\\\")  // Escape backslashes
                .replace("\"", "\\\"")  // Escape quotes
                .replace("\n", "\\n")   // Escape newlines
                .replace("\r", "\\r")   // Escape carriage returns
                .replace("\t", "\\t")   // Escape tabs
            "\"$key\": \"$escapedValue\""
        }
        return "{\n    $jsonEntries\n}"
    }

    /**
     * Get the sidecar C2PA manifest file for a given media hash
     *
     * @param context Application context
     * @param mediaHash MD5 hash string for the media
     * @return File object pointing to where the .c2pa.json sidecar should be stored
     */
    fun getC2paFile(context: Context, mediaHash: String): File {
        val c2paDir = File(context.filesDir, "c2pa_manifests")
        return File(c2paDir, "$mediaHash.c2pa.json")
    }

    /**
     * Remove C2PA manifest files for a given media hash
     *
     * @param context Application context
     * @param mediaHash MD5 hash string for the media
     */
    fun removeC2paFiles(context: Context, mediaHash: String) {
        val c2paFile = getC2paFile(context, mediaHash)
        if (c2paFile.exists()) {
            c2paFile.delete()
            AppLogger.d("Deleted C2PA manifest: ${c2paFile.absolutePath}")
        }
    }
}
