package com.zirakusa.contactos.data

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** A square grid of dark (true) and light modules, with its quiet margin. */
class Grid(val size: Int, private val cells: BooleanArray) {
    operator fun get(x: Int, y: Int) = cells[y * size + x]
}

/**
 * The QR code of a contact card, made on the phone with ZXing (no internet). Copied from the family's QR app
 * (qr-mcp, `data/Codes.kt`, `Codes.encode`), so both draw codes the same way.
 */
object Qr {
    /** A QR code for [text] (UTF‑8, medium error correction); null when it doesn't fit in one. */
    fun encode(text: String, margin: Int = 2): Grid? = try {
        val hints = mapOf(EncodeHintType.CHARACTER_SET to "UTF-8", EncodeHintType.MARGIN to margin, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M)
        val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, hints)
        Grid(m.width, BooleanArray(m.width * m.height) { m.get(it % m.width, it / m.width) })
    } catch (_: WriterException) { null } catch (_: IllegalArgumentException) { null }

    /** The card's code: its vCard (see [VCard.forQr]). */
    fun card(c: Contact) = encode(VCard.forQr(c))
}
