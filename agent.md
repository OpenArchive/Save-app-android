## Task Context
- Migrate `PreviewActivity` (XML) to a Compose-based `PreviewMediaScreen`, similar pattern as `ReviewMediaScreen`.
- Add navigation entry via `SpaceSetupActivity` (new `StartDestination` and nav graph node) so we can launch Compose Preview and navigate to Compose Review.
- Build reusable `MediaListItem` composable that mirrors `rv_media_box.xml`/`PreviewViewHolder` logic (image/video thumbnails via Coil, PDF thumbnail via `PdfThumbnailLoader`, placeholders with titles, video icon, selection overlay/border, status overlays for queued/uploading/error with progress and error icon).
- Preserve multi-select behavior (long-press to enter selection mode). Normal bottom bar shows “Add more”; selection bar shows edit, select/deselect all, delete. App bar needs upload button. Add more long-press should surface existing picker menu/bottom sheet.
- Keep ability to switch between XML and Compose implementations during navigation (similar toggle as Review path).
- Existing entry points: `PreviewActivity.start`, `MainActivity.navigateToPreview`, `MainMediaScreen` comments. Compose flows are hosted in `SpaceSetupActivity` nav graph (`app_nav_graph.xml`) using fragments/Compose destinations.

## Pending Design Notes
- Create `PreviewMediaFragment` (Compose host, `ToolbarConfigurable`) with menu `menu_preview` upload action.
- Add `PreviewMediaViewModel` (state/action/event) registered in Koin module.
- Add nav graph destination `fragment_preview_media` with args (project_id); add `StartDestination.PREVIEW_MEDIA` handling in `SpaceSetupActivity`.
- Update `PreviewActivity.start` and `MainActivity.navigateToPreview` to launch Compose via `SpaceSetupActivity` by default (keep toggle for old XML).
- Compose UI: `PreviewMediaScreen` → `PreviewMediaContent` using `LazyVerticalGrid` (2 cols) + bottom overlays; `MediaListItem` composable replicating `rv_media_box`.
- Bridge actions needing platform features (Picker, PermissionManager, content picker sheet, upload confirmation) through fragment events.

## Progress Notes (current)
- Implemented `PreviewMediaViewModel` (state/action/event, refresh/delete/select/upload logic, add-more/menu events, review navigation).
- Added Compose `PreviewMediaFragment`/`PreviewMediaScreen` with `MediaListItem` composable mirroring `rv_media_box` (thumbnails with Coil/video frame, PDF via `PdfThumbnailLoader` in `AndroidView`, selection overlay/border, status overlays, video badge, placeholder icons/titles). Bottom bars for add-more (long-press menu) and selection actions implemented.
- Added `StartDestination.PREVIEW_MEDIA`, nav graph destination `fragment_preview_media`, and Compose default routing via `PreviewActivity.start`; upload warning dialog preserved.
