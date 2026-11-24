package com.zirakusa.contactos.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.telefono.R
import com.zirakusa.contactos.data.Contact
import com.zirakusa.contactos.data.Contacts
import com.zirakusa.contactos.data.Phones
import com.zirakusa.contactos.ui.ContactosState
import com.zirakusa.contactos.ui.Page
import com.zirakusa.contactos.ui.theme.ContactosColors
import java.time.temporal.ChronoUnit

/** «Se encontraron contactos repetidos»: «Combinar» opens them; ✕ hides it until there are new ones. */
@Composable fun DupesBanner(state: ContactosState) = Group(Modifier.padding(vertical = 6.dp)) {
    Row(Modifier.padding(start = 20.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.c_dupes_banner), Modifier.weight(1f), fontSize = 14.sp)
        TextButton({ state.dupesBack = Page.LIST; state.page = Page.DUPLICATES }) { Text(stringResource(R.string.c_merge), color = ContactosColors.AccentText) }
        RoundButton(Icons.Outlined.Close, stringResource(R.string.c_close), size = 36.dp, onClick = state::closeBanner)
    }
}

@Composable fun Favorites(state: ContactosState, favorites: List<Contact>) = Column {
    Caption(stringResource(R.string.c_favorites))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
        items(favorites, key = { it.id }) { c ->
            Column(Modifier.width(64.dp).clickable { state.open(c) }, horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(c, 56.dp)
                Text(c.shownName.substringBefore(' '), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable fun Birthdays(state: ContactosState, birthdays: List<Pair<Contact, java.time.LocalDate>>) = Group(Modifier.padding(vertical = 8.dp)) {
    Caption(stringResource(R.string.c_birthdays))
    birthdays.take(3).forEach { (c, d) ->
        val days = ChronoUnit.DAYS.between(state.today(), d).toInt()
        Item(state.name(c), when (days) { 0 -> stringResource(R.string.c_today); 1 -> stringResource(R.string.c_tomorrow)
            else -> pluralStringResource(R.plurals.c_in_days, days, days) } + (c.birthday?.turns(state.today())?.let { " · " + stringResource(R.string.c_turns, it) } ?: ""),
            onClick = { state.open(c) })
    }
}

/** One person in the list: picture (with its SIM or Google mark) and name; the number too while searching. */
@Composable fun ContactRow(c: Contact, state: ContactosState, caption: String? = null) = Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
    .clickable { state.open(c) }.padding(vertical = 8.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
    Avatar(c)
    Column(Modifier.padding(start = 14.dp)) {
        Text(caption ?: state.name(c), fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        val under = if (caption != null) state.name(c) else if (state.query.isNotBlank()) c.phones.firstOrNull()?.let { Phones.pretty(it.number) } else null
        if (under != null) Text(under, color = ContactosColors.Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** The alphabet on the right (A–Z, Ñ, #): tap or slide a finger; a letter without people goes to the next one. */
@Composable fun LetterBar(present: List<Char>, modifier: Modifier = Modifier, onLetter: (Char) -> Unit) {
    var height by remember { mutableIntStateOf(1) }
    fun go(y: Float) {
        val i = (y / height * Contacts.INDEX.size).toInt().coerceIn(0, Contacts.INDEX.size - 1)
        Contacts.jump(present, Contacts.INDEX[i])?.let(onLetter)
    }
    Column(modifier.onSizeChanged { height = it.height.coerceAtLeast(1) }.pointerInput(present) { detectVerticalDragGestures { change, _ -> go(change.position.y) } },
        horizontalAlignment = Alignment.CenterHorizontally) {
        Contacts.INDEX.forEach { l ->
            Text("$l", Modifier.clickable(onClickLabel = "$l") { Contacts.jump(present, l)?.let(onLetter) }.padding(horizontal = 6.dp, vertical = 1.dp),
                color = if (l in present) ContactosColors.AccentText else ContactosColors.Muted, fontSize = 11.sp, lineHeight = 13.sp)
        }
    }
}
