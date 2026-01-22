You basically need to replace this old concept:

> “When *any* Activity starts (except passcode activities), launch PasscodeEntryActivity.”

with the Compose/NavDisplay version:

> “When app returns to foreground (or when you decide it’s required), push a **PasscodeEntryRoute overlay** on top of whatever route is showing, and block content until it succeeds.”

Below is a step-by-step migration that keeps the same behavior and works cleanly with **single-activity + Navigation3**.

---

# Step 1 — Add a tiny “PasscodeGate” coordinator (no Activities)

Create a class that listens to **ProcessLifecycleOwner** and emits “lock required”.

```kotlin
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.opendasharchive.openarchive.util.Prefs

class PasscodeGate(
    private val prefs: Prefs // or just read Prefs directly if it’s an object
) : DefaultLifecycleObserver {

    private val _shouldLock = MutableStateFlow(false)
    val shouldLock: StateFlow<Boolean> = _shouldLock

    // optional: only lock when returning from background
    private var wasInBackground = true

    override fun onStart(owner: LifecycleOwner) {
        // App to foreground
        if (prefs.passcodeEnabled && wasInBackground) {
            _shouldLock.value = true
        }
        wasInBackground = false
    }

    override fun onStop(owner: LifecycleOwner) {
        // App to background
        wasInBackground = true
    }

    fun unlock() {
        _shouldLock.value = false
    }
}
```

> If `Prefs` is an object, remove the constructor param and use `Prefs.passcodeEnabled` directly.

---

# Step 2 — Register it once (App or DI)

In `SaveApp.onCreate`, instead of `registerActivityLifecycleCallbacks(PasscodeManager())`, do:

```kotlin
val passcodeGate: PasscodeGate by inject()
ProcessLifecycleOwner.get().lifecycle.addObserver(passcodeGate)
```

Koin module:

```kotlin
single { PasscodeGate(Prefs) } // or single { PasscodeGate(get()) } if Prefs wrapper
```

---

# Step 3 — Give `SaveNavGraph` the gate and enforce it

Inside `SaveNavGraph`, observe `shouldLock`. When it turns `true`, navigate to `PasscodeEntryRoute` **on top**.

### 3.1 Add helpers in your navigator (recommended)

You want “ensure route present only once”:

```kotlin
fun Navigator.ensureOnTop(route: AppRoute) {
    if (backstack.lastOrNull() != route) navigateTo(route)
}
```

### 3.2 Use it in `SaveNavGraph`

Inject gate:

```kotlin
val passcodeGate: PasscodeGate = koinInject()
```

Then react:

```kotlin
val shouldLock by passcodeGate.shouldLock.collectAsStateWithLifecycle()

LaunchedEffect(shouldLock) {
    if (shouldLock) {
        navigator.ensureOnTop(AppRoute.PasscodeEntryRoute)
    }
}
```

Now any time the app comes foreground and passcode is enabled, your nav stack gets the passcode screen.

---

# Step 4 — Make PasscodeEntryRoute behave like a “lock overlay”

You need two behaviors:

1. **Block back** while locked
2. On success, **pop** the passcode route and mark unlocked

### 4.1 In PasscodeEntryScreen, call unlock + pop on success

Update your route entry:

```kotlin
entry<AppRoute.PasscodeEntryRoute> { route ->
    val viewModel = koinViewModel<PasscodeEntryViewModel> { parametersOf(navigator, route) }
    val passcodeGate: PasscodeGate = koinInject()

    DefaultScaffold(
        title = stringResource(id = R.string.enter_passcode),
        onNavigateBack = { /* no-op or exit */ }
    ) {
        PasscodeEntryScreen(
            viewModel = viewModel,
            onSuccess = {
                passcodeGate.unlock()
                navigator.navigateBack() // pop PasscodeEntryRoute
            },
            onExit = {
                // optional: close app or go to welcome
            }
        )
    }
}
```

(You already had callbacks in the older version; same concept.)

### 4.2 Disable system back while locked

In the PasscodeEntry composable:

```kotlin
import androidx.activity.compose.BackHandler

BackHandler(enabled = true) {
    // do nothing, or call onExit()
}
```

That gives you the old “can’t bypass lock” behavior.

---

# Step 5 — Prevent showing passcode while setting it up

Old code excluded `PasscodeSetupActivity`. Do the same:

In your `LaunchedEffect(shouldLock)` block:

```kotlin
val current = navigator.backstack.lastOrNull()
val isInPasscodeFlow = current is AppRoute.PasscodeEntryRoute || current is AppRoute.PasscodeSetupRoute

if (shouldLock && !isInPasscodeFlow) {
    navigator.ensureOnTop(AppRoute.PasscodeEntryRoute)
}
```

---

# Step 6 — Optional improvements you probably want

### A) Cooldown: don’t relock if user just unlocked

Add `Prefs.lastUnlockedAt` and only lock if elapsed > X seconds.

### B) Lock on timeout, not only background

If you want “lock after 30 seconds idle”, do it by setting `_shouldLock.value = true` from a timer in `PasscodeGate`.

### C) Make `PasscodeGate` the single source of truth

Instead of checking `Prefs.passcodeEnabled` in many places, centralize it in the gate.

---

# What you can delete after migration

✅ `PasscodeManager : ActivityLifecycleCallbacks`
✅ `PasscodeEntryActivity` / `PasscodeSetupActivity` (once fully moved)
✅ manifest passcode activities

---

# Minimal changes summary

1. Add `PasscodeGate` (DefaultLifecycleObserver + StateFlow)
2. Register observer with `ProcessLifecycleOwner`
3. In `SaveNavGraph`, observe `shouldLock` and navigate to `PasscodeEntryRoute`
4. On success: `passcodeGate.unlock()` + `navigator.navigateBack()`
5. Block back with `BackHandler`

---

If you paste your `Navigator` implementation (navigateTo / navigateBack / navigateAndClear), I’ll show the exact “ensureOnTop + avoid duplicates” logic that fits your backstack type.
