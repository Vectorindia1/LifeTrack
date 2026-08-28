package com.lifetrack.water.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lifetrack.R
import com.lifetrack.core.navigation.Destination
import com.lifetrack.core.ui.PlaceholderScreen

@Composable
fun WaterScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        title = stringResource(Destination.Water.labelRes),
        body = stringResource(R.string.placeholder_water_body),
        icon = Destination.Water.icon,
        milestone = 6,
        contentPadding = contentPadding,
        modifier = modifier,
    )
}
