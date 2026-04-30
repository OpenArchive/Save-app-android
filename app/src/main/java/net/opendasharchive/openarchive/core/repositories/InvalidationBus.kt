package net.opendasharchive.openarchive.core.repositories

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * InvalidationBus - A centralized signal bus for manual reactivity with Sugar ORM.
 * Each flow acts as an "invalidation signal" that triggers re-queries in repositories.
 *
 * Write -> Signal Mapping:
 * - Space write -> invalidateSpaces() + maybe invalidateCurrentSpace()
 * - Current space change -> invalidateCurrentSpace() + invalidateProjects() + invalidateCollections() + invalidateMedia()
 * - Project write -> invalidateProjects()
 * - Collection/Media write -> invalidateCollections() + invalidateMedia()
 */
object InvalidationBus {
    private val _spaces = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
    val spaces: SharedFlow<Unit> = _spaces.asSharedFlow()

    private val _currentSpace = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
    val currentSpace: SharedFlow<Unit> = _currentSpace.asSharedFlow()

    private val _projects = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
    val projects: SharedFlow<Unit> = _projects.asSharedFlow()

    private val _collections = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
    val collections: SharedFlow<Unit> = _collections.asSharedFlow()

    private val _media = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
    val media: SharedFlow<Unit> = _media.asSharedFlow()

    private fun MutableSharedFlow<Unit>.ping() = tryEmit(Unit)

    fun invalidateSpaces() = _spaces.ping()
    fun invalidateCurrentSpace() = _currentSpace.ping()
    fun invalidateProjects() = _projects.ping()
    fun invalidateCollections() = _collections.ping()
    fun invalidateMedia() = _media.ping()

    /**
     * Trigger a full refresh across all domains.
     */
    fun invalidateAll() {
        _spaces.ping()
        _currentSpace.ping()
        _projects.ping()
        _collections.ping()
        _media.ping()
    }
}
