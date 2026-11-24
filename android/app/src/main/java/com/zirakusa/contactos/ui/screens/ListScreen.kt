package com.zirakusa.contactos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.telefono.R
import com.zirakusa.contactos.ui.AppActions
import com.zirakusa.contactos.ui.ContactosState
import com.zirakusa.contactos.ui.Page
import com.zirakusa.contactos.ui.theme.ContactosColors
import kotlinx.coroutines.launch

/**
 * «Contactos»: the count, search, the duplicates banner, «Mi perfil», favourites, birthdays soon, and everyone A–Z with
 * the alphabet on the right to jump. «+» adds one.
 */
@Composable fun ListScreen(state: ContactosState, actions: AppActions) = Box(Modifier.fillMaxSize().background(ContactosColors.Background)) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text(stringResource(R.string.c_contacts_title), fontSize = 30.sp)
                if (state.allowed) Text(pluralStringResource(R.plurals.c_contacts_n, state.visible.size, state.visible.size), color = ContactosColors.Muted, fontSize = 14.sp)
            }
            RoundButton(Icons.Outlined.Settings, stringResource(R.string.c_settings)) { state.page = Page.SETTINGS }
        }
        if (!state.allowed) return@Column Group(Modifier.padding(16.dp)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Contacts, null, Modifier.size(48.dp), tint = ContactosColors.AccentText)
                Text(stringResource(R.string.c_allow_text), color = ContactosColors.Muted, textAlign = TextAlign.Center)
                MainButton(stringResource(R.string.c_allow), onClick = actions.allow)
            }
        }
        OutlinedTextField(state.query, { state.query = it.take(100) }, Modifier.fillMaxWidth().padding(horizontal = 16.dp), singleLine = true, shape = CircleShape,
            leadingIcon = { Icon(Icons.Outlined.Search, null) }, placeholder = { Text(pluralStringResource(R.plurals.c_search_n, state.visible.size, state.visible.size)) })
        if (state.query.isNotBlank()) {
            val found = state.results
            LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 100.dp)) {
                if (found.isEmpty()) item { Text(stringResource(R.string.c_nothing_found), Modifier.padding(16.dp), color = ContactosColors.Muted) }
                items(found, key = { it.id }) { ContactRow(it, state) }
            }
            return@Column
        }
        Box(Modifier.weight(1f)) {
            val list = rememberLazyListState()
            val groups = state.groups
            val favorites = state.favorites
            val birthdays = state.birthdays
            val banner = state.dupesBanner
            val me = state.profile
            val head = listOf(banner, me != null, favorites.isNotEmpty(), birthdays.isNotEmpty()).count { it }
            LazyColumn(state = list, contentPadding = PaddingValues(start = 16.dp, end = 36.dp, bottom = 100.dp)) {
                if (banner) item(key = "banner") { DupesBanner(state) }
                if (me != null) item(key = "me") { ContactRow(me, state, caption = stringResource(R.string.c_my_profile)) }
                if (favorites.isNotEmpty()) item(key = "favorites") { Favorites(state, favorites) }
                if (birthdays.isNotEmpty()) item(key = "birthdays") { Birthdays(state, birthdays) }
                groups.forEach { (letter, people) ->
                    item(key = "h$letter") { Text("$letter", Modifier.padding(start = 8.dp, top = 14.dp, bottom = 4.dp), color = ContactosColors.AccentText, fontSize = 16.sp) }
                    items(people, key = { it.id }) { ContactRow(it, state) }
                }
                if (groups.isEmpty()) item { Text(stringResource(R.string.c_empty), Modifier.padding(24.dp), color = ContactosColors.Muted) }
            }
            val scope = rememberCoroutineScope()
            var index = head
            val starts = groups.associate { (l, p) -> (l to index).also { index += 1 + p.size } }
            if (groups.isNotEmpty()) LetterBar(groups.map { it.first }, Modifier.align(Alignment.CenterEnd).padding(end = 2.dp)) { l ->
                starts[l]?.let { at -> scope.launch { list.scrollToItem(at) } }
            }
        }
    }
    if (state.allowed) Box(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp).size(60.dp).clip(RoundedCornerShape(30.dp))
        .background(ContactosColors.Accent).clickable(onClickLabel = stringResource(R.string.c_new_contact)) { state.edit() }, contentAlignment = Alignment.Center) {
        Icon(Icons.Outlined.Add, stringResource(R.string.c_new_contact), tint = ContactosColors.OnAccent)
    }
}
