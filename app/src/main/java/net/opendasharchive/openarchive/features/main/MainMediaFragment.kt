package net.opendasharchive.openarchive.features.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.databinding.FragmentMainMediaBinding
import net.opendasharchive.openarchive.databinding.ViewSectionBinding
import net.opendasharchive.openarchive.db.sugar.Collection
import net.opendasharchive.openarchive.db.sugar.Media
import net.opendasharchive.openarchive.db.sugar.Space
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.features.main.adapters.MainMediaAdapter
import net.opendasharchive.openarchive.upload.BroadcastManager
import net.opendasharchive.openarchive.upload.UploadJobScheduler
import net.opendasharchive.openarchive.util.extensions.toggle
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import kotlin.collections.set

class MainMediaFragment : BaseFragment() {

    companion object {
        private const val COLUMN_COUNT = 3
        private const val ARG_PROJECT_ID = "project_id"

        fun newInstance(projectId: Long): MainMediaFragment {
            val args = Bundle()
            args.putLong(ARG_PROJECT_ID, projectId)

            val fragment = MainMediaFragment()
            fragment.arguments = args

            return fragment
        }
    }

    private val uploadJobScheduler: UploadJobScheduler by inject()

    private var mAdapters = HashMap<Long, MainMediaAdapter>()
    private var mSection = HashMap<Long, SectionViewHolder>()
    private var mProjectId = -1L
    private var mCollections = mutableMapOf<Long, Collection>()

    private var selectedMediaIds = mutableSetOf<Long>()
    private var isSelecting = false
    private var selectionHasActiveItems = false

