package com.savr.app.ui.screens.grocery

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.FileProvider
import com.example.savr.data.FoodCatalogItem
import com.example.savr.data.getAllFoodCatalogItems
import com.example.savr.data.serializeGroceryItem
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.savr.app.ui.GroceryItem
import com.savr.app.ui.theme.SavrColors
import java.io.File

private enum class AddGroceryMode {
    MANUAL,
    PHOTO
}

private data class GroceryDraftItem(
    val id: Long,
    val item: FoodCatalogItem?,
    val quantity: String
)

private fun createGroceryImageUri(context: Context): Uri {
    val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
    val imageFile = File(imagesDir, "grocery_capture.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}

private fun normalizeReceiptText(value: String): String {
    return value.lowercase()
        .replace(Regex("[^a-z0-9 ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun extractProductCandidate(line: String): String {
    return line
        .replace(Regex("\\b\\d{6,}\\b"), " ")
        .replace(Regex("\\b\\d+\\.\\d{2}\\b"), " ")
        .replace(Regex("\\b(st#|op#|te#|tr#|subtotal|total|credit|change|approval|account|terminal|customer|copy|items sold)\\b", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("[xX]$"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun extractQuantityFromLine(line: String): String {
    return Regex("\\b(\\d+)\\b").find(line)?.groupValues?.get(1) ?: "1"
}

private fun scoreCatalogMatch(line: String, item: FoodCatalogItem): Int {
    val normalizedLine = normalizeReceiptText(extractProductCandidate(line))
    val normalizedName = normalizeReceiptText(item.name)
    if (normalizedLine.isBlank() || normalizedName.isBlank()) return 0
    if (normalizedLine.contains(normalizedName)) return normalizedName.length + 10
    if (normalizedName.contains(normalizedLine) && normalizedLine.length >= 4) return normalizedLine.length + 6

    val nameTokens = normalizedName.split(" ").filter { it.length > 2 }
    val matchedTokens = nameTokens.count { normalizedLine.contains(it) }
    return if (matchedTokens >= 1 && nameTokens.isNotEmpty()) {
        matchedTokens * 10 + normalizedName.length
    } else {
        0
    }
}

private fun buildGroceryDraftsFromText(
    text: String,
    foodCatalogItems: List<FoodCatalogItem>
): List<GroceryDraftItem> {
    val quantityByName = linkedMapOf<String, Pair<FoodCatalogItem, Int>>()

    text.lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .forEach { line ->
            val bestMatch = foodCatalogItems
                .map { item -> item to scoreCatalogMatch(line, item) }
                .maxByOrNull { it.second }

            val matchedItem = bestMatch?.takeIf { it.second > 0 }?.first ?: return@forEach
            val quantity = extractQuantityFromLine(line).toIntOrNull() ?: 1
            val current = quantityByName[matchedItem.name]
            quantityByName[matchedItem.name] = if (current == null) {
                matchedItem to quantity
            } else {
                matchedItem to (current.second + quantity)
            }
        }

    return quantityByName.values.mapIndexed { index, (item, quantity) ->
        GroceryDraftItem(
            id = System.currentTimeMillis() + index,
            item = item,
            quantity = quantity.toString()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGroceryItemSheet(
    onSave: (serializedItems: List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var foodCatalogItems by remember { mutableStateOf<List<FoodCatalogItem>>(emptyList()) }
    var isLoadingItems by remember { mutableStateOf(true) }
    var mode by remember { mutableStateOf(AddGroceryMode.MANUAL) }
    var selectedItem by remember { mutableStateOf<FoodCatalogItem?>(null) }
    var manualSearchQuery by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    val scannedItems = remember { mutableStateListOf<GroceryDraftItem>() }
    var isScanning by remember { mutableStateOf(false) }
    var scanMessage by remember { mutableStateOf<String?>(null) }
    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }

    fun processImage(uri: Uri) {
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (e: Exception) {
            isScanning = false
            scanMessage = "Could not open grocery image."
            return
        }

        textRecognizer.process(image)
            .addOnSuccessListener { result ->
                isScanning = false
                val drafts = buildGroceryDraftsFromText(result.text, foodCatalogItems)
                scannedItems.clear()
                scannedItems.addAll(drafts)
                scanMessage = if (drafts.isEmpty()) {
                    "No grocery items were recognized. You can still add rows manually below."
                } else {
                    null
                }
            }
            .addOnFailureListener {
                isScanning = false
                scanMessage = "Could not scan grocery image. Try again."
            }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingImageUri
        if (!success || uri == null) {
            isScanning = false
            scanMessage = "Photo capture was cancelled."
            return@rememberLauncherForActivityResult
        }
        processImage(uri)
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) {
            isScanning = false
            return@rememberLauncherForActivityResult
        }
        processImage(uri)
    }

    LaunchedEffect(Unit) {
        isLoadingItems = true
        try {
            foodCatalogItems = getAllFoodCatalogItems().sortedBy { it.name.lowercase() }
        } finally {
            isLoadingItems = false
        }
    }

    val canSaveManual = selectedItem != null && quantity.toIntOrNull()?.let { it > 0 } == true
    val canSavePhoto = scannedItems.isNotEmpty() && scannedItems.all {
        it.item != null && it.quantity.toIntOrNull()?.let { qty -> qty > 0 } == true
    }

    ModalBottomSheet(
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = onDismiss,
        containerColor = SavrColors.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDDDDDD))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 36.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (mode == AddGroceryMode.MANUAL) "Add Grocery Item" else "Scan Grocery List",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SavrColors.Dark
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(SavrColors.CreamMid)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", fontSize = 13.sp, color = SavrColors.TextMid)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModeChip(
                    text = "Manual",
                    isActive = mode == AddGroceryMode.MANUAL,
                    onClick = { mode = AddGroceryMode.MANUAL },
                    modifier = Modifier.weight(1f)
                )
                ModeChip(
                    text = "Grocery List",
                    isActive = mode == AddGroceryMode.PHOTO,
                    onClick = { mode = AddGroceryMode.PHOTO },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(14.dp))

            if (mode == AddGroceryMode.MANUAL) {
                SheetLabel("Select Item", Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(6.dp))
                SearchableFoodCatalogPicker(
                    query = manualSearchQuery,
                    onQueryChange = {
                        manualSearchQuery = it
                        if (it.isBlank()) selectedItem = null
                    },
                    selectedItem = selectedItem,
                    items = foodCatalogItems,
                    isLoading = isLoadingItems,
                    placeholder = "Search grocery item",
                    modifier = Modifier.padding(horizontal = 20.dp),
                    onItemSelected = { item ->
                        selectedItem = item
                        manualSearchQuery = item.name
                    }
                )

                Spacer(Modifier.height(14.dp))
                SheetLabel("Quantity", Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(6.dp))
                QuantityEditor(
                    quantity = quantity,
                    onQuantityChange = { quantity = it },
                    unitLabel = selectedItem?.defaultUnit.orEmpty(),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(Modifier.height(20.dp))
                PrimarySheetButton(
                    text = "+ Add to List",
                    enabled = canSaveManual,
                    onClick = {
                        val item = selectedItem
                        val qty = quantity.toIntOrNull()
                        if (item != null && qty != null) {
                            val serialized = serializeGroceryItem(
                                item = GroceryItem(
                                    id = 0,
                                    emoji = item.emoji,
                                    name = item.name,
                                    quantity = listOf(qty.toString(), item.defaultUnit).filter { it.isNotBlank() }.joinToString(" ")
                                ),
                                category = item.category
                            )
                            onSave(listOf(serialized))
                        }
                    }
                )
            } else {
                SheetLabel("Grocery List Scan", Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (!isScanning && !isLoadingItems) SavrColors.Sage
                            else SavrColors.CreamMid
                        )
                        .clickable(enabled = !isScanning && !isLoadingItems) {
                            isScanning = true
                            scanMessage = null
                            val uri = createGroceryImageUri(context)
                            pendingImageUri = uri
                            cameraLauncher.launch(uri)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isScanning) "Scanning..." else "Take Grocery List Picture",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!isScanning && !isLoadingItems) SavrColors.White else SavrColors.TextMuted
                    )
                }

                Spacer(Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (!isScanning && !isLoadingItems) SavrColors.CreamMid
                            else SavrColors.Cream
                        )
                        .clickable(enabled = !isScanning && !isLoadingItems) {
                            isScanning = true
                            scanMessage = null
                            imagePickerLauncher.launch("image/*")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Choose Grocery List Image",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (!isScanning && !isLoadingItems) SavrColors.Dark else SavrColors.TextMuted
                    )
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "We use free on-device text recognition, then match grocery list lines to your Firebase food catalog.",
                    color = SavrColors.TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                if (!scanMessage.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = scanMessage!!,
                        color = SavrColors.Terra,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                scannedItems.forEachIndexed { index, draft ->
                    GroceryDraftRow(
                        draft = draft,
                        foodCatalogItems = foodCatalogItems,
                        isLoadingItems = isLoadingItems,
                        onUpdate = { scannedItems[index] = it },
                        onRemove = { scannedItems.removeAll { item -> item.id == draft.id } }
                    )
                }

                if (scannedItems.isEmpty()) {
                    Text(
                        text = "No scanned items yet. Scan a grocery list to review predicted items and quantities.",
                        color = SavrColors.TextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SavrColors.CreamMid)
                        .clickable {
                            scannedItems.add(
                                GroceryDraftItem(
                                    id = System.currentTimeMillis(),
                                    item = null,
                                    quantity = "1"
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+ Add Item Manually",
                        color = SavrColors.Dark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(16.dp))
                PrimarySheetButton(
                    text = "Save Grocery Items",
                    enabled = canSavePhoto,
                    onClick = {
                        val serializedItems = scannedItems.mapNotNull { draft ->
                            val item = draft.item ?: return@mapNotNull null
                            val qty = draft.quantity.toIntOrNull() ?: return@mapNotNull null
                            serializeGroceryItem(
                                item = GroceryItem(
                                    id = 0,
                                    emoji = item.emoji,
                                    name = item.name,
                                    quantity = listOf(qty.toString(), item.defaultUnit).filter { it.isNotBlank() }.joinToString(" ")
                                ),
                                category = item.category
                            )
                        }
                        onSave(serializedItems)
                    }
                )
            }
        }
    }
}

@Composable
private fun GroceryDraftRow(
    draft: GroceryDraftItem,
    foodCatalogItems: List<FoodCatalogItem>,
    isLoadingItems: Boolean,
    onUpdate: (GroceryDraftItem) -> Unit,
    onRemove: () -> Unit
) {
    var searchQuery by remember(draft.id) { mutableStateOf(draft.item?.name.orEmpty()) }

    LaunchedEffect(draft.item?.name) {
        searchQuery = draft.item?.name.orEmpty()
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SavrColors.White,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Scanned Item", color = SavrColors.TextMid, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text(
                    text = "Remove",
                    color = SavrColors.Terra,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onRemove)
                )
            }
            Spacer(Modifier.height(10.dp))
            SearchableFoodCatalogPicker(
                query = searchQuery,
                onQueryChange = {
                    searchQuery = it
                    if (it.isBlank()) onUpdate(draft.copy(item = null))
                },
                selectedItem = draft.item,
                items = foodCatalogItems,
                isLoading = isLoadingItems,
                placeholder = "Search grocery item",
                onItemSelected = { item ->
                    searchQuery = item.name
                    onUpdate(draft.copy(item = item))
                }
            )
            Spacer(Modifier.height(10.dp))
            QuantityEditor(
                quantity = draft.quantity,
                onQuantityChange = { onUpdate(draft.copy(quantity = it)) },
                unitLabel = draft.item?.defaultUnit.orEmpty()
            )
        }
    }
}

@Composable
private fun SearchableFoodCatalogPicker(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedItem: FoodCatalogItem?,
    items: List<FoodCatalogItem>,
    isLoading: Boolean,
    placeholder: String,
    modifier: Modifier = Modifier,
    onItemSelected: (FoodCatalogItem) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val filteredItems = remember(query, items) {
        items.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                onQueryChange(it)
                expanded = true
            },
            placeholder = { Text(placeholder, color = SavrColors.TextMuted) },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { if (it.isFocused) expanded = true },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SavrColors.Sage,
                unfocusedBorderColor = Color(0xFFE2DDD5),
                focusedContainerColor = SavrColors.White,
                unfocusedContainerColor = SavrColors.Cream,
                cursorColor = SavrColors.Sage
            ),
            shape = RoundedCornerShape(13.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = SavrColors.Dark)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = false),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 320.dp)
                .background(SavrColors.White)
        ) {
            when {
                isLoading -> {
                    Text(
                        text = "Loading items...",
                        color = SavrColors.TextMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
                filteredItems.isEmpty() -> {
                    Text(
                        text = if (selectedItem != null && query == selectedItem.name) "No matching items" else "No matching items",
                        color = SavrColors.TextMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
                else -> {
                    filteredItems.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onItemSelected(item)
                                    expanded = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(item.emoji, fontSize = 18.sp)
                            Text(item.name, color = SavrColors.Dark, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuantityEditor(
    quantity: String,
    onQuantityChange: (String) -> Unit,
    unitLabel: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SavrColors.CreamMid)
                .clickable {
                    val current = quantity.toIntOrNull() ?: 1
                    if (current > 1) onQuantityChange((current - 1).toString())
                },
            contentAlignment = Alignment.Center
        ) {
            Text("-", fontSize = 20.sp, color = SavrColors.Dark, fontWeight = FontWeight.Bold)
        }
        OutlinedTextField(
            value = quantity,
            onValueChange = {
                if (it.isEmpty() || it.toIntOrNull()?.let { qty -> qty > 0 } == true) {
                    onQuantityChange(it)
                }
            },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SavrColors.Sage,
                unfocusedBorderColor = Color(0xFFE2DDD5),
                focusedContainerColor = SavrColors.White,
                unfocusedContainerColor = SavrColors.Cream,
                cursorColor = SavrColors.Sage
            ),
            shape = RoundedCornerShape(13.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = SavrColors.Dark),
            singleLine = true
        )
        Text(
            text = unitLabel,
            color = SavrColors.TextMuted,
            fontSize = 14.sp,
            modifier = Modifier.width(56.dp)
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SavrColors.CreamMid)
                .clickable {
                    val current = quantity.toIntOrNull() ?: 1
                    onQuantityChange((current + 1).toString())
                },
            contentAlignment = Alignment.Center
        ) {
            Text("+", fontSize = 20.sp, color = SavrColors.Dark, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ModeChip(
    text: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) SavrColors.Dark else SavrColors.CreamMid)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isActive) SavrColors.White else SavrColors.TextMid,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SmallActionButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) SavrColors.CreamMid else SavrColors.Cream)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = SavrColors.Dark, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PrimarySheetButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) SavrColors.Dark else SavrColors.CreamMid)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) SavrColors.White else SavrColors.TextMuted
        )
    }
}

@Composable
private fun SheetLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.8.sp,
        color = SavrColors.TextMuted,
        modifier = modifier
    )
}
