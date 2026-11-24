package com.zirakusa.contactos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SimCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.contactos.data.AccountKind
import com.zirakusa.contactos.data.Accounts
import com.zirakusa.contactos.data.Contact
import com.zirakusa.contactos.ui.theme.ContactosColors

private val tints = listOf(0xFFF06292, 0xFF4FC3F7, 0xFF4CE08A, 0xFFFF9A1F, 0xFFB57BEE, 0xFFF5C518, 0xFF26C6DA, 0xFFEF5350).map { Color(it) }

/**
 * A round picture with the contact's initials, always the same colour for the same name, and a small mark when it is
 * saved in a SIM (with its slot) or in Google.
 */
@Composable fun Avatar(c: Contact, size: Dp = 44.dp, badge: Boolean = true) = Box(Modifier.size(size)) {
    val tint = tints[Math.floorMod(c.shownName.hashCode(), tints.size)]
    Box(Modifier.size(size).clip(CircleShape).background(tint.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
        Text(c.initials, color = tint, fontSize = (size.value * 0.38f).sp, fontWeight = FontWeight.SemiBold)
    }
    val mark = if (badge) Accounts.badge(c.raws) else null
    if (mark != null) {
        val side = (size * 0.42f).coerceAtLeast(16.dp)
        Box(Modifier.align(Alignment.BottomEnd).size(side).clip(CircleShape).background(ContactosColors.Card).border(1.5.dp, ContactosColors.Background, CircleShape),
            contentAlignment = Alignment.Center) {
            if (mark.kind == AccountKind.SIM && mark.slot == null) Icon(Icons.Outlined.SimCard, null, Modifier.size(side * 0.62f), tint = ContactosColors.AccentText)
            else Text(if (mark.kind == AccountKind.SIM) "${mark.slot}" else "G", color = ContactosColors.AccentText, fontSize = (side.value * 0.55f).sp,
                fontWeight = FontWeight.Bold, lineHeight = (side.value * 0.6f).sp)
        }
    }
}
