package com.aemerse.bottomsheet_gallery

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

internal class ImageTileAdapter(
    private val isMultiSelect: Boolean,
    private val clickListener: (ClickedTile) -> Unit,
    private val selectionCountChanged: (Int) -> Unit
) : RecyclerView.Adapter<VHImageTileBase>() {

    var imageList: List<Uri> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var selection = HashSet<Int>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun getSelectedImages(): ArrayList<Uri> = ArrayList<Uri>(selection.size).apply {
        selection.forEach { add(imageList.getOrNull(it) ?: return@forEach) }
    }

    fun clear() {
        selection = HashSet()
        selectionCountChanged(0)
    }

    override fun getItemViewType(position: Int): Int = VT_IMAGE

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHImageTileBase =
        LayoutInflater.from(parent.context).inflate(
            if (viewType == VT_IMAGE) R.layout.tile_image else R.layout.tile_special,
            parent, false
        ).let { view ->
            when (viewType) {
                VT_IMAGE -> VHImageTileBase.VHImageTile(view)
                else -> throw IllegalStateException("viewType $viewType not allowed")
            }
        }

    override fun getItemCount(): Int =
        if (isMultiSelect) imageList.size
        else imageList.size

    override fun onBindViewHolder(holder: VHImageTileBase, position: Int) {
        val pos = getCorrectPosition(position)
        (holder as? VHImageTileBase.VHImageTile)?.update(
            imageList[pos],
            selection.contains(position),
            ::onImageTileClick
        )
    }

    private fun onImageTileClick(selectView: View, position: Int) {
        if (!isMultiSelect) {
            val pos = getCorrectPosition(position)
            clickListener.invoke(ClickedTile.ImageTile(imageList[pos]))
            return
        }
        if (selection.contains(position)) {
            selectView.isVisible = false
            selection.remove(position)
        } else {
            selectView.isVisible = true
            selection.add(position)
        }
        selectionCountChanged.invoke(selection.size)
    }

    private fun getCorrectPosition(position: Int): Int =
        if (isMultiSelect) position
        else position

    companion object {
        private const val VT_IMAGE = 0x1002
    }
}

sealed class ClickedTile {
    data class ImageTile(val uri: Uri) : ClickedTile()
}

sealed class VHImageTileBase(
    view: View
) : RecyclerView.ViewHolder(view) {

    class VHImageTile(view: View) : VHImageTileBase(view) {
        private val ivImage = view.findViewById<ImageView>(R.id.ivImage)
        private val ivSelect = view.findViewById<View>(R.id.ivSelect)

        private var clickListener: ((selectView: View, position: Int) -> Unit)? = null

        init {
            view.setOnClickListener { clickListener?.invoke(ivSelect, adapterPosition) }
        }

        fun update(
            uri: Uri,
            selected: Boolean,
            clickListener: (selectView: View, position: Int) -> Unit
        ) {
            this.clickListener = clickListener
            ivSelect.isVisible = selected
            Glide.with(ivImage).load(uri).error(R.drawable.ic_broken_image).into(ivImage)
        }
    }
}