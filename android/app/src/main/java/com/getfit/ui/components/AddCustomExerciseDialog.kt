package com.getfit.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomExerciseDialog(
    initialName: String = "",
    initialCategory: String = "",
    initialEquipment: String = "Dumbbell",
    initialIsCompound: Boolean = false,
    initialDescription: String = "",
    title: String = "Add Custom Exercise",
    confirmButtonText: String = "Add Exercise",
    existingCategories: List<String> = emptyList(),
    existingExerciseNames: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (name: String, category: String, equipment: String, isCompound: Boolean, description: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var customCategoryInput by remember { mutableStateOf("") }
    var isAddingCustomCategory by remember { mutableStateOf(false) }
    var selectedEquipment by remember { mutableStateOf(initialEquipment) }
    var isCompound by remember { mutableStateOf(initialIsCompound) }
    var description by remember { mutableStateOf(initialDescription) }

    // Standard popular categories + any categories already in user's DB
    val defaultCategories = listOf(
        "Chest", "Back", "Legs", "Shoulders", "Arms", "Biceps", "Triceps", "Forearms", "Core", "Calves", "Traps", "Neck", "Full Body"
    )
    val allCategoryOptions = remember(existingCategories) {
        val set = linkedSetOf<String>()
        set.addAll(defaultCategories)
        existingCategories.forEach { if (it != "All" && it.isNotBlank()) set.add(it) }
        set.toList()
    }

    val activeCategory = if (isAddingCustomCategory) customCategoryInput.trim() else selectedCategory

    // Duplicate detection
    val isDuplicateName = remember(name, existingExerciseNames, initialName) {
        val trimmed = name.trim()
        trimmed.isNotBlank() &&
            !trimmed.equals(initialName.trim(), ignoreCase = true) &&
            existingExerciseNames.any { it.trim().equals(trimmed, ignoreCase = true) }
    }

    val isDuplicateCategory = remember(customCategoryInput, existingCategories) {
        val trimmed = customCategoryInput.trim()
        trimmed.isNotBlank() && existingCategories.any { it.trim().equals(trimmed, ignoreCase = true) }
    }

    val canSave = name.isNotBlank() && activeCategory.isNotBlank() && !isDuplicateName

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .padding(vertical = 16.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(title, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Exercise Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exercise Name *") },
                    placeholder = { Text("e.g. Wrist Curls, Farmer's Walk") },
                    isError = isDuplicateName,
                    supportingText = {
                        if (isDuplicateName) {
                            Text(
                                text = "An exercise named \"${name.trim()}\" already exists.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Backspace) {
                                if (name.isNotEmpty()) {
                                    name = name.dropLast(1)
                                    return@onPreviewKeyEvent true
                                }
                            }
                            false
                        },
                    shape = RoundedCornerShape(12.dp)
                )

                // Category Section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "BODY CATEGORY *",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Quick category chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(allCategoryOptions) { cat ->
                            val isSelected = !isAddingCustomCategory && selectedCategory.equals(cat, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    isAddingCustomCategory = false
                                    selectedCategory = cat
                                },
                                label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    // Option to enter a brand new custom category like Forearms, Neck, Calves, etc.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp)
                    ) {
                        Text(
                            text = if (isAddingCustomCategory) "Custom category mode:" else "Need a custom category?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!isAddingCustomCategory) {
                            TextButton(onClick = {
                                isAddingCustomCategory = true
                                selectedCategory = ""
                            }) {
                                Text("+ Type Custom Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    if (isAddingCustomCategory) {
                        OutlinedTextField(
                            value = customCategoryInput,
                            onValueChange = { customCategoryInput = it },
                            label = { Text("Custom Category Name *") },
                            placeholder = { Text("e.g. Forearms, Neck, Glutes, Calves") },
                            isError = isDuplicateCategory,
                            supportingText = {
                                if (isDuplicateCategory) {
                                    Text(
                                        text = "Category \"${customCategoryInput.trim()}\" already exists in the list above.",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = {
                                    isAddingCustomCategory = false
                                    customCategoryInput = ""
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel custom category",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onPreviewKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Backspace) {
                                        if (customCategoryInput.isNotEmpty()) {
                                            customCategoryInput = customCategoryInput.dropLast(1)
                                            return@onPreviewKeyEvent true
                                        }
                                    }
                                    false
                                },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Equipment Section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "EQUIPMENT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    val equipments = listOf("Dumbbell", "Barbell", "Cable", "Machine", "Bodyweight", "Other")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(equipments) { equip ->
                            FilterChip(
                                selected = selectedEquipment == equip,
                                onClick = { selectedEquipment = equip },
                                label = { Text(equip, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                // Description / Notes
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Notes / Form Tips (Optional)") },
                    placeholder = { Text("e.g. Focus on forearm contraction and wrist flexion") },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Backspace) {
                                if (description.isNotEmpty()) {
                                    description = description.dropLast(1)
                                    return@onPreviewKeyEvent true
                                }
                            }
                            false
                        },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (canSave) {
                        onConfirm(
                            name.trim(),
                            activeCategory,
                            selectedEquipment,
                            isCompound,
                            description.trim()
                        )
                    }
                },
                enabled = canSave,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(confirmButtonText, fontWeight = FontWeight.Bold)
            }
        }
    )
}
