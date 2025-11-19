## App masking feature for Save app — action plan

### 1. Current context
- Entry point already exposed in `SettingsFragment` under Security, directly beneath “Set up passcode”.
- The previous `AppMaskingActivity` (checkbox toggles) will be removed; the screen will instead be provided as a Compose destination in `app_nav_graph` (hosted by `SpaceSetupActivity`).
- Passcode is optional, so masking cannot depend on lock verification.

### 2. Immediate implementation goals (Compose rewrite)
1. **Navigation migration**  
   - Remove the legacy `AppMaskingActivity`.  
   - Add a Compose destination entry (`<composable>`) to `app_nav_graph.xml` and a matching enum in `StartDestination`.  
   - Update `SpaceSetupActivity` to accept a new start destination for app masking and launch this nav graph when the Settings preference is tapped.
2. **Mask catalog**  
   - Provide four selectable personas backed by data class `AppMask`:  
     - `Save (default)` – standard icon and label.  
     - `QuickCalc` – calculator disguise.  
     - `LexiNote` – dictionary disguise.  
     - `Daylight Planner` – calendar disguise.  
   - Each entry carries alias name, icon, short label, description, and confirmation copy.
3. **Layout & interactions**  
   - Display mask cards in a vertical list (LazyColumn) with icon, title, subtitle, and status chip (“Active”).  
   - Tapping a card opens a confirmation sheet/modal (“Use QuickCalc appearance?”).  
   - Confirmation triggers alias switch using `AppMaskingUtils.setLauncherActivityAlias` and shows inline progress indicator for a brief moment.  
   - After success, show toast/snackbar “Appearance updated” and close activity with `finishAffinity()` to restart entry point smoothly.
4. **Utilities & state**  
   - Extend `AppMaskingUtils` to expose `availableMasks(context)` and `getCurrentMask()` wrappers.  
   - Persist currently applied mask metadata for UI state (alias + friendly name).  
   - Ensure manifest defines four `<activity-alias>` entries with matching labels/icons.
5. **UX polish**  
   - Add header text explaining limitation (“Appearance change may still show Save in system settings”).  
   - Provide `Use this look` button within each card when inactive; disabled `Current look` chip when active.  
   - Progress overlay uses Compose `Dialog` or full-width `LinearProgressIndicator` to keep transition smooth.

### 3. Risks & mitigations
- **Restart confusion**: clearly message that Save will restart; call `finishAffinity()` after switch.
- **Alias mismatch**: guard alias change result and show error snackbar if PackageManager throws.
- **Accessibility**: ensure cards are focusable, large touch targets, TalkBack-friendly labels describing new appearance.

### 4. Next step (enhanced redesign direction)
Once the Compose MVP ships, explore a richer disguise experience that feels unique to Save:
1. **Persona gallery** – introduce storytelling cards with subtle animations, showing context screenshots (e.g., a mock calculator keypad) and allowing preview before confirmation.
2. **Quick toggles** – pin the active mask in Settings summary with one-tap switcher chip to revert to Save without leaving Settings.
3. **Smart reminders** – after enabling a disguise, offer optional prompt to set/verify passcode for increased privacy (non-blocking).  
4. **Custom sets** – allow users to combine icon + label + accent color (within pre-approved safe set) to craft their own believable disguise, ensuring differentiation from other apps.  
5. **Automation hooks** – integrate with panic mode/quick exit so that activating a panic trigger can automatically fall back to the default identity or switch to the calmest persona.

This two-stage approach keeps the first release focused and achievable (Compose rewrite with 4 masks) while paving the way for a distinct, advanced masking experience tailored to Save. Once you confirm this plan, I can start implementing the Compose migration and mask catalog.
