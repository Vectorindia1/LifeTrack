package com.lifetrack.settings.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lifetrack.R
import com.lifetrack.core.navigation.Destination
import com.lifetrack.core.ui.PlaceholderScreen

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        title = stringResource(Destination.Settings.labelRes),
        body = stringResource(R.string.placeholder_settings_body),
        icon = Destination.Settings.icon,
        milestone = 10,
        contentPadding = contentPadding,
        modifier = modifier,
    )
}
