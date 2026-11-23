package com.getfit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.getfit.data.entity.FoodItem
import com.getfit.ui.theme.CaloriesOrange
import com.getfit.ui.theme.CarbsAmber
import com.getfit.ui.theme.FatRed
import com.getfit.ui.theme.ProteinBlue

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FoodDetailDialog(
    foodItem: FoodItem,
    isOnlineFood: Boolean = false,
    defaultMealType: String = "Breakfast",
    onDismiss: () -> Unit,
    onLogFood: (foodItem: FoodItem, quantity: Double, mealType: String) -> Unit
) {
    var quantityText by remember { mutableStateOf("1.0") }
    var quantity by remember { mutableDoubleStateOf(1.0) }
    var selectedMealType by remember { mutableStateOf(defaultMealType) }
    val mealTypes = listOf("Breakfast", "Lunch", "Dinner", "Snacks", "Pre-Workout", "Post-Workout")

    // Update numeric quantity safely
    val currentQty = quantityText.toDoubleOrNull() ?: quantity
    val isPiece = foodItem.servingUnit.equals("piece", ignoreCase = true) || foodItem.servingUnit.equals("scoop", ignoreCase = true)

    val ratio = if (isPiece) currentQty else (currentQty * foodItem.defaultServingSize) / 100.0

    val calcCalories = (foodItem.caloriesPer100g * ratio).toInt()
    val calcProtein = String.format(java.util.Locale.US, "%.1f", foodItem.proteinPer100g * ratio)
    val calcCarbs = String.format(java.util.Locale.US, "%.1f", foodItem.carbsPer100g * ratio)
    val calcFat = String.format(java.util.Locale.US, "%.1f", foodItem.fatPer100g * ratio)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = foodItem.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f, fill = false),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isOnlineFood) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Text(
                                            text = "Online",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            text = if (isPiece) {
                                val unitLabel = if (foodItem.name.contains("scoop", ignoreCase = true) || foodItem.servingUnit.equals("scoop", ignoreCase = true)) "1 scoop" else "1 piece"
                                "${foodItem.category} • Base serving: $unitLabel"
                            } else {
                                "${foodItem.category} • Base serving: ${foodItem.defaultServingSize.toInt()}g"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Calculated Nutrition Cards
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isPiece) {
                                    val qtyFormatted = if (currentQty % 1.0 == 0.0) currentQty.toInt().toString() else currentQty.toString()
                                    val unitName = if (foodItem.name.contains("scoop", ignoreCase = true) || foodItem.servingUnit.equals("scoop", ignoreCase = true)) "scoop" else "piece"
                                    "Total Nutrition ($qtyFormatted $unitName${if (currentQty > 1.0) "s" else ""})"
                                } else {
                                    val g = (currentQty * foodItem.defaultServingSize).toInt()
                                    "Total Nutrition (${g}g)"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "$calcCalories kcal",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                color = CaloriesOrange
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            MacroBadge("Protein", "${calcProtein}g", ProteinBlue)
                            MacroBadge("Carbs", "${calcCarbs}g", CarbsAmber)
                            MacroBadge("Fat", "${calcFat}g", FatRed)
                        }
                    }
                }

                // Quantity Selector
                Text(
                    text = if (isPiece) {
                        if (foodItem.name.contains("scoop", ignoreCase = true) || foodItem.servingUnit.equals("scoop", ignoreCase = true)) "Number of Scoops" else "Number of Pieces"
                    } else "Number of Servings",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilledTonalIconButton(
                        onClick = {
                            val next = ((currentQty - 0.5) * 10).toInt() / 10.0
                            if (next >= 0.5) {
                                quantity = next
                                quantityText = next.toString()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                    }

                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = {
                            quantityText = it
                            it.toDoubleOrNull()?.let { v -> quantity = v }
                        },
                        modifier = Modifier
                            .width(100.dp)
                            .padding(horizontal = 8.dp),
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    FilledTonalIconButton(
                        onClick = {
                            val next = ((currentQty + 0.5) * 10).toInt() / 10.0
                            quantity = next
                            quantityText = next.toString()
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase")
                    }
                }

                Slider(
                    value = currentQty.toFloat().coerceIn(0.5f, 5f),
                    onValueChange = {
                        val rounded = (it * 2).toInt() / 2f
                        quantity = rounded.toDouble()
                        quantityText = rounded.toString()
                    },
                    valueRange = 0.5f..5f,
                    steps = 8
                )

                // Meal Type Selection
                Text(
                    text = "Meal Type",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    mealTypes.forEach { type ->
                        FilterChip(
                            selected = selectedMealType == type,
                            onClick = { selectedMealType = type },
                            label = { Text(type) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Action Buttons
                Button(
                    onClick = {
                        val finalQty = quantityText.toDoubleOrNull() ?: 1.0
                        onLogFood(foodItem, finalQty, selectedMealType)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (isOnlineFood) "Add to My Foods & Log ($calcCalories kcal)"
                        else "Log Food ($calcCalories kcal)"
                    )
                }
            }
        }
    }
}

@Composable
private fun MacroBadge(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
