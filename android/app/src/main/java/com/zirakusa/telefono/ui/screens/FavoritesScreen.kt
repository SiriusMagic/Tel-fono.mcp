package com.zirakusa.telefono.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.telefono.R
import com.zirakusa.telefono.ui.AppActions
import com.zirakusa.telefono.ui.TelefonoState
import com.zirakusa.telefono.ui.theme.TelefonoColors

/** «Favoritos»: the contacts with a star (marked in Contactos), big; one tap calls. */
@Composable fun FavoritesScreen(state: TelefonoState, actions: AppActions) = Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp)) {
    Text(stringResource(R.string.tab_favorites), Modifier.padding(top = 20.dp, start = 4.dp, bottom = 12.dp), fontSize = 26.sp)
    val favs = state.favorites
    if (favs.isEmpty()) Text(stringResource(R.string.no_favorites), Modifier.padding(16.dp), color = TelefonoColors.Muted)
    LazyVerticalGrid(GridCells.Fixed(3), contentPadding = PaddingValues(bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(favs, key = { it.id }) { p ->
            Column(Modifier.clip(CircleShape).clickable(onClickLabel = stringResource(R.string.call)) { actions.call(p.numbers.first()) }.padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(76.dp).clip(CircleShape).background(TelefonoColors.Accent.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Text(p.name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1).uppercase() }, color = TelefonoColors.AccentText,
                        fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(p.name, Modifier.padding(top = 6.dp), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            }
        }
    }
}
