package com.lifetrack.expense.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lifetrack.R
import com.lifetrack.expense.data.ExpenseCategories

/**
 * Quick-add. Amount is focused first because it is the one field that always has to
 * be typed; category is a single chip tap, and the note is optional.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddExpenseSheet(
    knownCategories: List<String>,
    onDismiss: () -> Unit,
    onSave: (amount: Double, category: String, note: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ExpenseCategories.PRESETS.first()) }
    var note by remember { mutableStateOf("") }
    var customMode by remember { mutableStateOf(false) }
    var customCategory by remember { mutableStateOf("") }

    val amount = amountText.replace(',', '.').toDoubleOrNull()
    val effectiveCategory = if (customMode) customCategory else category
    val canSave = amount != null && amount > 0.0 && effectiveCategory.isNotBlank()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.expense_add_title),
                style = MaterialTheme.typography.titleLarge,
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    // Digits and a single separator only — no free text in a number field.
                    if (input.all { it.isDigit() || it == '.' || it == ',' }) amountText = input
                },
                label = { Text(stringResource(R.string.expense_amount_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = stringResource(R.string.expense_category_label),
                style = MaterialTheme.typography.labelLarge,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                knownCategories.forEach { preset ->
                    FilterChip(
                        selected = !customMode && category == preset,
                        onClick = {
                            customMode = false
                            category = preset
                        },
                        label = { Text(preset) },
                    )
                }
                FilterChip(
                    selected = customMode,
                    onClick = { customMode = true },
                    label = { Text(stringResource(R.string.expense_custom_category)) },
                )
            }

            if (customMode) {
                OutlinedTextField(
                    value = customCategory,
                    onValueChange = { customCategory = it },
                    label = { Text(stringResource(R.string.expense_custom_category_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.expense_note_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.expense_cancel)) }
                Button(
                    enabled = canSave,
                    onClick = { onSave(amount!!, effectiveCategory, note.ifBlank { null }) },
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Text(stringResource(R.string.expense_save))
                }
            }
        }
    }
}
