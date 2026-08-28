package com.lifetrack.expense.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lifetrack.R
import com.lifetrack.core.navigation.Destination
import com.lifetrack.core.ui.PlaceholderScreen

@Composable
fun ExpenseScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        title = stringResource(Destination.Expenses.labelRes),
        body = stringResource(R.string.placeholder_expenses_body),
        icon = Destination.Expenses.icon,
        milestone = 4,
        contentPadding = contentPadding,
        modifier = modifier,
    )
}
