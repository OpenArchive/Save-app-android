# Navigation 3 Analytics and Session Tracking

## Context

`BaseFragment` previously centralized screen analytics and session tracking through lifecycle callbacks:

- `onResume()`
  - set current screen for logging
  - track screen view start
  - update session tracker current screen
  - track navigation transition from previous screen
- `onPause()`
  - track time spent on current screen
  - update previous screen reference

After migrating to Navigation 3 + Compose screens, this behavior is now implemented at the navigation entry level in:

- `app/src/main/java/net/opendasharchive/openarchive/features/main/ui/SaveNavGraph.kt`

## Implementation

### 1. Centralized decorator for screen tracking

`SaveNavGraph` now installs:

- `rememberAnalyticsNavEntryDecorator(analyticsManager, sessionTracker)`

inside `NavDisplay(entryDecorators = ...)`.

This replaces the previous logging-only decorator and applies tracking uniformly to all screens in the graph.

### 2. Dependencies injected at nav-graph level

Inside `SaveNavGraph`:

- `AnalyticsManager` is injected via Koin.
- `SessionTracker` is injected via Koin.

These are passed into the decorator factory so all entries use the same analytics/session services.

### 3. Lifecycle-driven parity with `BaseFragment`

Within the decorator:

- `ON_RESUME`:
  - stores screen start time per entry key
  - calls `AppLogger.setCurrentScreen(screenName)`
  - calls `sessionTracker.setCurrentScreen(screenName)`
  - calls `analyticsManager.trackScreenView(screenName, null, previousScreen)`
  - calls `analyticsManager.trackNavigation(previousScreen, screenName)` when previous screen exists and differs

- `ON_PAUSE`:
  - computes seconds spent on that screen
  - calls `analyticsManager.trackScreenView(screenName, timeSpent, previousScreen)`
  - updates `previousScreen = screenName`

### 4. Entry cleanup

On pop (`onPop`), per-entry timing state is removed from the decorator map to avoid stale tracking data.

### 5. Screen naming

A helper resolves analytics screen names from navigation keys:

- for `AppRoute`: class simple name fallback to deeplink string
- otherwise: class simple name fallback to `toString()`

## Why this approach

- Keeps analytics logic centralized like `BaseFragment`.
- Avoids per-screen boilerplate.
- Uses Navigation 3 `NavEntryDecorator` as the proper extension point for per-entry behavior.
- Preserves existing `AnalyticsManager` and `SessionTracker` contracts without changing their APIs.

## Validation checklist

1. Navigate `A -> B`:
   - screen view start for `A`
   - screen view start for `B`
   - navigation event `A -> B`
2. Navigate back `B -> A`:
   - time spent event for `B`
   - navigation event `B -> A`
3. Confirm `sessionTracker.currentScreen` updates to currently resumed screen.
4. Confirm no duplicate events from recomposition (events tied to lifecycle callbacks, not pure composition).

