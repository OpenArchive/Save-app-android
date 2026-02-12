EntryDecorator in Navigation 3
==============================


🎯 What Exactly Is `EntryDecorator`?
------------------------------------

`EntryDecorator` is one of the most important additions in **Navigation 3 (Navigation 2.8+)**.
It acts like a **middleware** for your navigation graph — letting you **modify**, **decorate**, or **inject data** into a `NavBackStackEntry` _right when it is created_.

In other words:

> **_EntryDecorator gives you a hook into the “moment of navigation” — before the destination becomes active._**

This unlocks capabilities Android developers never had before: per-entry lifecycle data, custom scopes, ephemeral state, analytics tagging, and more.



🧠 Why was `EntryDecorator` introduced?
---------------------------------------

Before Navigation 3, there was **no clean way** to:

*   inject dependencies per navigation entry
*   store per-entry custom state
*   share data between destinations without ViewModel
*   apply custom animations/flags based on _how_ the entry was created
*   intercept an entry the moment it is added

**Developers often relied on:**

*   shared ViewModels
*   savedStateHandle hacks
*   putting too much in arguments
*   custom controllers

`EntryDecorator` fixes all of this by giving a **first-class hook** into the back stack entry lifecycle.

🧱 What exactly does it do?
---------------------------

When navigation creates a new `NavBackStackEntry`, the `EntryDecorator` lets you:

### ✅ Add custom data

### ✅ Modify existing data

### ✅ Attach metadata

### ✅ Prepare UI context

### ✅ Initialize per-entry objects (state, managers, trackers)

✨ Official Definition (Simplified)
----------------------------------

**EntryDecorator allows you to transform NavBackStackEntry instances when they are created.**

It is registered on a `Navigator` (like `FragmentNavigator` or `ComposeNavigator`).

🧱 How `EntryDecorator` Works (Simple Explanation)
--------------------------------------------------

Whenever navigation pushes a new `NavBackStackEntry`:

```
navigate() → entry created → EntryDecorator runs → entry put on back stack
```

This gives you a chance to:

*   insert values into `SavedStateHandle`
*   attach ephemeral state
*   create DI scopes
*   inject single-use objects
*   mutate arguments
*   wrap or transform the entry

🔧 Full Example: Inject Analytics Per Navigation Entry
------------------------------------------------------

```
class AnalyticsEntryDecorator(
    private val tracker: AnalyticsTracker
) : EntryDecorator {
    override fun decorate(entry: NavBackStackEntry): NavBackStackEntry {
        val session = tracker.createSession(entry.destination.route)
        entry.savedStateHandle["analytics_session"] = session
        return entry
    }
}
```

Usage inside NavHost:

```
NavHost(
    navController = navController,
    startDestination = "home",
    entryDecorator = AnalyticsEntryDecorator(analytics)
) {
    composable("home") { HomeScreen() }
}
```

🧪 Real Use Cases (Where It Matters)
------------------------------------

✓ 1. Per-Entry DI Scopes
------------------------

Inject screen-scoped dependencies that die with the back stack entry.

✓ 2. Attaching Analytics Metadata
---------------------------------

Add flags, tags, route info — without polluting arguments.

✓ 3. Passing Ephemeral UI Info
------------------------------

Stuff that shouldn’t be saved across configuration changes (e.g., animation direction).

✓ 4. Debugging & Logging
------------------------

Record how and when entries are created.

✓ 5. Compose-Specific Lifecycles
--------------------------------

Attach objects needed by a composable that shouldn’t leak outside the entry lifecycle.

Visual Representation(In Simple Words)
--------------------------------------

**Without EntryDecorator:**

```
navigate() → entry added → you get it after it's already active
```

**With EntryDecorator:**

```
navigate() 
     ↓
EntryDecorator modifies entry
     ↓
entry added to back stack
     ↓
Composable/Fragment receives enriched entry
```

This small hook produces huge architectural improvements.

✨ Compose-Specific Example: Add Animation Direction
---------------------------------------------------

```
NavHost(
    navController = navController,
    startDestination = "home",
    entryDecorator = { entry ->
        val pop = entry.arguments?.getBoolean("isPop") ?: false
        entry.savedStateHandle["animationDirection"] = if (pop) "backward" else "forward"
        entry
    }
) {
    composable("home") { ... }
}
```

Your composables can now react to animations based on nav direction — cleanly and safely.

🚀 What Problem Does It Solve?
------------------------------

```
**| Problem Before                                  | Solution Now                            |
**| ----------------------------------------------- | --------------------------------------- |
| Hard to add custom metadata to each entry       | Decorator modifies entries when created |
| No central point to observe navigation creation | EntryDecorator gives a hook             |
| Per-destination state was tricky                | Add ephemeral state to entry            |
| Dependency injection scoping hacks              | Create per-entry DI scopes              |
| No awareness of how entry was triggered         | Decorator sees arguments, type, etc     |
```

🥳 Summary in One Line
----------------------

> **_EntryDecorator is a powerful new middleware that lets you modify or enrich navigation entries at creation time — solving long-standing gaps in Navigation architecture._**

