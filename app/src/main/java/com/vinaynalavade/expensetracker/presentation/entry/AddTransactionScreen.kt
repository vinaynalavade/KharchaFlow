package com.vinaynalavade.expensetracker.presentation.entry

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.vinaynalavade.expensetracker.presentation.theme.safeBottomInsets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.presentation.components.AppTopBar
import com.vinaynalavade.expensetracker.presentation.components.CalculatorBottomSheet
import com.vinaynalavade.expensetracker.presentation.components.PaymentMethodSelector
import com.vinaynalavade.expensetracker.presentation.entry.components.AmountInput
import com.vinaynalavade.expensetracker.presentation.entry.components.CategorySelector
import com.vinaynalavade.expensetracker.presentation.entry.components.DiscardChangesDialog
import com.vinaynalavade.expensetracker.presentation.entry.components.TransactionDateSelector
import com.vinaynalavade.expensetracker.presentation.entry.components.TransactionNoteField
import com.vinaynalavade.expensetracker.presentation.entry.components.TransactionSaveButton
import com.vinaynalavade.expensetracker.presentation.theme.spacing

/**
 * Unified, high-performance transaction entry screen for Add & Edit operations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: AddTransactionViewModel,
    onNavigateBack: () -> Unit,
    onTransactionSaved: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val amountFocusRequester = remember { FocusRequester() }

    var showDiscardDialog by remember { mutableStateOf(false) }
    var showCalculatorSheet by remember { mutableStateOf(false) }
    val calcSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val handleBackNavigation = {
        if (uiState.hasUnsavedChanges && !uiState.isSaveSuccess) {
            showDiscardDialog = true
        } else {
            keyboardController?.hide()
            onNavigateBack()
        }
    }

    BackHandler(enabled = true) {
        handleBackNavigation()
    }

    LaunchedEffect(Unit) {
        if (!uiState.isEditMode) {
            amountFocusRequester.requestFocus()
        }
    }

    val screenTitle = if (uiState.isEditMode) {
        if (uiState.transactionType == TransactionType.INCOME) "Edit Income" else "Edit Expense"
    } else {
        if (uiState.transactionType == TransactionType.INCOME) "Add Income" else "Add Expense"
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = screenTitle,
                canNavigateBack = true,
                onNavigateBack = handleBackNavigation
            )
        },
        bottomBar = {
            TransactionSaveButton(
                transactionType = uiState.transactionType,
                isEnabled = uiState.isFormValid,
                isSaving = uiState.isSaving,
                isEditMode = uiState.isEditMode,
                onSaveClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    viewModel.saveTransaction { confirmationMessage ->
                        onTransactionSaved(confirmationMessage)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .safeBottomInsets()
                    .padding(bottom = MaterialTheme.spacing.md)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = MaterialTheme.spacing.xl)
        ) {
            if (uiState.generalError != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.sm),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = uiState.generalError ?: "Failed to save transaction",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(MaterialTheme.spacing.md)
                    )
                }
            }

            // 1. Primary Calculator Amount Input
            AmountInput(
                amountText = uiState.amountInput,
                onAmountChange = viewModel::onAmountChange,
                transactionType = uiState.transactionType,
                errorMessage = uiState.amountError,
                focusRequester = amountFocusRequester,
                onOpenCalculator = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    showCalculatorSheet = true
                },
                onImeAction = {
                    focusManager.clearFocus()
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.lg),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

            // 2. Category Selection
            CategorySelector(
                categories = uiState.availableCategories,
                selectedCategory = uiState.selectedCategory,
                onCategorySelect = viewModel::onCategorySelect,
                errorMessage = uiState.categoryError
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))

            // 3. Payment Method Selection
            PaymentMethodSelector(
                selectedMethod = uiState.selectedPaymentMethod,
                onMethodSelect = viewModel::onPaymentMethodSelect
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))

            // 4. Transaction Date Selection
            TransactionDateSelector(
                selectedDateEpoch = uiState.selectedDateEpoch,
                onDateSelect = viewModel::onDateSelect
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))

            // 5. Optional Note Field
            TransactionNoteField(
                note = uiState.note,
                onNoteChange = viewModel::onNoteChange,
                onImeAction = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xxl))
        }
    }

    if (showDiscardDialog) {
        DiscardChangesDialog(
            onConfirmDiscard = {
                showDiscardDialog = false
                keyboardController?.hide()
                onNavigateBack()
            },
            onDismiss = {
                showDiscardDialog = false
            }
        )
    }

    if (showCalculatorSheet) {
        CalculatorBottomSheet(
            sheetState = calcSheetState,
            initialAmount = uiState.amountInput,
            onDismissRequest = {
                showCalculatorSheet = false
            },
            onUseResult = { calculatedAmount ->
                viewModel.onAmountChange(calculatedAmount)
                showCalculatorSheet = false
            }
        )
    }
}
