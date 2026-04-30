package net.opendasharchive.openarchive.features.media.camera

import java.io.Serializable

/**
 * Configuration class for camera functionality with optimization options.
 *
 * This class allows fine-grained control over camera behavior, performance, and
 * resource usage. All settings have sensible defaults for typical use cases.
 */
@kotlinx.serialization.Serializable
data class CameraConfig(
    // ===== Capture Modes =====
    /** Enable photo capture functionality */
    val allowVideoCapture: Boolean = true,
    /** Enable video recording functionality */
    val allowPhotoCapture: Boolean = true,
    /** Allow capturing multiple photos/videos in one session */
    val allowMultipleCapture: Boolean = false,
    /** Enable preview functionality (should generally stay true) */
    val enablePreview: Boolean = true,
    /** Initial capture mode when camera opens */
    val initialMode: CameraCaptureMode = CameraCaptureMode.PHOTO,
    // ===== UI Controls =====
    /** Show flash toggle button (only appears if camera has flash hardware) */
    val showFlashToggle: Boolean = true,
    /** Show grid overlay toggle button for composition assistance */
    val showGridToggle: Boolean = true,
    /** Show button to switch between front and back cameras */
    val showCameraSwitch: Boolean = true,
    // When true, uses IMG_123.jpg instead of 20250119_143045.IMG_123.jpg
    val useCleanFilenames: Boolean = false,
    // ===== Power Management =====
    /**
     * Override screen brightness to maximum when camera is active.
     *
     * When enabled, the screen brightness will be set to maximum for better
     * camera preview visibility and to prevent automatic brightness reduction.
     * When disabled, system brightness is used.
     *
     * Impact: MEDIUM - Affects battery usage and visibility
     * Recommendation: Keep enabled to prevent screen dimming during camera use
     * Default: true (prevents automatic brightness reduction)
     */
    val overrideScreenBrightness: Boolean = true,
    /**
     * Enable automatic camera pause after inactivity.
     *
     * When enabled, the camera will automatically unbind after [idleTimeoutSeconds]
     * of no user interaction. This saves battery and reduces thermal load.
     *
     * Impact: HIGH - Can save 15-25% battery during extended sessions
     * Recommendation: Keep enabled for better battery life
     */
    val enableIdleTimeout: Boolean = true,
    /**
     * Duration in seconds before camera automatically pauses (when [enableIdleTimeout] is true).
     *
     * Lower values save more power but may interrupt user workflow.
     * Higher values are less disruptive but save less power.
     *
     * Impact: MEDIUM - Affects user experience vs battery tradeoff
     * Recommendation: 30-120 seconds depending on use case
     * Default: 60 seconds
     */
    val idleTimeoutSeconds: Int = 60,
    // ===== Preview Optimization =====
    /**
     * Camera preview resolution.
     *
     * Lower resolution reduces memory usage, CPU load, and thermal generation
     * with minimal impact on perceived quality on most screens.
     *
     * Impact: HIGH - Can reduce memory by ~50% (HD vs FHD)
     * Recommendation:
     * - HD (1280x720): Best balance for most devices
     * - FHD (1920x1080): Only if higher preview quality needed
     * - MAX: Avoid unless absolutely necessary
     * Default: HD
     */
    val previewResolution: PreviewResolution = PreviewResolution.HD,
    /**
     * PreviewView rendering implementation mode.
     *
     * PERFORMANCE uses SurfaceView with hardware acceleration for better battery
     * and smoother rendering. COMPATIBLE uses TextureView for wider compatibility
     * but higher resource usage.
     *
     * Impact: HIGH - Performance mode significantly reduces CPU usage
     * Recommendation:
     * - PERFORMANCE: Use for better battery and smoothness (default)
     * - COMPATIBLE: Only if rendering issues occur on specific devices
     * Default: PERFORMANCE
     */
    val implementationMode: ImplementationMode = ImplementationMode.PERFORMANCE,
    // ===== Video Recording Settings =====
    /**
     * Video recording quality.
     *
     * Higher quality produces larger files and increases encoding load on CPU,
     * which impacts battery life and thermal performance.
     *
     * Impact: HIGH - Affects file size, battery, and thermal load
     * Recommendation:
     * - HD (720p): Good balance for general use
     * - FHD (1080p): For archival or professional use
     * - SD (480p): For low-end devices or storage constraints
     * - UHD (4K): Only on high-end devices with ample storage
     * Default: HD
     */
    val videoQuality: VideoQuality = VideoQuality.HD,
    /**
     * Enable audio recording with video (requires RECORD_AUDIO permission).
     *
     * Impact: MINIMAL
     * Default: true
     */
    val enableAudio: Boolean = true,
    // ===== Video Playback Optimization =====
    /**
     * Defer video player initialization until preview is actually shown.
     *
     * When enabled, ExoPlayer is only created when the user views the preview,
     * reducing memory usage when multiple captures are queued.
     *
     * Impact: MEDIUM - Reduces memory footprint
     * Recommendation: Keep enabled
     * Default: true
     */
    val enableLazyVideoLoading: Boolean = true,
    /**
     * Target buffer duration in milliseconds required to start/resume video playback.
     *
     * This is the minimum amount of buffered data required before playback can begin
     * or resume after rebuffering.
     *
     * Impact: MEDIUM - Lower values start faster, higher values prevent stuttering
     * Recommendation: 1000-2000ms depending on device capability
     * Default: 1500ms
     */
    val videoBufferMs: Int = 1500,
    /**
     * Minimum total buffer duration for video playback (in milliseconds).
     *
     * This is the minimum total amount of media that should be buffered at any time.
     * IMPORTANT: Must be >= videoBufferMs (ExoPlayer constraint)
     *
     * Impact: MEDIUM - Affects memory usage and playback smoothness
     * Recommendation: 2000-4000ms
     * Default: 2500ms
     */
    val minVideoBufferMs: Int = 2500,
    /**
     * Maximum buffer duration for video playback (in milliseconds).
     *
     * Higher values provide more buffer headroom but use more memory.
     *
     * Impact: MEDIUM - Affects memory usage
     * Recommendation: 5000-10000ms
     * Default: 5000ms
     */
    val maxVideoBufferMs: Int = 5000,
) : Serializable

/**
 * Camera capture modes.
 */
enum class CameraCaptureMode : Serializable {
    PHOTO,
    VIDEO,
}

/**
 * Preview resolution options.
 *
 * Lower resolutions reduce resource usage with minimal perceived quality loss.
 */
enum class PreviewResolution : Serializable {
    /** 1280x720 - Recommended for most devices (good balance) */
    HD,

    /** 1920x1080 - Higher quality, more resource intensive */
    FHD,

    /** Maximum supported resolution - Use sparingly */
    MAX,
}

/**
 * PreviewView implementation modes.
 *
 * Determines how the camera preview is rendered.
 */
enum class ImplementationMode : Serializable {
    /**
     * Uses SurfaceView with hardware acceleration.
     * Recommended for better performance and battery life.
     */
    PERFORMANCE,

    /**
     * Uses TextureView for rendering.
     * Use only if PERFORMANCE mode causes rendering issues.
     */
    COMPATIBLE,
}

/**
 * Video recording quality options.
 *
 * Higher quality produces larger files and increases resource usage.
 */
enum class VideoQuality : Serializable {
    /** 480p - For low-end devices or storage constraints */
    SD,

    /** 720p - Recommended balance of quality and size */
    HD,

    /** 1080p - Higher quality for archival purposes */
    FHD,

    /** 4K - Only for high-end devices (if supported) */
    UHD,
}
