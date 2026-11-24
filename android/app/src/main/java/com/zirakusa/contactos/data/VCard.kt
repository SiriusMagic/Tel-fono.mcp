package com.zirakusa.contactos.data

/** vCard 3.0, to share a contact with any app (and to read one shared with us). Pure. */
object VCard {
    fun build(c: Contact): String = buildString {
        append("BEGIN:VCARD\r\nVERSION:3.0\r\n")
        append("FN:").append(esc(c.shownName)).append("\r\n")
        val parts = c.name.trim().split(Regex("\\s+"))
        append("N:").append(esc(if (parts.size > 1) parts.drop(1).joinToString(" ") else "")).append(';').append(esc(parts.first())).append(";;;\r\n")
        c.phones.forEach { append("TEL;TYPE=").append(type(it.label)).append(':').append(esc(it.number)).append("\r\n") }
        c.emails.forEach { append("EMAIL;TYPE=").append(type(it.label)).append(':').append(esc(it.address)).append("\r\n") }
        if (c.org.isNotBlank()) append("ORG:").append(esc(c.org)).append("\r\n")
        c.birthday?.let { append("BDAY:").append(it.iso()).append("\r\n") }
        if (c.address.isNotBlank()) append("ADR;TYPE=HOME:;;").append(esc(c.address)).append(";;;;\r\n")
        if (c.note.isNotBlank()) append("NOTE:").append(esc(c.note)).append("\r\n")
        append("END:VCARD\r\n")
    }

    /** «Compartir» › «Texto»: name, numbers and emails, one per line. */
    fun plain(c: Contact) = (listOf(c.shownName) + c.phones.map { Phones.pretty(it.number) } + c.emails.map { it.address }).joinToString("\n")

    /** Many contacts in one .vcf («Exportar»). */
    fun buildAll(items: List<Contact>) = items.joinToString("") { build(it) }

    /** What the QR code of «Compartir» holds: the card without the long parts (note, address), so it stays easy to read. */
    fun forQr(c: Contact) = build(c.copy(note = "", address = "", phones = c.phones.take(3), emails = c.emails.take(2)))

    /** Every card in a .vcf (from this or another phone, 2.1, 3.0 or 4.0). */
    fun parseAll(text: String): List<Contact> =
        Regex("(?is)BEGIN:VCARD.*?END:VCARD").findAll(text).mapNotNull { parse(it.value) }.toList()

    fun parse(text: String): Contact? {
        if (!text.trimStart().startsWith("BEGIN:VCARD", true)) return null
        // Folded lines (a space at the start) belong to the line before; so does what follows a quoted-printable «=».
        val lines = ArrayList<String>()
        for (raw in text.replace("\r\n", "\n").split('\n')) {
            val last = lines.lastOrNull()
            when {
                last != null && (raw.startsWith(" ") || raw.startsWith("\t")) -> lines[lines.size - 1] = last + raw.drop(1)
                last != null && last.endsWith("=") && "QUOTED-PRINTABLE" in last.substringBefore(':').uppercase() -> lines[lines.size - 1] = last.dropLast(1) + raw
                else -> lines += raw
            }
        }
        var c = Contact()
        var n = ""
        for (line in lines.map { it.trim() }.filter { it.isNotEmpty() }) {
            val head = line.substringBefore(':')
            val body = line.substringAfter(':', "")
            val value = unesc(if ("QUOTED-PRINTABLE" in head.uppercase()) quoted(body) else body)
            // «item1.TEL» (from iPhones) is a TEL.
            val key = head.substringBefore(';').substringAfterLast('.').uppercase()
            val label = labelOf(head)
            c = when (key) {
                "FN" -> c.copy(name = value)
                "N" -> { n = value.split(';').let { p -> listOfNotNull(p.getOrNull(1), p.getOrNull(0)).filter { it.isNotBlank() }.joinToString(" ") }; c }
                "TEL" -> c.copy(phones = c.phones + Phone(value, label))
                "EMAIL" -> c.copy(emails = c.emails + Email(value, if (label == Label.MOBILE) Label.HOME else label))
                "ORG" -> c.copy(org = value.replace(';', ' ').trim())
                "BDAY" -> c.copy(birthday = Birthday.parse(value))
                "NOTE" -> c.copy(note = value)
                "ADR" -> c.copy(address = value.split(';').filter { it.isNotBlank() }.joinToString(", "))
                else -> c
            }
        }
        if (c.name.isBlank()) c = c.copy(name = n)
        return c.takeIf { it.shownName.isNotBlank() }
    }

    /** «=C3=B1» → «ñ» (UTF-8, as Android writes vCard 2.1). */
    private fun quoted(s: String): String {
        val out = java.io.ByteArrayOutputStream()
        var i = 0
        while (i < s.length) {
            val hex = if (s[i] == '=' && i + 2 < s.length) s.substring(i + 1, i + 3).toIntOrNull(16) else null
            if (hex != null) { out.write(hex); i += 3 } else { out.write(s[i].toString().toByteArray()); i++ }
        }
        return out.toString(Charsets.UTF_8.name())
    }

    private fun type(l: Label) = when (l) { Label.MOBILE -> "CELL"; Label.HOME -> "HOME"; Label.WORK -> "WORK"; Label.OTHER -> "OTHER" }
    private fun labelOf(head: String) = head.uppercase().let { h ->
        when { "CELL" in h -> Label.MOBILE; "WORK" in h -> Label.WORK; "HOME" in h -> Label.HOME; "TEL" in h -> Label.MOBILE; else -> Label.OTHER }
    }
    private fun esc(s: String) = s.replace("\\", "\\\\").replace("\n", "\\n").replace(",", "\\,").replace(";", "\\;")
    private fun unesc(s: String) = s.replace("\\n", "\n").replace("\\N", "\n").replace("\\,", ",").replace("\\;", ";").replace("\\\\", "\\")
}
