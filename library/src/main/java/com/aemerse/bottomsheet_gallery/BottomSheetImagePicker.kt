package com.aemerse.bottomsheet_gallery

import android.app.Dialog
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DimenRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.recyclerview.widget.SimpleItemAnimator
import com.aemerse.bottomsheet_gallery.databinding.ImagepickerBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kroegerama.kaiteki.recyclerview.layout.AutofitLayoutManager

class BottomSheetImagePicker internal constructor() : BottomSheetDialogFragment(), LoaderManager.LoaderCallbacks<Cursor> {

    private var currentPhotoUri: Uri? = null

    private var isMultiSelect = false
    private var multiSelectMin = 1
    private var multiSelectMax = Int.MAX_VALUE

    private var providerAuthority = ""
    private var requestTag = ""

    @StringRes
    private var resTitleSingle = R.string.imagePickerSingle
    @PluralsRes
    private var resTitleMulti = R.plurals.imagePickerMulti
    @PluralsRes
    private var resTitleMultiMore = R.plurals.imagePickerMultiMore
    @StringRes
    private var resTitleMultiLimit = R.string.imagePickerMultiLimit
    @DimenRes
    private var peekHeight = R.dimen.imagePickerPeekHeight
    @DimenRes
    private var columnSizeRes = R.dimen.imagePickerColumnSize
    @StringRes
    private var loadingRes = R.string.imagePickerLoading
    @StringRes
    private var emptyRes = R.string.imagePickerEmpty

    private var onImagesSelectedListener: OnImagesSelectedListener? = null
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>
    private var _binding: ImagepickerBinding? = null
    private val binding get() = _binding!!

