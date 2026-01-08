package net.opendasharchive.openarchive.util

import net.opendasharchive.openarchive.core.logger.AppLogger

/**
 * JNI wrapper for C2PA Rust FFI library
 *
 * This class provides a Kotlin interface to the native Rust implementation of C2PA.
 * The Rust library is compiled from source for FOSS builds, ensuring F-Droid compliance.
 */
object C2paFfi {

    private var initialized = false
    private var libraryLoaded = false

    init {
        try {
            // Load the native library compiled from Rust
            System.loadLibrary("c2pa_ffi")
            libraryLoaded = true
            AppLogger.d("C2PA FFI native library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            AppLogger.e("Failed to load C2PA FFI native library", e)
            AppLogger.e("Make sure Rust is installed and the library is compiled")
            libraryLoaded = false
        }
    }

    /**
     * Initialize the C2PA FFI library
     * Must be called before using any other functions
     *
     * @return true if initialization succeeded, false otherwise
     */
    fun initialize(): Boolean {
        if (!libraryLoaded) {
            AppLogger.w("Cannot initialize C2PA FFI - native library not loaded")
            return false
        }

        if (initialized) {
            AppLogger.d("C2PA FFI already initialized")
            return true
        }

        return try {
            val result = nativeInit()
            initialized = result
            if (result) {
                AppLogger.i("C2PA FFI initialized successfully")
            } else {
                AppLogger.w("C2PA FFI initialization returned false")
            }
            result
        } catch (e: Exception) {
            AppLogger.e("Failed to initialize C2PA FFI", e)
            false
        }
    }

    /**
     * Generate a C2PA manifest for a media file
     *
     * @param filePath Absolute path to the media file
     * @param metadataJson JSON string containing metadata (title, description, author, etc.)
     * @return JSON string containing the C2PA manifest, or null if generation failed
     */
    fun generateManifest(filePath: String, metadataJson: String): String? {
        if (!initialized) {
            AppLogger.w("C2PA FFI not initialized, attempting to initialize now")
            if (!initialize()) {
                AppLogger.e("Cannot generate manifest - FFI initialization failed")
                return null
            }
        }

        return try {
            val result = nativeGenerateManifest(filePath, metadataJson)
            if (result != null) {
                AppLogger.d("C2PA manifest generated for: $filePath")
            } else {
                AppLogger.w("C2PA manifest generation returned null for: $filePath")
            }
            result
        } catch (e: Exception) {
            AppLogger.e("Failed to generate C2PA manifest", e)
            null
        }
    }

    /**
     * Verify a C2PA manifest
     *
     * @param manifestJson JSON string containing the C2PA manifest
     * @return true if the manifest is valid, false otherwise
     */
    fun verifyManifest(manifestJson: String): Boolean {
        if (!initialized) {
            AppLogger.w("C2PA FFI not initialized, attempting to initialize now")
            if (!initialize()) {
                AppLogger.e("Cannot verify manifest - FFI initialization failed")
                return false
            }
        }

        return try {
            val result = nativeVerifyManifest(manifestJson)
            AppLogger.d("C2PA manifest verification result: $result")
            result
        } catch (e: Exception) {
            AppLogger.e("Failed to verify C2PA manifest", e)
            false
        }
    }

    /**
     * Check if the native library is loaded and ready to use
     *
     * @return true if library is loaded, false otherwise
     */
    fun isAvailable(): Boolean {
        return libraryLoaded && initialized
    }

    // Native method declarations
    // These are implemented in rust-c2pa-ffi/src/lib.rs

    /**
     * Initialize the C2PA FFI library (native implementation)
     */
    private external fun nativeInit(): Boolean

    /**
     * Generate C2PA manifest (native implementation)
     */
    private external fun nativeGenerateManifest(
        filePath: String,
        metadataJson: String
    ): String?

    /**
     * Verify C2PA manifest (native implementation)
     */
    private external fun nativeVerifyManifest(manifestJson: String): Boolean
}
