package com.aemerse.bottomsheet_gallery_app;

import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.LayoutInflater;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.aemerse.bottomsheet_gallery.BottomSheetImagePicker;
import com.aemerse.bottomsheet_gallery_app.databinding.AcMainBinding;
import com.bumptech.glide.Glide;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AcMainJava extends AppCompatActivity implements BottomSheetImagePicker.OnImagesSelectedListener {

    private AcMainBinding binding;

    @Override
    public void onCreate(@androidx.annotation.Nullable Bundle savedInstanceState, @androidx.annotation.Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        binding = AcMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnClear.setOnClickListener(v -> binding.imageContainer.removeAllViews());
        binding.btnPickSingle.setOnClickListener(v -> new BottomSheetImagePicker.Builder(getString(R.string.file_provider))
                .singleSelectTitle(R.string.pick_single)
                .peekHeight(R.dimen.peekHeight)
                .requestTag("single")
                .show(getSupportFragmentManager(), null));
        binding.btnPickMulti.setOnClickListener(v -> new BottomSheetImagePicker.Builder(getString(R.string.file_provider))
                .columnSize(R.dimen.columnSize)
                .multiSelect(3, 6)
                .multiSelectTitles(
                        R.plurals.pick_multi,
                        R.plurals.pick_multi_more,
                        R.string.pick_multi_limit
                )
                .requestTag("multi")
                .show(getSupportFragmentManager(), null));
    }

    @Override
    public void onImagesSelected(@NotNull List<? extends Uri> uris, @Nullable String tag) {
        binding.imageContainer.removeAllViews();
        for (Uri uri : uris) {
            ImageView iv = (ImageView) LayoutInflater.from(this).inflate(R.layout.scrollitem_image, binding.imageContainer, false);
            binding.imageContainer.addView(iv);
            Glide.with(this).load(uri).into(iv);
        }
    }
}
