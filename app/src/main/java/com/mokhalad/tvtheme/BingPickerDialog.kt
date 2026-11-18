package com.mokhalad.tvtheme

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * نافذة اختيار صورة من صور Bing (8 صور).
 * تعيد فهرس الصورة عبر onPick.
 */
class BingPickerDialog(
    private val context: Context,
    private val onPick: (Int) -> Unit
) {
    private fun dp(v: Int) = (v * context.resources.displayMetrics.density).toInt()
    private fun str(ar: String, en: String) = if (Prefs.s(context, "lang", "ar") == "en") en else ar

    fun show() {
        val loading = UI.tv(context, str("جارٍ التحميل…", "Loading…"), 16f, Color.WHITE).apply {
            gravity = Gravity.CENTER; setPadding(dp(40), dp(40), dp(40), dp(40))
        }
        val dialog = AlertDialog.Builder(context)
            .setTitle(str("صور Bing", "Bing Images"))
            .setView(loading)
            .setNegativeButton(str("إلغاء", "Cancel"), null)
            .create()
        dialog.show()

        Net.async({ Data.bingList(8) }) { list ->
            if (dialog.isShowing) dialog.dismiss()
            if (list.isNullOrEmpty()) {
                Toast.makeText(context, str("تعذر تحميل الصور", "Failed to load images"), Toast.LENGTH_SHORT).show()
                return@async
            }
            showGrid(list)
        }
    }

    private fun showGrid(list: List<BingImg>) {
        val p = Themes.get(context)
        val rv = RecyclerView(context).apply {
            layoutManager = GridLayoutManager(context, 3)
            setPadding(dp(16), dp(16), dp(16), dp(16))
            clipToPadding = false
        }
        val dialog = AlertDialog.Builder(context)
            .setTitle(str("اختر خلفية", "Choose Wallpaper"))
            .setView(rv)
            .setNegativeButton(str("إلغاء", "Cancel"), null)
            .create()

        rv.adapter = object : RecyclerView.Adapter<Holder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
                val box = FrameLayout(context).apply {
                    isFocusable = true
                    layoutParams = RecyclerView.LayoutParams(dp(210), dp(120)).apply {
                        setMargins(dp(6), dp(6), dp(6), dp(6))
                    }
                    background = UI.glass(p, 12f)
                    setOnFocusChangeListener { v, f ->
                        v.animate().scaleX(if (f) 1.05f else 1f).scaleY(if (f) 1.05f else 1f).setDuration(120).start()
                        v.background = UI.glass(p, 12f, f)
                    }
                }
                val img = ImageView(context).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setPadding(dp(6), dp(6), dp(6), dp(6))
                }
                box.addView(img, -1, -1)
                return Holder(box, img)
            }

            override fun onBindViewHolder(holder: Holder, position: Int) {
                val item = list[position]
                holder.img.setImageDrawable(null)
                Net.async({ Net.bitmap(item.thumb.ifEmpty { item.url }) }) { b ->
                    if (b != null && holder.img.tag == item.url) holder.img.setImageBitmap(b)
                }
                holder.img.tag = item.url
                holder.itemView.setOnClickListener {
                    onPick(item.idx)
                    dialog.dismiss()
                }
            }

            override fun getItemCount() = list.size
        }
        dialog.show()
        rv.post { rv.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus() }
    }

    private class Holder(v: FrameLayout, val img: ImageView) : RecyclerView.ViewHolder(v)
}