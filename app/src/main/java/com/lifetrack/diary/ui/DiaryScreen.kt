package com.lifetrack.diary.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lifetrack.R
import com.lifetrack.core.navigation.Destination
import com.lifetrack.core.ui.PlaceholderScreen

@Composable
fun DiaryScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        title = stringResource(Destination.Diary.labelRes),
        body = stringResource(R.string.placeholder_diary_body),
        icon = Destination.Diary.icon,
        milestone = 8,
        contentPadding = contentPadding,
        modifier = modifier,
    )
}