    private lateinit var binding: FragmentMainMediaBinding

    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        private val handler = Handler(Looper.getMainLooper())
        override fun onReceive(context: Context, intent: Intent) {
            val action = BroadcastManager.getAction(intent) ?: return

            when (action) {
                BroadcastManager.Action.Change -> {
                    handler.post {
                        updateProjectItem(
                            collectionId = action.collectionId,
                            mediaId = action.mediaId,
                            progress = action.progress,
                            isUploaded = action.isUploaded
                        )
                    }
                }

                BroadcastManager.Action.Delete -> {
                    handler.post {
                        refresh()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        BroadcastManager.register(requireContext(), mMessageReceiver)
    }

    override fun onStop() {
        super.onStop()
        BroadcastManager.unregister(requireContext(), mMessageReceiver)
    }

    override fun onPause() {
        cancelSelection()
        super.onPause()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mProjectId = arguments?.getLong(ARG_PROJECT_ID, -1) ?: -1

        binding = FragmentMainMediaBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val space = Space.current
        val text: String = if (space != null) {
            val projects = space.projects
            if (projects.isNotEmpty()) {
                getString(R.string.tap_to_add)
            } else {
                getString(R.string.tap_to_add_folder)
            }
        } else {
            getString(R.string.tap_to_add_server)
        }

        binding.tvWelcomeDescr.text = text

        if (space != null) {
            binding.tvWelcome.visibility = View.INVISIBLE
        } else {
            binding.tvWelcome.visibility = View.VISIBLE
        }


        refresh()
    }

    fun updateProjectItem(collectionId: Long, mediaId: Long, progress: Int, isUploaded: Boolean) {
        AppLogger.i("Current progress for $collectionId: ", progress)
        mAdapters[collectionId]?.apply {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                updateItem(mediaId, progress, isUploaded)
                if (progress == -1) {
                    updateHeader(collectionId, media)
                }
            }
        }
    }

    private fun updateHeader(collectionId: Long, media: ArrayList<Media>) {
        lifecycleScope.launch(Dispatchers.IO) {
            Collection.get(collectionId)?.let { collection ->
                mCollections[collectionId] = collection
                withContext(Dispatchers.Main) {
                    mSection[collectionId]?.setHeader(collection, media)
                }
            }
        }
    }
    //codex resume 019b3b3f-4cab-7393-b441-28cfe89f47f6
    fun refresh() {
        mCollections = Collection.getByProject(mProjectId).associateBy { it.id }.toMutableMap()

        // Remove all sections, which' collections don't exist anymore.
        val toDelete = mAdapters.keys.filter { id ->
            mCollections.containsKey(id).not()
        }.toMutableList()

        mCollections.forEach { (id, collection) ->
            val media = collection.media

            // Also remove all empty collections.
            if (media.isEmpty()) {
                toDelete.add(id)
                return@forEach
            }

            val adapter = mAdapters[id]
            val holder = mSection[id]

            if (adapter != null) {
                adapter.updateData(media)
                holder?.setHeader(collection, media)
            } else if (media.isNotEmpty()) {
                val view = createMediaList(collection, media)

                binding.mediaContainer.addView(view, 0)
            }
        }

        // DO NOT delete the collection here, this could lead to a race condition
        // while adding images.
        deleteCollections(toDelete, false)

        binding.addMediaHint.toggle(mCollections.isEmpty())
    }

    fun enableSelectionMode() {
        isSelecting = true
        selectionHasActiveItems = false
        mAdapters.values.forEach { it.selecting = true }
        updateSelectionState()
    }

    fun cancelSelection() {
        isSelecting = false
        selectionHasActiveItems = false
        selectedMediaIds.clear()
        mAdapters.values.forEach { it.clearSelections() }
        updateSelectionCount()
    }

    fun deleteSelected() {
        val toDelete = ArrayList<Long>()

        mCollections.forEach { (id, collection) ->
            if (mAdapters[id]?.deleteSelected() == true) {
                val media = collection.media

                if (media.isEmpty()) {
                    toDelete.add(collection.id)
                } else {
                    mSection[id]?.setHeader(collection, media)
                }
            }
        }

        deleteCollections(toDelete, true)
        // If all collections are removed or empty, show add media hint.
        binding.addMediaHint.toggle(mCollections.isEmpty())
    }

    private fun createMediaList(collection: Collection, media: List<Media>): View {
        val holder = SectionViewHolder(ViewSectionBinding.inflate(layoutInflater))
        holder.recyclerView.layoutManager = GridLayoutManager(activity, COLUMN_COUNT)
        holder.recyclerView.isNestedScrollingEnabled = false

        holder.setHeader(collection, media)

        val mediaAdapter = MainMediaAdapter(
            activity = requireActivity(),
            mediaList = media,
            recyclerView = holder.recyclerView,
            checkSelecting = { updateSelectionState() },
            onDeleteClick = { mediaItem, itemPosition ->
                showDeleteConfirmationDialog(
                    mediaItem = mediaItem,
                    itemPosition = itemPosition
                )

            }
        )

        holder.recyclerView.adapter = mediaAdapter
        mAdapters[collection.id] = mediaAdapter
        mSection[collection.id] = holder

        return holder.root
    }

    private fun showDeleteConfirmationDialog(mediaItem: Media, itemPosition: Int) {
        dialogManager.showDialog(dialogManager.requireResourceProvider()) {
            type = DialogType.Error
            title = UiText.Resource(R.string.upload_unsuccessful)
            message = UiText.Resource(R.string.upload_unsuccessful_description)
            positiveButton {
                text = UiText.Resource(R.string.lbl_retry)
                action = {
                    mediaItem.apply {
                        sStatus = Media.Status.Queued
                        statusMessage = ""
                        save()
                        BroadcastManager.postChange(
                            requireActivity(),
                            mediaItem.collectionId,
                            mediaItem.id
                        )
                    }
                    uploadJobScheduler.schedule()
                }
            }
            destructiveButton {
                text = UiText.Resource(R.string.btn_lbl_remove_media)
                action = {
                    val adapter = mAdapters[mediaItem.collectionId]
                    adapter?.deleteItem(itemPosition)
                }
            }
        }
    }

    //update selection UI by summing selected counts from all adapters.
    fun updateSelectionState() {
        val totalSelected = mAdapters.values.sumOf { it.getSelectedCount() }

        if (isSelecting && totalSelected > 0) {
            selectionHasActiveItems = true
        }
        // If we were in selection mode, had selections, and now none remain, exit selection.
        if (isSelecting && selectionHasActiveItems && totalSelected == 0) {
            isSelecting = false
            selectionHasActiveItems = false
        }

        val selectionActive = isSelecting || totalSelected > 0

        // Keep all adapters in sync so a tap in any collection can toggle selection.
        mAdapters.values.forEach { adapter ->
            adapter.selecting = selectionActive
        }

        (activity as? MainActivity)?.setSelectionMode(selectionActive)
        (activity as? MainActivity)?.updateSelectedCount(totalSelected)
    }


    private fun updateSelectionCount() {
        (activity as? MainActivity)?.updateSelectedCount(selectedMediaIds.size)
    }

    private fun deleteCollections(collectionIds: List<Long>, cleanup: Boolean) {
        collectionIds.forEach { collectionId ->
            mAdapters.remove(collectionId)

            val holder = mSection.remove(collectionId)
            (holder?.root?.parent as? ViewGroup)?.removeView(holder.root)

            mCollections[collectionId]?.let {
                mCollections.remove(collectionId)
                if (cleanup) {
                    it.delete()
                }
            }
        }
    }

    fun showUploadManager() {
        (activity as? MainActivity)?.showUploadManagerFragment()
    }

    fun setArrowVisible(visible: Boolean) {
        binding.imgWelcomeArrowLayout.visibility =
            if (visible) View.VISIBLE else View.INVISIBLE
    }


    override fun getToolbarTitle(): String = ""
}
