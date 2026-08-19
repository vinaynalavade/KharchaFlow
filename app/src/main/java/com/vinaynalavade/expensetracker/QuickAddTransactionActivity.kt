package com.vinaynalavade.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.core.model.Currency
import com.vinaynalavade.expensetracker.core.notification.NotificationHelper
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.di.AppContainer
import com.vinaynalavade.expensetracker.domain.model.Category
import com.vinaynalavade.expensetracker.domain.model.Transaction
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.domain.model.UserPreferences
import com.vinaynalavade.expensetracker.presentation.components.CategoryIcon
import com.vinaynalavade.expensetracker.presentation.entry.components.TransactionDateSelector
import com.vinaynalavade.expensetracker.presentation.theme.ButtonShape
import com.vinaynalavade.expensetracker.presentation.theme.CardShape
import com.vinaynalavade.expensetracker.presentation.theme.ExpenseTrackerTheme
import com.vinaynalavade.expensetracker.presentation.theme.SheetShape
import com.vinaynalavade.expensetracker.presentation.theme.financialColors
import com.vinaynalavade.expensetracker.presentation.theme.spacing
import com.vinaynalavade.expensetracker.presentation.widget.ExpenseTrackerWidgetProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Lightweight, compact Activity for quick transaction entry from widgets.
 * Launches as a translucent overlay, saves via existing domain layer, then finish().
 */
class QuickAddTransactionActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as ExpenseTrackerApp
        val container = app.container

        val startRoute = intent?.getStringExtra(NotificationHelper.EXTRA_START_ROUTE)
        val transactionType = if (startRoute == NotificationHelper.ROUTE_ADD_INCOME) {
            TransactionType.INCOME
        } else {
            TransactionType.EXPENSE
        }

        setContent {
            val userPreferences by container.getUserPreferencesUseCase()
                .collectAsStateWithLifecycle(initialValue = UserPreferences())

            ExpenseTrackerTheme(
                themeMode = userPreferences.themeMode,
                dynamicColor = userPreferences.useDynamicColors,
                currency = userPreferences.currency
            ) {
                QuickAddOverlay(
                    transactionType = transactionType,
                    container = container,
                    currency = userPreferences.currency,
                    onDismiss = { finish() },
                    onSaved = {
                        ExpenseTrackerWidgetProvider.updateAll(this@QuickAddTransactionActivity)
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickAddOverlay(
    transactionType: TransactionType,
    container: AppContainer,
    currency: Currency,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedDateEpoch by remember { mutableStateOf(System.currentTimeMillis()) }
    var note by remember { mutableStateOf("") }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    val isExpense = transactionType == TransactionType.EXPENSE
    val accentColor = if (isExpense) MaterialTheme.financialColors.expense else MaterialTheme.financialColors.income
    val title = if (isExpense) "Quick Expense" else "Quick Income"

    LaunchedEffect(Unit) {
        categories = container.getCategoriesUseCase.getByType(transactionType).firstOrNull() ?: emptyList()
        selectedCategory = categories.firstOrNull()
        try { focusRequester.requestFocus() } catch (_: Exception) {}
    }

    fun doSave() {
        val parsedAmount = Amount.fromStringOrNull(amountText, currency)
        val cat = selectedCategory
        if (parsedAmount == null || parsedAmount.isZero || cat == null || isSaving) return

        isSaving = true
        keyboardController?.hide()
        focusManager.clearFocus()

        scope.launch {
            val now = System.currentTimeMillis()
            val transaction = Transaction(
                amount = parsedAmount,
                type = transactionType,
                category = cat,
                note = note.trim().ifBlank { null },
                timestamp = selectedDateEpoch,
                createdAt = now,
                updatedAt = now
            )
            val result = container.addTransactionUseCase(transaction)
            if (result is AppResult.Success) {
                delay(150)
                onSaved()
            } else if (result is AppResult.Error) {
                isSaving = false
                errorMessage = result.error.message
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = false, onClick = {})
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            shape = SheetShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.lg)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(MaterialTheme.spacing.sm)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

                // Amount Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() || it == '.' }) {
                            if (input.count { it == '.' } <= 1) {
                                amountText = input
                            }
                        }
                    },
                    placeholder = { Text("0.00", style = MaterialTheme.typography.headlineMedium) },
                    prefix = {
                        Text(
                            text = currency.symbol,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    ),
                    singleLine = true,
                    shape = ButtonShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        cursorColor = accentColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

                // Category Grid
                Text(
                    text = "CATEGORY",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
                ) {
                    categories.forEach { category ->
                        val isSelected = selectedCategory?.id == category.id
                        Surface(
                            shape = CardShape,
                            color = if (isSelected) accentColor.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .clip(CardShape)
                                .clickable { selectedCategory = category }
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = MaterialTheme.spacing.sm,
                                    vertical = MaterialTheme.spacing.xs
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CategoryIcon(
                                    iconName = category.iconName,
                                    colorHex = category.colorHex,
                                    size = 24.dp,
                                    iconSize = 14.dp
                                )
                                Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(MaterialTheme.spacing.sm)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

                // Date Selector
                TransactionDateSelector(
                    selectedDateEpoch = selectedDateEpoch,
                    onDateSelect = { selectedDateEpoch = it },
                    horizontalPadding = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

                // Optional Note
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("Add a note (optional)") },
                    singleLine = true,
                    shape = ButtonShape,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))

                // Save Button
                Button(
                    onClick = { doSave() },
                    enabled = amountText.isNotBlank() && selectedCategory != null && !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = ButtonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isExpense) "Save Expense" else "Save Income",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))
            }
        }
    }
}
