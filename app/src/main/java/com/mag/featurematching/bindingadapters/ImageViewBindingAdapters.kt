package com.mag.featurematching.bindingadapters

import android.graphics.Bitmap
import android.widget.ImageView
import androidx.databinding.BindingAdapter


@BindingAdapter("android:src")
fun setImageViewResource(imageView: ImageView, bitmap: Bitmap?) {
    imageView.setImageBitmap(bitmap)
}