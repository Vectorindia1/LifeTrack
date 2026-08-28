package com.lifetrack.goal.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lifetrack.R
import com.lifetrack.core.navigation.Destination
import com.lifetrack.core.ui.PlaceholderScreen

@Composable
fun GoalScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        title = stringResource(Destination.Goals.labelRes),
        body = stringResource(R.string.placeholder_goals_body),
        icon = Destination.Goals.icon,
        milestone = 7,
        contentPadding = contentPadding,
        modifier = modifier,
    )
}
