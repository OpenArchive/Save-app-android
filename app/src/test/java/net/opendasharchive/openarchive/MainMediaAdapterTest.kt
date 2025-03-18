package net.opendasharchive.openarchive

import android.app.Activity
import androidx.recyclerview.widget.RecyclerView
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.features.main.adapters.MainMediaAdapter
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import java.util.Date


// Since Media is a SugarRecord and calls save()/delete() internally,
// for testing we want to stub these methods.
// One option is to use a fake subclass. For simplicity, we’ll create a helper
// function that creates Media instances with test values. (In a real test suite,
// you may want to use a mocking library like Mockito or create a fake subclass.)
fun createTestMedia(
    id: Long,
    uri: String,
    status: Media.Status,
    progress: Int? = 0,
    selected: Boolean = false,
    title: String = "Test Media"
): Media {
    return Media(
        originalFilePath = uri,
        mimeType = "image/jpeg",
        createDate = Date(),
        title = title,
        description = "",
        author = "",
        location = "",
        tags = "",
        licenseUrl = null,
        mediaHash = byteArrayOf(),
        mediaHashString = "",
        status = status.id,
        statusMessage = "",
        projectId = 100,
        collectionId = 200,
        contentLength = 1024L,
        progress = 0,
        flag = false,
        priority = 0,
        selected = selected
    ).apply {
        // Stub out save() and delete() if needed (SugarRecord normally writes to DB).
        // For tests, you can override these to no-ops.
        // For example, if using Mockito, you could spy() and stub save()/delete().
    }
}

@RunWith(RobolectricTestRunner::class)
class MainMediaAdapterTest {


    private lateinit var activity: Activity
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MainMediaAdapter
    private lateinit var mediaList: MutableList<Media>

    @Before
    fun setup() {
        // Use Robolectric to create an Activity
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        recyclerView = RecyclerView(activity)
        // Create a dummy list of Media items
        mediaList = mutableListOf(
            createTestMedia(id = 1, uri = "file://uri1", status = Media.Status.Local, progress = 0),
            createTestMedia(
                id = 2,
                uri = "file://uri2",
                status = Media.Status.Queued,
                progress = 0
            ),
            createTestMedia(
                id = 3,
                uri = "file://uri3",
                status = Media.Status.Uploading,
                progress = 50
            )
        )
        adapter = MainMediaAdapter(
            activity,
            mediaList,
            recyclerView,
            checkSelecting = { },
            allowMultiProjectSelection = TODO(),
            onDeleteClick = TODO(),
        )
    }

    @Test
    fun testItemCountAndGetItemId() {
        // Verify that the item count matches the list size.
        assertEquals(mediaList.size, adapter.itemCount)
        // Verify stable IDs are returned correctly.
        assertEquals(1L, adapter.getItemId(0))
        assertEquals(2L, adapter.getItemId(1))
        assertEquals(3L, adapter.getItemId(2))
    }

    @Test
    fun testUpdateItemProgress() {
        // Update item with id 3 to a new progress value.
        val result = adapter.updateItem(3, progress = 80)
        assertTrue(result)
        val updatedMedia = adapter.media.first { it.id == 3L }
        assertEquals(80, updatedMedia.uploadPercentage)
        // Since we update the status when progress is updated, check that status is set to Uploading.
        assertEquals(Media.Status.Uploading.id, updatedMedia.status)
    }

    @Test
    fun testRemoveItem() {
        // Remove media with id 2.
        val result = adapter.removeItem(2)
        assertTrue(result)
        // After removal, item count should decrease.
        assertEquals(2, adapter.itemCount)
        // And no item with id 2 should exist.
        assertFalse(adapter.media.any { it.id == 2L })
    }

    @Test
    fun testUpdateData() {
        // Simulate a data refresh with a new media list.
        val newMediaList = listOf(
            createTestMedia(
                id = 1,
                uri = "file://uri1",
                status = Media.Status.Uploaded,
                progress = 100
            ),
            createTestMedia(id = 2, uri = "file://uri2", status = Media.Status.Local, progress = 0),
            createTestMedia(id = 4, uri = "file://uri4", status = Media.Status.Local, progress = 0)
        )
        adapter.updateData(newMediaList)
        // Verify that the adapter now has three items.
        assertEquals(3, adapter.itemCount)
        // New item (id = 4) should be present.
        assertTrue(adapter.media.any { it.id == 4L })
        // Item with id 3 should have been removed.
        assertFalse(adapter.media.any { it.id == 3L })
    }

    @Test
    fun testClearSelections() {
        // Mark some items as selected.
        adapter.media[0].selected = true
        adapter.media[1].selected = true
        // Call clearSelections() and verify all items are unselected.
        adapter.clearSelections()
        adapter.media.forEach { assertFalse(it.selected) }
    }

    @Test
    fun testOnItemMove() {
        // Enable edit mode so onItemMove works.
        adapter.isEditMode = true
        val firstItemId = adapter.media[0].id
        // Move item at position 0 to position 2.
        adapter.onItemMove(0, 2)
        // Check that the item now appears at position 2.
        assertEquals(firstItemId, adapter.media[2].id)
    }

    @Test
    fun testDeleteSelected() {
        // Mark two items as selected.
        adapter.media[0].selected = true
        adapter.media[2].selected = true
        val originalCount = adapter.itemCount
        // Call deleteSelected() and verify it returns true.
        val result = adapter.deleteSelected()
        assertTrue(result)
        // The new count should be originalCount - 2.
        assertEquals(originalCount - 2, adapter.itemCount)
        // Verify that no selected items remain.
        assertEquals(0, adapter.getSelectedCount())
    }
}