package com.savr.app.ui.screens.CurrentInventory

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.example.savr.data.FoodCatalogItem
import com.example.savr.data.calculateExpiryStatus
import com.example.savr.data.getAllFoodCatalogItems
import com.example.savr.data.serializeInventoryItem
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.savr.app.ui.CurrentInventoryCategory
import com.savr.app.ui.CurrentInventoryItem
import com.savr.app.ui.theme.SavrColors
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.ZoneId

private enum class AddInventoryMode {
    MANUAL,
    RECEIPT
}

private data class ReceiptDraftItem(
    val id: Long,
    val item: FoodCatalogItem?,
    val quantity: String,
    val expiryDate: String
)

private fun mapCategoryToInventoryCategory(category: String): CurrentInventoryCategory {
    return when (category.lowercase()) {
        "fruit", "fruits" -> CurrentInventoryCategory.FRUIT
        "vegetables", "vegetable", "produce" -> CurrentInventoryCategory.VEG
        "dairy", "dairy & eggs" -> CurrentInventoryCategory.DAIRY
        "protein", "meat" -> CurrentInventoryCategory.PROTEIN
        "grain", "grains", "pantry", "baking" -> CurrentInventoryCategory.GRAIN
        else -> CurrentInventoryCategory.OTHER
    }
}

private val inventoryDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

private fun defaultReceiptExpiryDate(): String {
    return LocalDate.now()
        .plusDays(7)
        .format(inventoryDateFormatter)
}

private fun parseInventoryDateOrNull(value: String): LocalDate? {
    return try {
        LocalDate.parse(value.trim(), inventoryDateFormatter)
    } catch (_: Exception) {
        null
    }
}

