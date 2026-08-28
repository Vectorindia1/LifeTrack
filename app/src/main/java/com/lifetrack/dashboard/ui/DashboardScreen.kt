package com.lifetrack.dashboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifetrack.R
import com.lifetrack.core.navigation.Destination
import com.lifetrack.core.ui.AppViewModelProvider
import com.lifetrack.core.ui.theme.LifeTrackTheme
import com.lifetrack.dashboard.viewmodel.DashboardUiState
import com.lifetrack.dashboard.viewmodel.DashboardViewModel

/**
 * Milestone-1 dashboard: a database status card plus a way into every tracker.
 * PRD 7.1's real at-a-glance dashboard is milestone 3.
 */
@Composable
fun DashboardScreen(
    contentPadding: PaddingValues,
    onOpen: (Destination) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardContent(
        uiState = uiState,
        onOpen = onOpen,
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    onOpen: (Destination) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
            start = 16.dp,
            end = 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.dashboard_title),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        item {
            DatabaseStatusCard(uiState)
        }
        items(Destination.entries.filter { it != Destination.Dashboard && it != Destination.Settings }) { destination ->
            TrackerCard(destination = destination, onClick = { onOpen(destination) })
        }
    }
}

@Composable
private fun DatabaseStatusCard(uiState: DashboardUiState, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.db_status_title),
                style = MaterialTheme.typography.titleMedium,
            )
            if (uiState.isLoading) {
                Text(
                    text = stringResource(R.string.db_status_loading),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(
                    text = stringResource(R.string.db_status_ready, TABLE_COUNT),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(
                        R.string.db_status_detail,
                        uiState.habitCount,
                        uiState.goalCount,
                        uiState.expenseCount,
                        uiState.diaryCount,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(
                        R.string.db_status_targets,
                        uiState.calorieTarget,
                        uiState.waterTargetMl,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TrackerCard(
    destination: Destination,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = destination.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(destination.labelRes),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.dashboard_open),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** Entities declared in LifeTrackDatabase. Kept here only for the milestone-1 status card. */
private const val TABLE_COUNT = 10

@Preview(showBackground = true)
@Composable
private fun DashboardPreview() {
    LifeTrackTheme {
        DashboardContent(
            uiState = DashboardUiState(
                isLoading = false,
                calorieTarget = 2000,
                waterTargetMl = 2500,
                reminderCount = 6,
            ),
            onOpen = {},
            contentPadding = PaddingValues(0.dp),
        )
    }
}
