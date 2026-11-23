package com.zirakusa.telefono.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.telefono.R
import com.zirakusa.telefono.ui.theme.TelefonoColors

@Composable fun PageTop(title: String, onBack: () -> Unit) = Row(Modifier.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
    RoundButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back), size = 52.dp, onClick = onBack)
    Text(title, Modifier.padding(start = 16.dp), fontSize = 22.sp)
}

@Composable fun Divider() = HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = TelefonoColors.Chip)
