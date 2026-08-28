package com.lifetrack.calorie.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lifetrack.R
import com.lifetrack.core.navigation.Destination
import com.lifetrack.core.ui.PlaceholderScreen

@Composable
fun CalorieScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        title = stringResource(Destination.Calories.labelRes),
        body = stringResource(R.string.placeholder_calories_body),
        icon = Destination.Calories.icon,
        milestone = 5,
        contentPadding = contentPadding,
        modifier = modifier,
    )
}
