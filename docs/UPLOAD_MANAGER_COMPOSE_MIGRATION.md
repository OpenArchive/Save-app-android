# Upload Manager Compose Migration

## Overview

The UploadManagerFragment has been successfully migrated to Jetpack Compose while maintaining full backward compatibility with the original XML implementation. The project now includes both implementations, and you can toggle between them using a simple configuration flag.

## Files Created

### 1. **UploadManagerViewModel.kt**
   - Location: `app/src/main/java/net/opendasharchive/openarchive/upload/UploadManagerViewModel.kt`
   - Purpose: Manages the state and business logic for the upload manager
   - Features:
     - State management using StateFlow
     - Actions for refresh, update, remove, delete, retry, and move items
     - Event-driven architecture for navigation and dialogs
     - Support for drag-and-drop reordering with priority updates

### 2. **UploadManagerScreen.kt**
   - Location: `app/src/main/java/net/opendasharchive/openarchive/upload/UploadManagerScreen.kt`
   - Purpose: Main Compose UI for the upload manager bottom sheet
   - Features:
     - Drag-and-drop reordering using the `reorderable` library
     - Media thumbnails with loading states
     - PDF thumbnail support using AndroidView
     - Status overlays for error, queued, and uploading states
     - Fully responsive Material 3 design
     - Dark mode support

### 3. **ComposeUploadManagerFragment.kt**
   - Location: `app/src/main/java/net/opendasharchive/openarchive/upload/ComposeUploadManagerFragment.kt`
   - Purpose: Wrapper fragment that hosts the Compose screen
   - Features:
     - Extends SKBottomSheetDialogFragment for consistent bottom sheet behavior
     - Integrates with MainActivity lifecycle
     - Handles retry dialogs using the existing dialog system
     - Provides the same public API as UploadManagerFragment

## Configuration

### Toggle Between Implementations

The implementation can be toggled using the `useComposeUploadManager` flag in `AppConfig`:

```kotlin
// AppConfig.kt
data class AppConfig(
    // ... other properties
    val useComposeUploadManager: Boolean = true, // Set to false to use XML version
)
```

**Default:** The Compose version is enabled by default (`true`)

### How It Works

In `MainActivity.kt`, the `showUploadManagerFragment()` method checks the flag:

```kotlin
fun showUploadManagerFragment() {
    if (uploadManagerFragment == null) {
        if (appConfig.useComposeUploadManager) {
            // Use Compose version
            uploadManagerFragment = ComposeUploadManagerFragment()
            uploadManagerFragment?.show(supportFragmentManager, ComposeUploadManagerFragment.TAG)
        } else {
            // Use XML version
            uploadManagerFragment = UploadManagerFragment()
            uploadManagerFragment?.show(supportFragmentManager, UploadManagerFragment.TAG)
        }

        // Stop the upload service when the bottom sheet is shown
        UploadService.stopUploadService(this)
    }
}
```

## Features

### Drag-to-Reorder
- Long-press on any media item to start dragging
- Reorder items in the upload queue
- Priorities are automatically updated in the database

### Media Item UI
Each media item displays:
- **Delete button** (trash icon) on the left
- **Thumbnail** showing:
  - Images: Actual image preview
  - Videos: Video thumbnail
  - PDFs: First page preview
  - Audio: Music icon placeholder
  - Other files: Generic file icon placeholder
- **Title and file info** in the middle
  - Shows file size or upload date
  - For error items, shows error message
- **Drag handle** (reorder icon) on the right

### Status Overlays
- **Error**: Red error icon overlay
- **Queued**: Circular progress indicator
- **Uploading**: Progress is managed by the service (no overlay in list)

### Actions
- **Delete**: Tap the trash icon to remove an item
- **Retry**: Tap on an error item to show retry dialog
- **Reorder**: Long-press and drag to reorder
- **Close**: Tap "DONE" button to close the bottom sheet

## Dependencies

### New Dependency Added

The Compose version uses the modern `sh.calvin.reorderable` library for drag-and-drop functionality (compatible with Compose 1.10.0+):

