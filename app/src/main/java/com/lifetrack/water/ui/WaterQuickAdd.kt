package com.lifetrack.water.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lifetrack.R
import com.lifetrack.water.viewmodel.WaterViewModel

/**
 * PRD 7.6's quick-add row. These are single taps by design — logging water is the
 * most frequent action in the app, so it must never cost more than one.
 */
@Composable
fun WaterQuickAddRow(
    onAdd: (Int) -> Unit,
    onCustom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = { onAdd(WaterViewModel.QUICK_SMALL_ML) },
            modifier = Modifier.weight(1f),
        ) {
            Text(stringResource(R.string.water_add_small))
        }
        Button(
            onClick = { onAdd(WaterViewModel.QUICK_LARGE_ML) },
            modifier = Modifier.weight(1f),
        ) {
            Text(stringResource(R.string.water_add_large))
        }
        OutlinedButton(onClick = onCustom) {
            Text(stringResource(R.string.water_add_custom))
        }
    }
}

@Composable
fun CustomWaterDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val parsed = text.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.water_custom_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { input -> if (input.all { it.isDigit() }) text = input },
                label = { Text(stringResource(R.string.water_custom_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = {
            TextButton(enabled = parsed != null && parsed > 0, onClick = { onConfirm(parsed!!) }) {
                Text(stringResource(R.string.water_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.water_cancel)) }
        },
    )
}

@Composable
fun WaterTargetDialog(
    current: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var text by remember { mutableStateOf(current.toString()) }
    val parsed = text.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.water_target_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { input -> if (input.all { it.isDigit() }) text = input },
                label = { Text(stringResource(R.string.water_target_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = {
            TextButton(enabled = parsed != null && parsed > 0, onClick = { onConfirm(parsed!!) }) {
                Text(stringResource(R.string.water_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.water_cancel)) }
        },
    )
}
