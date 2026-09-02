package com.vinaynalavade.expensetracker

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.core.model.Currency
import com.vinaynalavade.expensetracker.core.notification.NotificationHelper
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.core.util.AmountInputFormatter
import com.vinaynalavade.expensetracker.di.AppContainer
import com.vinaynalavade.expensetracker.domain.model.Category
import com.vinaynalavade.expensetracker.domain.model.PaymentMethod
import com.vinaynalavade.expensetracker.domain.model.Transaction
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.domain.model.UserPreferences
import com.vinaynalavade.expensetracker.presentation.components.CategoryIcon
import com.vinaynalavade.expensetracker.presentation.components.PaymentMethodSelector
import com.vinaynalavade.expensetracker.presentation.entry.components.AmountVisualTransformation
import com.vinaynalavade.expensetracker.presentation.entry.components.TransactionDateSelector
import com.vinaynalavade.expensetracker.presentation.security.AppLockViewModel
import com.vinaynalavade.expensetracker.presentation.security.UnlockScreen
import com.vinaynalavade.expensetracker.presentation.theme.ButtonShape
import com.vinaynalavade.expensetracker.presentation.theme.CardShape
import com.vinaynalavade.expensetracker.presentation.theme.ExpenseTrackerTheme
import com.vinaynalavade.expensetracker.presentation.theme.PillShape
import com.vinaynalavade.expensetracker.presentation.theme.SheetShape
import com.vinaynalavade.expensetracker.presentation.theme.financialColors
import com.vinaynalavade.expensetracker.presentation.theme.safeBottomInsets
import com.vinaynalavade.expensetracker.presentation.theme.spacing
import com.vinaynalavade.expensetracker.presentation.widget.WidgetUpdateManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Lightweight, premium modal Activity for rapid transaction entry.
 * Launches as a translucent overlay with immediate keyboard focus, live comma amount formatting,
 * responsive category & source selection, and a pinned Save action that never occludes behind the IME.
 * Integrated with App Lock security to require biometric/PIN auth before unlocking.
 */
class QuickAddTransactionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TRANSACTION_TYPE = "extra_transaction_type"
    }

    override fun onStop() {
        super.onStop()
        val app = application as ExpenseTrackerApp
        app.container.appLockManager.onAppBackgrounded()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as ExpenseTrackerApp
        val container = app.container

        val rawType = intent?.getStringExtra(EXTRA_TRANSACTION_TYPE)
        val startRoute = intent?.getStringExtra(NotificationHelper.EXTRA_START_ROUTE)
        val initialType = when {
            rawType != null -> try {
                TransactionType.valueOf(rawType.uppercase())
            } catch (_: Exception) {
                TransactionType.EXPENSE
            }
            startRoute == NotificationHelper.ROUTE_ADD_INCOME -> TransactionType.INCOME
            else -> TransactionType.EXPENSE
        }

        setContent {
            val userPreferences by container.getUserPreferencesUseCase()
                .collectAsStateWithLifecycle(initialValue = UserPreferences())
            val isSessionUnlocked by container.appLockManager.isSessionUnlocked
                .collectAsStateWithLifecycle()

            val isLocked = userPreferences.appLockEnabled && !isSessionUnlocked

            ExpenseTrackerTheme(
                themeMode = userPreferences.themeMode,
                dynamicColor = userPreferences.useDynamicColors,
                currency = userPreferences.currency
            ) {
                if (isLocked) {
                    BackHandler {
                        finish()
                    }

                    val unlockViewModel: AppLockViewModel = viewModel(
                        factory = AppLockViewModel.Factory(
                            container.getUserPreferencesUseCase,
                            container.appLockManager,
                            container.securePinManager,
                            container.verifyPinUseCase,
                            container.savePinUseCase,
                            container.changePinUseCase,
                            container.setAppLockEnabledUseCase,
                            container.setBiometricEnabledUseCase,
                            container.disableAppLockUseCase
                        )
                    )

                    UnlockScreen(
                        viewModel = unlockViewModel,
                        onUnlockSuccess = {
                            container.appLockManager.unlock()
                            WidgetUpdateManager.refreshAllWidgets(this@QuickAddTransactionActivity)
                        }
                    )
                } else {
                    QuickAddOverlay(
                        initialType = initialType,
                        container = container,
                        userPreferences = userPreferences,
                        currency = userPreferences.currency,
                        onDismiss = { finish() },
                        onSaved = {
                            WidgetUpdateManager.refreshAllWidgets(this@QuickAddTransactionActivity)
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickAddOverlay(
    initialType: TransactionType,
    container: AppContainer,
    userPreferences: UserPreferences,
    currency: Currency,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var currentType by remember { mutableStateOf(initialType) }
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var hasUserManuallySelectedSource by remember { mutableStateOf(false) }

    val defaultSourceForCurrentType = userPreferences.getDefaultSource(currentType)

    var selectedPaymentMethod by remember { mutableStateOf(defaultSourceForCurrentType) }
    var selectedDateEpoch by remember { mutableStateOf(System.currentTimeMillis()) }
    var note by remember { mutableStateOf("") }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    val isExpense = currentType == TransactionType.EXPENSE
    val accentColor = if (isExpense) MaterialTheme.financialColors.expense else MaterialTheme.financialColors.income

    // Keep payment method in sync with default preferences & type changes unless manually overridden by user
    LaunchedEffect(currentType, userPreferences.defaultExpenseSource, userPreferences.defaultIncomeSource) {
        if (!hasUserManuallySelectedSource) {
            selectedPaymentMethod = userPreferences.getDefaultSource(currentType)
        }
    }

    // Load categories whenever currentType changes
    LaunchedEffect(currentType) {
        categories = container.getCategoriesUseCase.getByType(currentType).firstOrNull() ?: emptyList()
        selectedCategory = categories.firstOrNull()
    }

    // Auto-focus amount field on launch
    LaunchedEffect(Unit) {
        try {
            delay(100)
            focusRequester.requestFocus()
        } catch (_: Exception) {}
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
                type = currentType,
                category = cat,
                paymentMethod = selectedPaymentMethod,
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
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f))
            .statusBarsPadding()
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 540.dp)
                .fillMaxHeight(0.92f)
                .clickable(enabled = false, onClick = {})
                .safeBottomInsets(),
            shape = SheetShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Drag Handle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(PillShape)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    )
                }

                // 1. Header with Title, Type Toggle, and Dismiss Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quick Add",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
                    ) {
                        QuickTypeSegmentedButton(
                            selectedType = currentType,
                            onTypeSelected = { type ->
                                if (currentType != type) {
                                    currentType = type
                                    hasUserManuallySelectedSource = false
                                }
                            }
                        )

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.xs)
                    ) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(MaterialTheme.spacing.sm)
                        )
                    }
                }

                // 2. Scrollable Middle Form (Amount -> Category -> Source -> Date & Note)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.xs)
                ) {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

                    // Priority 1: Prominent Amount Input with live comma transformation
                    QuickAmountDisplay(
                        amountText = amountText,
                        onAmountChange = { input ->
                            val sanitized = AmountInputFormatter.sanitizeAmountInput(input, currency.decimalDigits)
                            amountText = sanitized
                        },
                        currency = currency,
                        accentColor = accentColor,
                        focusRequester = focusRequester
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

                    // Priority 3: Category Selection Chips
                    Text(
                        text = "CATEGORY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
                    ) {
                        categories.forEach { category ->
                            val isSelected = selectedCategory?.id == category.id
                            QuickCategoryChip(
                                category = category,
                                isSelected = isSelected,
                                accentColor = accentColor,
                                onClick = { selectedCategory = category }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

                    // Priority 4: Financial Source (Cash vs Account)
                    PaymentMethodSelector(
                        selectedMethod = selectedPaymentMethod,
                        onMethodSelect = {
                            selectedPaymentMethod = it
                            hasUserManuallySelectedSource = true
                        },
                        isCompact = true,
                        horizontalPadding = 0.dp
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

                    // Details: Date Selector & Optional Note
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TransactionDateSelector(
                            selectedDateEpoch = selectedDateEpoch,
                            onDateSelect = { selectedDateEpoch = it },
                            horizontalPadding = 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        placeholder = { Text("Note (optional)", style = MaterialTheme.typography.bodySmall) },
                        singleLine = true,
                        shape = ButtonShape,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                }

                // 3. Pinned Bottom Save Button (Always visible above keyboard and navigation insets)
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.sm)
                ) {
                    Button(
                        onClick = { doSave() },
                        enabled = amountText.isNotBlank() && selectedCategory != null && !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
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
                }
            }
        }
    }
}

@Composable
private fun QuickTypeSegmentedButton(
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = PillShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isExpense = selectedType == TransactionType.EXPENSE
            val isIncome = selectedType == TransactionType.INCOME

            val expenseBg by animateColorAsState(
                targetValue = if (isExpense) MaterialTheme.financialColors.expense.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f),
                animationSpec = tween(180),
                label = "expenseBg"
            )
            val incomeBg by animateColorAsState(
                targetValue = if (isIncome) MaterialTheme.financialColors.income.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f),
                animationSpec = tween(180),
                label = "incomeBg"
            )

            // Expense Tab
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(expenseBg)
                    .clickable { onTypeSelected(TransactionType.EXPENSE) }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Expense",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isExpense) FontWeight.Bold else FontWeight.Medium,
                    color = if (isExpense) MaterialTheme.financialColors.expense else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Income Tab
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(incomeBg)
                    .clickable { onTypeSelected(TransactionType.INCOME) }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Income",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isIncome) FontWeight.Bold else FontWeight.Medium,
                    color = if (isIncome) MaterialTheme.financialColors.income else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuickAmountDisplay(
    amountText: String,
    onAmountChange: (String) -> Unit,
    currency: Currency,
    accentColor: androidx.compose.ui.graphics.Color,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.5.dp,
            color = accentColor.copy(alpha = 0.4f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                focusRequester.requestFocus()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.md, vertical = MaterialTheme.spacing.md),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currency.symbol,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                modifier = Modifier.padding(end = MaterialTheme.spacing.xs)
            )

            BasicTextField(
                value = amountText,
                onValueChange = onAmountChange,
                visualTransformation = AmountVisualTransformation(),
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .semantics {
                        contentDescription = "Quick transaction amount in ${currency.name}"
                    },
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (amountText.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    textAlign = TextAlign.Start
                ),
                cursorBrush = SolidColor(accentColor),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (amountText.isEmpty()) {
                            Text(
                                text = "0.00",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

@Composable
private fun QuickCategoryChip(
    category: Category,
    isSelected: Boolean,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedBg by animateColorAsState(
        targetValue = if (isSelected) accentColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        animationSpec = tween(150),
        label = "chipBg"
    )
    val animatedBorder by animateColorAsState(
        targetValue = if (isSelected) accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
        animationSpec = tween(150),
        label = "chipBorder"
    )

    Surface(
        shape = PillShape,
        color = animatedBg,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = animatedBorder
        ),
        modifier = modifier
            .clip(PillShape)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.RadioButton
                selected = isSelected
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIcon(
                iconName = category.iconName,
                colorHex = category.colorHex,
                size = 20.dp,
                iconSize = 12.dp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isSelected) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
