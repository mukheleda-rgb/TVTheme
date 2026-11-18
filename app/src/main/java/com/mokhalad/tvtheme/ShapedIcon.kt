package com.mokhalad.tvtheme

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.view.View

class ShapedIcon(c: Context, private val shape: String, private val fillColor: Int, ringCol: Int) : View(c) {
    private var d: Drawable? = null
    private var banner: Drawable? = null // ✅ دعم البانر
    private var bg: Drawable? = null
    private var fg: Drawable? = null
    private val path = Path()
    private val ringPath = Path()
    private val dens = c.resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    var accent: Int = ringCol
    var ring = false
        set(v) { field = v; invalidate() }

    fun setApp(icon: Drawable, bannerImg: Drawable? = null) {
        d = icon
        banner = bannerImg
        if (icon is AdaptiveIconDrawable) { bg = icon.background; fg = icon.foreground } else { bg = null; fg = null }
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        val fw = w.toFloat(); val fh = h.toFloat()
        path.reset()
        when (shape) {
            "circle" -> path.addOval(RectF(0f, 0f, fw, fh), Path.Direction.CW)
            "diamond" -> { path.moveTo(fw / 2, 0f); path.lineTo(fw, fh / 2); path.lineTo(fw / 2, fh); path.lineTo(0f, fh / 2); path.close() }
            "card" -> path.addRoundRect(RectF(0f, 0f, fw, fh), fw * 0.1f, fw * 0.1f, Path.Direction.CW)
            else -> path.addRoundRect(RectF(0f, 0f, fw, fh), fw * 0.2f, fw * 0.2f, Path.Direction.CW)
        }
        // المؤشر يُرسم خارج الأيقونة (بفاصل صغير) كي لا يتداخل معها ولا يُقص من الحواف
        val g = 5f * dens
        val m = Matrix()
        m.setScale((fw + 2 * g) / fw, (fh + 2 * g) / fh, fw / 2, fh / 2)
        ringPath.set(path); ringPath.transform(m)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width; val h = height
        
        if (shape == "card" && banner != null) {
            // ✅ رسم البانر الرسمي داخل البطاقة
            canvas.save()
            canvas.clipPath(path)
            banner?.setBounds(0, 0, w, h)
            banner?.draw(canvas)
            canvas.restore()
        } else if (shape == "default") {
            d?.let { it.setBounds(0, 0, w, h); it.draw(canvas) }
        } else {
            paint.style = Paint.Style.FILL; paint.color = if(shape=="card") fillColor else fillColor
            canvas.drawPath(path, paint)
            canvas.save()
            canvas.clipPath(path)
            val b = bg; val f = fg
            if (b != null && f != null) {
                val ox = if (shape == "diamond") 0 else w / 4
                val oy = if (shape == "diamond") 0 else h / 4
                b.setBounds(-ox, -oy, w + ox, h + oy); b.draw(canvas)
                f.setBounds(-ox, -oy, w + ox, h + oy); f.draw(canvas)
            } else {
                val pad = if (shape == "diamond") w / 5 else if (shape == "card") w / 4 else w / 10
                d?.let { it.setBounds(pad, pad, w - pad, h - pad); it.draw(canvas) }
            }
            canvas.restore()
        }
        
        if (ring) {
            paint.style = Paint.Style.STROKE
            paint.color = (accent and 0x00FFFFFF) or 0x55000000
            paint.strokeWidth = 6f * dens
            canvas.drawPath(ringPath, paint)
            paint.color = accent
            paint.strokeWidth = 3f * dens
            canvas.drawPath(ringPath, paint)
        }
    }
}