```kotlin
// gradle/libs.versions.toml
reorderable = "3.0.0"

reorderable = { group = "sh.calvin.reorderable", name = "reorderable", version.ref = "reorderable" }

// app/build.gradle.kts
implementation(libs.reorderable)
```

### Dependency Injection

The ViewModel is registered in Koin DI:

```kotlin
// FeaturesModule.kt
viewModelOf(::UploadManagerViewModel)
```

## API Compatibility

Both implementations provide the same public API, so they are drop-in replacements:

```kotlin
// Both support these methods:
fun updateItem(mediaId: Long)
fun removeItem(mediaId: Long)
fun refresh()
fun getUploadingCounter(): Int
```

## Testing

To test the implementation:

1. **Enable Compose version** (default):
   - Set `useComposeUploadManager = true` in AppConfig
   - Build and run the app
   - Upload some media items
   - Tap the upload notification to open the upload manager
   - Test drag-to-reorder, delete, and retry

2. **Test XML version** (for comparison):
   - Set `useComposeUploadManager = false` in AppConfig
   - Build and run the app
   - Verify the same functionality works

3. **Test error handling**:
   - Force an upload error (e.g., disconnect network)
   - Verify error overlay appears
   - Tap the error item to show retry dialog
   - Test retry and delete actions

## Migration Notes

### Differences from XML Version

1. **No ItemTouchHelper**: The Compose version uses the `reorderable` library instead of RecyclerView's ItemTouchHelper
2. **No DiffUtil**: Compose handles list updates efficiently without explicit DiffUtil
3. **State management**: Uses ViewModel with StateFlow instead of direct adapter manipulation
4. **Thumbnails**: Uses Coil3 Compose APIs for images/videos, AndroidView for PDFs

### Preserved Features

- ✅ Drag-to-reorder with priority updates
- ✅ Delete functionality
- ✅ Retry dialog for errors
- ✅ Status overlays (error, queued)
- ✅ File size and date display
- ✅ PDF thumbnail loading
- ✅ Dark mode support
- ✅ Same UX and visual design
- ✅ Integration with MainActivity lifecycle
- ✅ Fragment result listeners for retry actions

## Future Considerations

When MainActivity is fully migrated to Compose:

1. Replace `ComposeUploadManagerFragment` with a direct composable function
2. Use `ModalBottomSheet` from Material 3 Compose instead of BottomSheetDialogFragment
3. Remove the XML version files if no longer needed

## Reverting to XML Version

If you need to revert to the XML version:

1. Set `useComposeUploadManager = false` in AppConfig
2. Rebuild the app
3. Optionally, you can remove the Compose files (but they won't affect the app when disabled):
   - `UploadManagerViewModel.kt`
   - `UploadManagerScreen.kt`
   - `ComposeUploadManagerFragment.kt`

## File Checklist

- ✅ UploadManagerViewModel.kt - Created
- ✅ UploadManagerScreen.kt - Created
- ✅ ComposeUploadManagerFragment.kt - Created
- ✅ AppConfig.kt - Updated with toggle flag
- ✅ MainActivity.kt - Updated to support both implementations
- ✅ FeaturesModule.kt - Added ViewModel to DI
- ✅ build.gradle.kts - Added reorderable dependency
- ✅ Original UploadManagerFragment.kt - Preserved and untouched
- ✅ Original UploadMediaAdapter.kt - Preserved and untouched
- ✅ Original UploadMediaViewHolder.kt - Preserved and untouched

## String Resources Used

All UI strings use the existing string resources with translations:

- `R.string.edit_queue` - "Edit Queue"
- `R.string.uploading_is_paused` - "Uploading is paused"
- `R.string.done` - "Done"
- `R.string.error` - "Error"
- `R.string.menu_delete` - "Delete"
- `R.string.reorder` - "Reorder"
- `R.string.upload_unsuccessful` - "Upload Unsuccessful"
- `R.string.upload_unsuccessful_description` - Error description
- `R.string.lbl_retry` - "Retry"
- `R.string.btn_lbl_remove_media` - "Remove Media"

No new string resources were needed!

---

**Migration completed successfully!** 🎉

You can now use the Compose version by default, or easily toggle back to XML if needed.
