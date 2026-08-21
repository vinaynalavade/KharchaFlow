package com.vinaynalavade.expensetracker.presentation.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vinaynalavade.expensetracker.domain.model.Category
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.presentation.components.CategoryIcon
import com.vinaynalavade.expensetracker.presentation.theme.ButtonShape
import com.vinaynalavade.expensetracker.presentation.theme.CardShape
import com.vinaynalavade.expensetracker.presentation.theme.spacing

private val AVAILABLE_ICONS = listOf(
    "restaurant", "directions_car", "shopping_bag", "receipt_long",
    "movie", "medical_services", "school", "spa", "home",
    "account_balance", "subscriptions", "flight", "fitness_center",
    "local_cafe", "pets", "payments", "work", "trending_up",
    "storefront", "card_giftcard", "replay", "attach_money"
)

private val AVAILABLE_COLORS = listOf(
    "#EF4444", "#F97316", "#F59E0B", "#10B981", "#14B8A6",
    "#06B6D4", "#3B82F6", "#6366F1", "#8B5CF6", "#EC4899",
    "#64748B", "#DC2626"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditCategoryDialog(
    categoryToEdit: Category?,
    defaultType: TransactionType,
    onDismiss: () -> Unit,
    onSave: (name: String, iconName: String, colorHex: String, type: TransactionType, id: Long, isDefault: Boolean) -> Unit,
    errorMessage: String? = null
) {
    var name by remember { mutableStateOf(categoryToEdit?.name ?: "") }
    var selectedIcon by remember { mutableStateOf(categoryToEdit?.iconName ?: "restaurant") }
    var selectedColor by remember { mutableStateOf(categoryToEdit?.colorHex ?: "#EF4444") }
    val type = categoryToEdit?.type ?: defaultType
    val isDefault = categoryToEdit?.isDefault ?: false

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (categoryToEdit == null) "Add ${type.displayName} Category" else "Edit Category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = MaterialTheme.spacing.sm)
                    )
                }

                // Live Preview Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = MaterialTheme.spacing.md),
                    shape = CardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MaterialTheme.spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryIcon(
                            iconName = selectedIcon,
                            colorHex = selectedColor,
                            size = 40.dp,
                            iconSize = 22.dp
                        )
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.md))
                        Column {
                            Text(
                                text = name.ifBlank { "Category Name" },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (name.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${type.displayName} • ${if (isDefault) "Default System Category" else "Custom Category"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 30) name = it },
                    label = { Text("Category Name") },
                    placeholder = { Text("e.g. Groceries") },
                    singleLine = true,
                    shape = ButtonShape,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

                // Icon Picker
                Text(
                    text = "SELECT ICON",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AVAILABLE_ICONS.forEach { iconName ->
                        val isSelected = selectedIcon == iconName
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedIcon = iconName },
                            contentAlignment = Alignment.Center
                        ) {
                            CategoryIcon(
                                iconName = iconName,
                                colorHex = selectedColor,
                                size = 32.dp,
                                iconSize = 18.dp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

                // Color Picker
                Text(
                    text = "SELECT COLOR",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AVAILABLE_COLORS.forEach { hex ->
                        val isSelected = selectedColor == hex
                        val parsedColor = try {
                            Color(android.graphics.Color.parseColor(hex))
                        } catch (_: Exception) {
                            MaterialTheme.colorScheme.primary
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(parsedColor)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.White,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            name.trim(),
                            selectedIcon,
                            selectedColor,
                            type,
                            categoryToEdit?.id ?: 0L,
                            isDefault
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text(text = "Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        },
        shape = CardShape,
        containerColor = MaterialTheme.colorScheme.surface
    )
}
