package com.lifetrack.calorie.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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

/** Manual entry only — PRD 3 rules out food recognition and scanning for v1. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCalorieSheet(
    onDismiss: () -> Unit,
    onSave: (foodName: String, calories: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var food by remember { mutableStateOf("") }
    var caloriesText by remember { mutableStateOf("") }

    val calories = caloriesText.toIntOrNull()
    val canSave = calories != null && calories > 0

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.calorie_add_title),
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(
                value = food,
                onValueChange = { food = it },
                label = { Text(stringResource(R.string.calorie_food_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = caloriesText,
                onValueChange = { input -> if (input.all { it.isDigit() }) caloriesText = input },
                label = { Text(stringResource(R.string.calorie_amount_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.calorie_cancel)) }
                Button(
                    enabled = canSave,
                    onClick = { onSave(food, calories!!) },
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Text(stringResource(R.string.calorie_save))
                }
            }
        }
    }
}
