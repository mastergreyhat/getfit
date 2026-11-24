package com.getfit.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getfit.ui.viewmodel.FoodViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddCustomFoodScreen(
    viewModel: FoodViewModel,
    onNavigateBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Custom") }
    var caloriesText by remember { mutableStateOf("") }
    var proteinText by remember { mutableStateOf("") }
    var carbsText by remember { mutableStateOf("") }
    var fatText by remember { mutableStateOf("") }
    var fiberText by remember { mutableStateOf("0") }
    
    // Strict unit options: "Per 100g", "Per Gram", "Per Piece"
    val unitOptions = listOf("Per 100g", "Per Gram", "Per Piece")
    var selectedUnitOption by remember { mutableStateOf("Per 100g") }
    var servingSizeText by remember { mutableStateOf("100") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val categories = listOf(
        "Custom", "Indian Breads", "Rice & Grains", "Lentils & Dal",
        "Non-Veg", "Vegetables", "Dairy", "Protein Supplements",
        "Fruits", "Snacks", "Beverages"
    )

    // Dynamic nutritional info section label in braces based strictly on chosen unit
    val dynamicNutritionalBrace = when (selectedUnitOption) {
        "Per 100g" -> "per 100g"
        "Per Gram" -> "per gram"
        "Per Piece" -> "per piece"
        else -> "per 100g"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Add Custom Food", 
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Food Name *", maxLines = 1, softWrap = false) },
                placeholder = { Text("e.g. Homemade Paneer Bhurji", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Category selector
            Text(
                text = "Category", 
                style = MaterialTheme.typography.titleSmall, 
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat, maxLines = 1, softWrap = false) }
                    )
                }
            }

            // Measurement Unit selector (Strictly: Per 100g, Per Gram, or Per Piece)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Measurement Unit *",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    unitOptions.forEach { option ->
                        val isSelected = selectedUnitOption == option
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedUnitOption = option
                                when (option) {
                                    "Per 100g" -> servingSizeText = "100"
                                    "Per Gram" -> servingSizeText = "1"
                                    "Per Piece" -> servingSizeText = "1"
                                }
                            },
                            label = {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = option,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Serving Size description / input
            OutlinedTextField(
                value = servingSizeText,
                onValueChange = { servingSizeText = it },
                label = {
                    Text(
                        text = when (selectedUnitOption) {
                            "Per Piece" -> "Base Serving Quantity (pieces)"
                            else -> "Base Serving Size (grams)"
                        },
                        maxLines = 1,
                        softWrap = false
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp)
            )

            // Dynamic header based on chosen unit
            Text(
                text = "Nutritional Info ($dynamicNutritionalBrace)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false
            )

            OutlinedTextField(
                value = caloriesText,
                onValueChange = { caloriesText = it },
                label = { Text("Calories (kcal) *", maxLines = 1, softWrap = false) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = proteinText,
                    onValueChange = { proteinText = it },
                    label = { Text("Protein (g) *", maxLines = 1, softWrap = false) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = carbsText,
                    onValueChange = { carbsText = it },
                    label = { Text("Carbs (g) *", maxLines = 1, softWrap = false) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = fatText,
                    onValueChange = { fatText = it },
                    label = { Text("Fat (g) *", maxLines = 1, softWrap = false) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = fiberText,
                    onValueChange = { fiberText = it },
                    label = { Text("Fiber (g)", maxLines = 1, softWrap = false) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val cal = caloriesText.toDoubleOrNull()
                    val pro = proteinText.toDoubleOrNull()
                    val carb = carbsText.toDoubleOrNull()
                    val fat = fatText.toDoubleOrNull()
                    val fib = fiberText.toDoubleOrNull() ?: 0.0
                    val sSize = servingSizeText.toDoubleOrNull() ?: when (selectedUnitOption) {
                        "Per 100g" -> 100.0
                        "Per Gram" -> 1.0
                        "Per Piece" -> 100.0
                        else -> 100.0
                    }

                    if (name.isBlank() || cal == null || pro == null || carb == null || fat == null) {
                        errorMessage = "Please fill in all required fields marked with *"
                    } else {
                        // Standardize storage based on chosen unit:
                        // 1. "Per 100g": stored directly as per 100g, default serving size = sSize (100g), unit = "g"
                        // 2. "Per Gram": values entered per 1g -> multiplied by 100 for per 100g storage, default serving size = 1.0, unit = "g"
                        // 3. "Per Piece": values entered per 1 piece -> stored as caloriesPer100g = cal, default serving size = 100.0, unit = "piece"
                        //    (in FoodViewModel: scale = (quantity * 100) / 100 = quantity, giving exact calories = quantity * cal)
                        val cal100g: Double
                        val pro100g: Double
                        val carb100g: Double
                        val fat100g: Double
                        val fib100g: Double
                        val finalServingSize: Double
                        val finalUnit: String

                        when (selectedUnitOption) {
                            "Per Gram" -> {
                                cal100g = cal * 100.0
                                pro100g = pro * 100.0
                                carb100g = carb * 100.0
                                fat100g = fat * 100.0
                                fib100g = fib * 100.0
                                finalServingSize = 1.0
                                finalUnit = "g"
                            }
                            "Per Piece" -> {
                                cal100g = cal
                                pro100g = pro
                                carb100g = carb
                                fat100g = fat
                                fib100g = fib
                                finalServingSize = 100.0
                                finalUnit = "piece"
                            }
                            else -> { // "Per 100g"
                                cal100g = cal
                                pro100g = pro
                                carb100g = carb
                                fat100g = fat
                                fib100g = fib
                                finalServingSize = sSize.coerceAtLeast(1.0)
                                finalUnit = "g"
                            }
                        }

                        viewModel.addCustomFood(
                            name = name.trim(),
                            category = category,
                            caloriesPer100g = cal100g,
                            proteinPer100g = pro100g,
                            carbsPer100g = carb100g,
                            fatPer100g = fat100g,
                            fiberPer100g = fib100g,
                            servingSize = finalServingSize,
                            servingUnit = finalUnit
                        )
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save Custom Food", 
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
