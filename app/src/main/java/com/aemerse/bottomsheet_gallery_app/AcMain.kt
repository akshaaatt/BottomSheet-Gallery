package com.aemerse.bottomsheet_gallery_app

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aemerse.bottomsheet_gallery.BottomSheetImagePicker
import com.aemerse.bottomsheet_gallery_app.databinding.AcMainBinding
import com.bumptech.glide.Glide

class AcMain : AppCompatActivity(), BottomSheetImagePicker.OnImagesSelectedListener {

    private lateinit var binding: AcMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AcMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPickSingle.setOnClickListener { pickSingle() }
        binding.btnPickMulti.setOnClickListener { pickMulti() }
        binding.btnClear.setOnClickListener { binding.imageContainer.removeAllViews() }
    }

    private fun pickSingle() {
        BottomSheetImagePicker.Builder(getString(R.string.file_provider))
            .singleSelectTitle(R.string.pick_single)
            .peekHeight(R.dimen.peekHeight)
            .requestTag("single")
            .show(supportFragmentManager)
    }

    private fun pickMulti() {
        BottomSheetImagePicker.Builder(getString(R.string.file_provider))
            .columnSize(R.dimen.columnSize)
            .multiSelect(3, 6)
            .multiSelectTitles(
                R.plurals.pick_multi,
                R.plurals.pick_multi_more,
                R.string.pick_multi_limit
            )
            .requestTag("multi")
            .show(supportFragmentManager)
    }

    override fun onImagesSelected(uris: List<Uri>, tag: String?) {
        Toast.makeText(this,"Result from tag: $tag", Toast.LENGTH_SHORT).show()

        binding.imageContainer.removeAllViews()
        uris.forEach { uri ->
            val iv = LayoutInflater.from(this).inflate(
                R.layout.scrollitem_image,
                binding.imageContainer,
                false
            ) as ImageView
            binding.imageContainer.addView(iv)
            Glide.with(this).load(uri).into(iv)
        }
    }
}