package com.lifetrack.water.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.style.TextOverflow
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

/**
 * PRD 7.6's quick-add row. These are single taps by design — logging water is the
 * most frequent action in the app, so it must never cost more than one.
 */
@Composable
fun WaterQuickAddRow(
    smallMl: Int,
    largeMl: Int,
    onAdd: (Int) -> Unit,
    onCustom: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * PRD 7.6 only asks for the two quick-add buttons on the dashboard, where space
     * is tight; the full Water screen keeps Custom. Without this, three elements
     * fighting for a narrow row starved the buttons of width — with no line limit on
     * the label, that made a Button wrap "+250 ml" one character per line and
     * balloon into a huge vertical pill instead of failing gracefully.
     */
    showCustom: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(onClick = { onAdd(smallMl) }, modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.water_add_amount, smallMl),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Button(onClick = { onAdd(largeMl) }, modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.water_add_amount, largeMl),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showCustom) {
            OutlinedButton(onClick = onCustom) {
                Text(stringResource(R.string.water_add_custom), maxLines = 1)
            }
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