    private val adapter by lazy {
        ImageTileAdapter(
            isMultiSelect,
            ::tileClick,
            ::selectionCountChanged
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnImagesSelectedListener) {
            onImagesSelectedListener = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadArguments()
        if (requireContext().hasReadStoragePermission) {
            LoaderManager.getInstance(this).initLoader(LOADER_ID, null, this)
        } else {
            requestReadStoragePermission(REQUEST_PERMISSION_READ_STORAGE)
        }
        if (savedInstanceState != null) {
            currentPhotoUri = savedInstanceState.getParcelable(STATE_CURRENT_URI)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ImagepickerBinding.inflate(inflater, container, false)
        val view = binding.root
        view.also {
            (parentFragment as? OnImagesSelectedListener)?.let { onImagesSelectedListener = it }
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvHeader.setOnClickListener {
            when {
                bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED -> {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
                else -> {
                    binding.recycler.smoothScrollToPosition(0)
                }
            }
        }
        binding.tvHeader.setText(resTitleSingle)
        binding.tvEmpty.setText(loadingRes)

        if (isMultiSelect) {
            binding.btnDone.isVisible = true
            binding.btnDone.setOnClickListener {
                onImagesSelectedListener?.onImagesSelected(adapter.getSelectedImages(), requestTag)
                dismissAllowingStateLoss()
            }
            binding.btnClearSelection.isVisible = true
            binding.btnClearSelection.setOnClickListener { adapter.clear() }
        }

        binding.recycler.layoutManager = AutofitLayoutManager(requireContext(), columnSizeRes)
        (binding.recycler.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        binding.recycler.adapter = adapter

        val oldSelection = savedInstanceState?.getIntArray(STATE_SELECTION)
        if (oldSelection != null) {
            adapter.selection = oldSelection.toHashSet()
        }
        selectionCountChanged(adapter.selection.size)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        super.onCreateDialog(savedInstanceState).apply {
            setOnShowListener {
                val bottomSheet = findViewById<View>(R.id.design_bottom_sheet)
                bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
                bottomSheetBehavior.peekHeight = resources.getDimensionPixelSize(peekHeight)
                bottomSheetBehavior.addBottomSheetCallback(bottomSheetCallback)
            }
        }

    private val bottomSheetCallback by lazy {
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                view?.alpha = if (slideOffset < 0f) 1f + slideOffset else 1f
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismissAllowingStateLoss()
                }
            }
        }
    }

    private fun tileClick(tile: ClickedTile) {
        when (tile) {
            is ClickedTile.ImageTile -> {
                onImagesSelectedListener?.onImagesSelected(listOf(tile.uri), requestTag)
                dismissAllowingStateLoss()
            }
        }
    }

    private fun selectionCountChanged(count: Int) {
        if (!isMultiSelect) return
        when {
            count < multiSelectMin -> {
                val delta = multiSelectMin - count
                binding.tvHeader.text = resources.getQuantityString(resTitleMultiMore, delta, delta)
            }
            count > multiSelectMax -> binding.tvHeader.text = getString(resTitleMultiLimit, multiSelectMax)
            else -> binding.tvHeader.text = resources.getQuantityString(resTitleMulti, count, count)
        }
        binding.btnDone.isEnabled = count in multiSelectMin..multiSelectMax
        binding.btnDone.animate().alpha(
            when {
                binding.btnDone.isEnabled -> 1f
                else -> .2f
            }
        )
        binding.btnClearSelection.isEnabled = count > 0
        binding.btnClearSelection.animate().alpha(
            when {
                binding.btnClearSelection.isEnabled -> 1f
                else -> .2f
            }
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(STATE_CURRENT_URI, currentPhotoUri)
        outState.putIntArray(STATE_SELECTION, adapter.selection.toIntArray())
    }

    private fun loadArguments() {
        val args = arguments ?: return
        isMultiSelect = args.getBoolean(KEY_MULTI_SELECT, isMultiSelect)
        multiSelectMin = args.getInt(KEY_MULTI_SELECT_MIN, multiSelectMin)
        multiSelectMax = args.getInt(KEY_MULTI_SELECT_MAX, multiSelectMax)
        providerAuthority = args.getString(KEY_PROVIDER, this::class.java.canonicalName)
        columnSizeRes = args.getInt(KEY_COLUMN_SIZE_RES, columnSizeRes)
        requestTag = args.getString(KEY_REQUEST_TAG, requestTag)

        resTitleSingle = args.getInt(KEY_TITLE_RES_SINGLE, resTitleSingle)
        resTitleMulti = args.getInt(KEY_TITLE_RES_MULTI, resTitleMulti)
        resTitleMultiMore = args.getInt(KEY_TITLE_RES_MULTI_MORE, resTitleMultiMore)
        resTitleMultiLimit = args.getInt(KEY_TITLE_RES_MULTI_LIMIT, resTitleMultiLimit)

        peekHeight = args.getInt(KEY_PEEK_HEIGHT, peekHeight)

        emptyRes = args.getInt(KEY_TEXT_EMPTY, emptyRes)
        loadingRes = args.getInt(KEY_TEXT_LOADING, loadingRes)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        if (id != LOADER_ID) throw IllegalStateException("illegal loader id: $id")
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC"
        return CursorLoader(requireContext(), uri, projection, null, null, sortOrder)
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
        binding.progress.isVisible = false
        binding.tvEmpty.setText(emptyRes)
        data ?: return

        val columnIndex = data.getColumnIndex(MediaStore.Images.Media._ID)
        val items = ArrayList<Uri>()
        while (items.size < MAX_CURSOR_IMAGES && data.moveToNext()) {
            val id = data.getLong(columnIndex)
            val contentUri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                id
            )
            items.add(contentUri)
        }
        data.moveToFirst()
        adapter.imageList = items
        binding.tvEmpty.isVisible = items.size == 0
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        adapter.imageList = emptyList()
    }

    interface OnImagesSelectedListener {
        fun onImagesSelected(uris: List<Uri>, tag: String?)
    }

    companion object {
        private const val LOADER_ID = 0x1337

        private const val REQUEST_PERMISSION_READ_STORAGE = 0x2000

        private const val KEY_PROVIDER = "provider"
        private const val KEY_REQUEST_TAG = "requestTag"

        private const val KEY_MULTI_SELECT = "multiSelect"
        private const val KEY_MULTI_SELECT_MIN = "multiSelectMin"
        private const val KEY_MULTI_SELECT_MAX = "multiSelectMax"
        private const val KEY_COLUMN_SIZE_RES = "columnCount"

        private const val KEY_TITLE_RES_SINGLE = "titleResSingle"
        private const val KEY_TITLE_RES_MULTI = "titleResMulti"
        private const val KEY_TITLE_RES_MULTI_MORE = "titleResMultiMore"
        private const val KEY_TITLE_RES_MULTI_LIMIT = "titleResMultiLimit"

        private const val KEY_TEXT_EMPTY = "emptyText"
        private const val KEY_TEXT_LOADING = "loadingText"

        private const val KEY_PEEK_HEIGHT = "peekHeight"

        private const val STATE_CURRENT_URI = "stateUri"
        private const val STATE_SELECTION = "stateSelection"

        private const val MAX_CURSOR_IMAGES = 512
    }

    class Builder(provider: String) {

        private val args = Bundle().apply {
            putString(KEY_PROVIDER, provider)
        }

        fun requestTag(requestTag: String) = args.run {
            putString(KEY_REQUEST_TAG, requestTag)
            this@Builder
        }

        fun multiSelect(min: Int = 1, max: Int = Int.MAX_VALUE) = args.run {
            putBoolean(KEY_MULTI_SELECT, true)
            putInt(KEY_MULTI_SELECT_MIN, min)
            putInt(KEY_MULTI_SELECT_MAX, max)
            this@Builder
        }

        fun columnSize(@DimenRes columnSizeRes: Int) = args.run {
            putInt(KEY_COLUMN_SIZE_RES, columnSizeRes)
            this@Builder
        }

        fun singleSelectTitle(@StringRes titleRes: Int) = args.run {
            putInt(KEY_TITLE_RES_SINGLE, titleRes)
            this@Builder
        }

        fun peekHeight(@DimenRes peekHeightRes: Int) = args.run {
            putInt(KEY_PEEK_HEIGHT, peekHeightRes)
            this@Builder
        }

        fun multiSelectTitles(
            @PluralsRes titleCount: Int,
            @PluralsRes titleNeedMore: Int,
            @StringRes titleLimit: Int
        ) = args.run {
            putInt(KEY_TITLE_RES_MULTI, titleCount)
            putInt(KEY_TITLE_RES_MULTI_MORE, titleNeedMore)
            putInt(KEY_TITLE_RES_MULTI_LIMIT, titleLimit)
            this@Builder
        }

        private fun build() = BottomSheetImagePicker().apply { arguments = args }

        fun show(fm: FragmentManager, tag: String? = null) = build().show(fm, tag)

    }
}

enum class ButtonType {
    Button
}