private fun LocalDate.toPickerMillis(): Long {
    return atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private fun pickerMillisToDateString(value: Long): String {
    return Instant.ofEpochMilli(value)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(inventoryDateFormatter)
}

private fun createReceiptImageUri(context: Context): Uri {
    val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
    val imageFile = File(imagesDir, "receipt_capture.jpg")
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

private fun buildReceiptDraftsFromText(
    receiptText: String,
    foodCatalogItems: List<FoodCatalogItem>
): List<ReceiptDraftItem> {
    val quantityByName = linkedMapOf<String, Pair<FoodCatalogItem, Int>>()

    receiptText.lines()
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
        ReceiptDraftItem(
            id = System.currentTimeMillis() + index,
            item = item,
            quantity = quantity.toString(),
            expiryDate = defaultReceiptExpiryDate()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCurrentInventoryItemSheet(
    onSave: (serializedItems: List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var foodCatalogItems by remember { mutableStateOf<List<FoodCatalogItem>>(emptyList()) }
    var isLoadingItems by remember { mutableStateOf(true) }
    var mode by remember { mutableStateOf(AddInventoryMode.MANUAL) }

    var selectedItem by remember { mutableStateOf<FoodCatalogItem?>(null) }
    var manualSearchQuery by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var expirationDate by remember { mutableStateOf(defaultReceiptExpiryDate()) }

    val scannedItems = remember { mutableStateListOf<ReceiptDraftItem>() }
    var isScanningReceipt by remember { mutableStateOf(false) }
    var receiptMessage by remember { mutableStateOf<String?>(null) }

    val textRecognizer = remember {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    var pendingReceiptUri by remember { mutableStateOf<Uri?>(null) }

    fun processReceiptUri(uri: Uri) {
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (e: Exception) {
            isScanningReceipt = false
            receiptMessage = "Could not open receipt image."
            android.util.Log.e("AddInventoryItemSheet", "Failed to create InputImage from uri", e)
            return
        }

        textRecognizer.process(image)
            .addOnSuccessListener { result ->
                isScanningReceipt = false
                android.util.Log.d("AddInventoryItemSheet", "Receipt OCR text:\n${result.text}")
                val drafts = buildReceiptDraftsFromText(result.text, foodCatalogItems)
                scannedItems.clear()
                scannedItems.addAll(drafts)
                receiptMessage = if (drafts.isEmpty()) {
                    "No inventory items were recognized. You can still add rows manually below."
                } else {
                    null
                }
            }
            .addOnFailureListener { e ->
                isScanningReceipt = false
                receiptMessage = "Could not scan receipt. Try again."
                android.util.Log.e("AddInventoryItemSheet", "Receipt scan failed", e)
            }
    }

    val receiptCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val uri = pendingReceiptUri
        if (!success || uri == null) {
            isScanningReceipt = false
            receiptMessage = "Receipt capture was cancelled."
            return@rememberLauncherForActivityResult
        }
        processReceiptUri(uri)
    }

    val receiptImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) {
            isScanningReceipt = false
            return@rememberLauncherForActivityResult
        }
        processReceiptUri(uri)
    }

    LaunchedEffect(Unit) {
        isLoadingItems = true
        try {
            foodCatalogItems = getAllFoodCatalogItems().sortedBy { it.name.lowercase() }
        } catch (e: Exception) {
            android.util.Log.e("AddInventoryItemSheet", "Error loading food catalog items", e)
        } finally {
            isLoadingItems = false
        }
    }

    val canSaveManual = selectedItem != null &&
        quantity.toIntOrNull()?.let { it > 0 } == true &&
        expirationDate.isNotBlank()

    val canSaveReceipt = scannedItems.isNotEmpty() &&
        scannedItems.all {
            it.item != null &&
                it.quantity.toIntOrNull()?.let { qty -> qty > 0 } == true &&
                it.expiryDate.isNotBlank()
        }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = SavrColors.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
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
                    text = if (mode == AddInventoryMode.MANUAL) "Add Item" else "Scan Receipt",
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
                    Text("X", fontSize = 13.sp, color = SavrColors.TextMid)
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
                    isActive = mode == AddInventoryMode.MANUAL,
                    onClick = { mode = AddInventoryMode.MANUAL },
                    modifier = Modifier.weight(1f)
                )
                ModeChip(
                    text = "Receipt",
                    isActive = mode == AddInventoryMode.RECEIPT,
                    onClick = { mode = AddInventoryMode.RECEIPT },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(14.dp))

            if (mode == AddInventoryMode.MANUAL) {
                SheetSectionLabel(
                    text = "Select Item",
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
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
                    placeholder = "Search for an item",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    onItemSelected = { item ->
                        selectedItem = item
                        manualSearchQuery = item.name
                    }
                )

                Spacer(Modifier.height(14.dp))
                SheetSectionLabel(
                    text = "Quantity",
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(6.dp))
                QuantityEditor(
                    quantity = quantity,
                    onQuantityChange = { quantity = it },
                    unit = selectedItem?.defaultUnit.orEmpty(),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(Modifier.height(14.dp))
                SheetSectionLabel(
                    text = "Best-by date",
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(6.dp))
                DatePickerField(
                    value = expirationDate,
                    label = "Best-by date",
                    onDateSelected = { expirationDate = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                )

                Spacer(Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (canSaveManual) SavrColors.Dark else SavrColors.CreamMid)
                        .clickable(enabled = canSaveManual) {
                            val item = selectedItem ?: return@clickable
                            val qty = quantity.toIntOrNull() ?: return@clickable
                            val quantityWithUnit = "$qty ${item.defaultUnit}".trim()
                            val serialized = serializeInventoryItem(
                                item = CurrentInventoryItem(
                                    id = System.currentTimeMillis().toInt(),
                                    emoji = item.emoji,
                                    name = item.name,
                                    quantity = quantityWithUnit,
                                    expiryLabel = expirationDate.trim(),
                                    status = calculateExpiryStatus(expirationDate.trim()),
                                    category = mapCategoryToInventoryCategory(item.category)
                                ),
                                category = item.category
                            )
                            onSave(listOf(serialized))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+ Add to Inventory",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (canSaveManual) SavrColors.White else SavrColors.TextMuted
                    )
                }
            } else {
                SheetSectionLabel(
                    text = "Receipt Scan",
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (!isScanningReceipt && !isLoadingItems) SavrColors.Sage
                            else SavrColors.CreamMid
                        )
                        .clickable(enabled = !isScanningReceipt && !isLoadingItems) {
                            receiptMessage = null
                            isScanningReceipt = true
                            val uri = createReceiptImageUri(context)
                            pendingReceiptUri = uri
                            receiptCameraLauncher.launch(uri)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when {
                            isLoadingItems -> "Loading catalog..."
                            isScanningReceipt -> "Scanning receipt..."
                            else -> "Take Receipt Picture"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!isScanningReceipt && !isLoadingItems) SavrColors.White else SavrColors.TextMuted
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
                            if (!isScanningReceipt && !isLoadingItems) SavrColors.CreamMid
                            else SavrColors.Cream
                        )
                        .clickable(enabled = !isScanningReceipt && !isLoadingItems) {
                            receiptMessage = null
                            isScanningReceipt = true
                            receiptImagePickerLauncher.launch("image/*")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Choose Receipt Image",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (!isScanningReceipt && !isLoadingItems) SavrColors.Dark else SavrColors.TextMuted
                    )
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    text = "We use free on-device text recognition, then match receipt lines to your Firebase food catalog.",
                    color = SavrColors.TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                if (receiptMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = receiptMessage.orEmpty(),
                        color = SavrColors.Terra,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                scannedItems.forEachIndexed { index, draft ->
                    ReceiptDraftRow(
                        draft = draft,
                        foodCatalogItems = foodCatalogItems,
                        onUpdate = { scannedItems[index] = it },
                        onRemove = { scannedItems.removeAll { item -> item.id == draft.id } }
                    )
                }

                if (scannedItems.isEmpty()) {
                    Text(
                        text = "No scanned items yet. Scan a receipt to review predicted items and quantities.",
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
                        .background(if (foodCatalogItems.isNotEmpty()) SavrColors.CreamMid else SavrColors.Cream)
                        .clickable(enabled = foodCatalogItems.isNotEmpty()) {
                            scannedItems.add(
                                ReceiptDraftItem(
                                    id = System.currentTimeMillis(),
                                    item = foodCatalogItems.firstOrNull(),
                                    quantity = "1",
                                    expiryDate = defaultReceiptExpiryDate()
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+ Add Item Manually",
                        color = if (foodCatalogItems.isNotEmpty()) SavrColors.Dark else SavrColors.TextMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (canSaveReceipt) SavrColors.Dark else SavrColors.CreamMid)
                        .clickable(enabled = canSaveReceipt) {
                            val serializedItems = scannedItems.mapNotNull { draft ->
                                val item = draft.item ?: return@mapNotNull null
                                val qty = draft.quantity.toIntOrNull() ?: return@mapNotNull null
                                if (draft.expiryDate.isBlank()) return@mapNotNull null
                                val quantityWithUnit = "$qty ${item.defaultUnit}".trim()
                                serializeInventoryItem(
                                    item = CurrentInventoryItem(
                                        id = (System.currentTimeMillis() + draft.id).toInt(),
                                        emoji = item.emoji,
                                        name = item.name,
                                        quantity = quantityWithUnit,
                                        expiryLabel = draft.expiryDate.trim(),
                                        status = calculateExpiryStatus(draft.expiryDate.trim()),
                                        category = mapCategoryToInventoryCategory(item.category)
                                    ),
                                    category = item.category
                                )
                            }
                            if (serializedItems.size == scannedItems.size) {
                                onSave(serializedItems)
                            } else {
                                receiptMessage = "Please fix invalid scanned rows before saving."
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+ Add Scanned Items",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (canSaveReceipt) SavrColors.White else SavrColors.TextMuted
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReceiptDraftRow(
    draft: ReceiptDraftItem,
    foodCatalogItems: List<FoodCatalogItem>,
    onUpdate: (ReceiptDraftItem) -> Unit,
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
            .padding(bottom = 8.dp)
            .border(1.dp, SavrColors.CreamMid, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Scanned Item", color = SavrColors.TextMid, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "Remove",
                    color = SavrColors.Terra,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onRemove() }
                )
            }

            Spacer(Modifier.height(6.dp))

            SearchableFoodCatalogPicker(
                query = searchQuery,
                onQueryChange = {
                    searchQuery = it
                    if (it.isBlank()) onUpdate(draft.copy(item = null))
                },
                selectedItem = draft.item,
                items = foodCatalogItems,
                isLoading = false,
                placeholder = "Search item",
                onItemSelected = { item ->
                    searchQuery = item.name
                    onUpdate(draft.copy(item = item))
                }
            )

            Spacer(Modifier.height(8.dp))

            QuantityEditor(
                quantity = draft.quantity,
                onQuantityChange = { onUpdate(draft.copy(quantity = it)) },
                unit = draft.item?.defaultUnit.orEmpty()
            )

            Spacer(Modifier.height(8.dp))

            DatePickerField(
                value = draft.expiryDate,
                label = "Best-by date",
                onDateSelected = { onUpdate(draft.copy(expiryDate = it)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun QuantityEditor(
    quantity: String,
    onQuantityChange: (String) -> Unit,
    unit: String,
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
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.toIntOrNull()?.let { it > 0 } == true) {
                    onQuantityChange(newValue)
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
            text = unit,
            color = SavrColors.TextMuted,
            fontSize = 14.sp,
            modifier = Modifier.width(64.dp)
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
        items.filter {
            query.isBlank() || it.name.contains(query, ignoreCase = true)
        }
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
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) expanded = true
                },
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
                        text = "No matching items",
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
private fun SheetSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text          = text.uppercase(),
        fontSize      = 10.sp,
        fontWeight    = FontWeight.ExtraBold,
        letterSpacing = 0.8.sp,
        color         = SavrColors.TextMuted,
        modifier      = modifier
    )
}

@Composable
private fun DatePickerField(
    value: String,
    label: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.clickable { showPicker = true }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label, color = SavrColors.TextMid) },
            placeholder = {
                Text("Select a date", color = SavrColors.TextMuted, fontSize = 15.sp)
            },
            trailingIcon = {
                Text(
                    text = "📅",
                    color = SavrColors.TextMuted,
                    fontSize = 16.sp
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(13.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = SavrColors.Dark,
                disabledBorderColor = Color(0xFFE2DDD5),
                disabledContainerColor = SavrColors.Cream,
                disabledLabelColor = SavrColors.TextMid,
                disabledPlaceholderColor = SavrColors.TextMuted,
                disabledTrailingIconColor = SavrColors.TextMuted,
                focusedBorderColor = SavrColors.Sage,
                unfocusedBorderColor = Color(0xFFE2DDD5),
                focusedContainerColor = SavrColors.White,
                unfocusedContainerColor = SavrColors.Cream,
                cursorColor = SavrColors.Sage
            ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = SavrColors.Dark)
        )
    }

    if (showPicker) {
        InventoryDatePickerDialog(
            initialDate = value,
            onDismiss = { showPicker = false },
            onDateSelected = {
                onDateSelected(it)
                showPicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InventoryDatePickerDialog(
    initialDate: String,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    val initialSelectedDateMillis = parseInventoryDateOrNull(initialDate)?.toPickerMillis()
        ?: LocalDate.now().toPickerMillis()
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialSelectedDateMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedDateMillis = datePickerState.selectedDateMillis
                    if (selectedDateMillis != null) {
                        onDateSelected(pickerMillisToDateString(selectedDateMillis))
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
