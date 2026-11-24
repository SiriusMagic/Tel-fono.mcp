package com.zirakusa.contactos

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.graphics.createBitmap
import com.zirakusa.contactos.data.Contact
import com.zirakusa.contactos.data.Phones
import com.zirakusa.contactos.data.Qr
import java.io.ByteArrayOutputStream

/** The card of «Compartir» as a picture (white, name, number, QR code and [caption]), for «Imagen» and «Guardar». */
object CardImage {
    fun png(c: Contact, name: String, label: String?, caption: String): ByteArray {
        val w = 1080
        val grid = Qr.card(c)
        val h = if (grid != null) 1500 else 700
        val bmp = createBitmap(w, h)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF111111.toInt(); textAlign = Paint.Align.CENTER }
        fun line(s: String, y: Float, size: Float, bold: Boolean = false, grey: Boolean = false) {
            text.textSize = size; text.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            text.color = if (grey) 0xFF6B6B70.toInt() else 0xFF111111.toInt()
            canvas.drawText(s.take(40), w / 2f, y, text)
        }
        line(name, 170f, 76f, bold = true)
        c.phones.firstOrNull()?.let { line(Phones.pretty(it.number), 270f, 56f) }
        if (label != null) line(label, 340f, 40f, grey = true)
        if (grid != null) {
            val side = 760; val cell = side.toFloat() / grid.size; val left = (w - side) / 2f; val top = 420f
            val dark = Paint().apply { color = Color.BLACK }
            for (y in 0 until grid.size) for (x in 0 until grid.size) if (grid[x, y])
                canvas.drawRect(left + x * cell, top + y * cell, left + (x + 1) * cell + 0.5f, top + (y + 1) * cell + 0.5f, dark)
            line(caption, top + side + 110f, 40f, grey = true)
        }
        return ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.PNG, 100, it); bmp.recycle() }.toByteArray()
    }
}
