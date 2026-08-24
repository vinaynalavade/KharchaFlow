package com.vinaynalavade.expensetracker.presentation.tour

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vinaynalavade.expensetracker.R
import com.vinaynalavade.expensetracker.presentation.theme.ButtonShape
import com.vinaynalavade.expensetracker.presentation.theme.PillShape
import com.vinaynalavade.expensetracker.presentation.theme.spacing
import com.vinaynalavade.expensetracker.presentation.tour.components.Step1WelcomeIllustration
import com.vinaynalavade.expensetracker.presentation.tour.components.Step2OverviewIllustration
import com.vinaynalavade.expensetracker.presentation.tour.components.Step3TransactionsIllustration
import com.vinaynalavade.expensetracker.presentation.tour.components.Step4AnalyticsIllustration
import com.vinaynalavade.expensetracker.presentation.tour.components.Step5SettingsIllustration
import kotlinx.coroutines.launch

private data class TourStepData(
    @StringRes val titleRes: Int,
    @StringRes val descRes: Int
)

private val TOUR_STEPS = listOf(
    TourStepData(
        titleRes = R.string.app_tour_step1_title,
        descRes = R.string.app_tour_step1_desc
    ),
    TourStepData(
        titleRes = R.string.app_tour_step2_title,
        descRes = R.string.app_tour_step2_desc
    ),
    TourStepData(
        titleRes = R.string.app_tour_step3_title,
        descRes = R.string.app_tour_step3_desc
    ),
    TourStepData(
        titleRes = R.string.app_tour_step4_title,
        descRes = R.string.app_tour_step4_desc
    ),
    TourStepData(
        titleRes = R.string.app_tour_step5_title,
        descRes = R.string.app_tour_step5_desc
    )
)

@Composable
fun AppTourScreen(
    viewModel: AppTourViewModel,
    onTourComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { TOUR_STEPS.size })
    val coroutineScope = rememberCoroutineScope()

    BackHandler {
        if (pagerState.currentPage > 0) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(pagerState.currentPage - 1)
            }
        } else {
            viewModel.onTourSkipped(onTourComplete)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TourTopBar(
                onSkip = {
                    viewModel.onTourSkipped(onTourComplete)
                }
            )
        },
        bottomBar = {
            TourBottomBar(
                currentPage = pagerState.currentPage,
                pageCount = TOUR_STEPS.size,
                onPrevious = {
                    if (pagerState.currentPage > 0) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                },
                onNext = {
                    if (pagerState.currentPage < TOUR_STEPS.size - 1) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        viewModel.onTourCompleted(onTourComplete)
                    }
                }
            )
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { pageIndex ->
            val step = TOUR_STEPS[pageIndex]
            TourStepPage(
                pageIndex = pageIndex,
                title = stringResource(step.titleRes),
                description = stringResource(step.descRes),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun TourTopBar(
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.md, vertical = MaterialTheme.spacing.sm),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onSkip,
            shape = ButtonShape
        ) {
            Text(
                text = stringResource(R.string.app_tour_btn_skip),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TourStepPage(
    pageIndex: Int,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = MaterialTheme.spacing.lg)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

        // Step Illustration
        when (pageIndex) {
            0 -> Step1WelcomeIllustration()
            1 -> Step2OverviewIllustration()
            2 -> Step3TransactionsIllustration()
            3 -> Step4AnalyticsIllustration()
            4 -> Step5SettingsIllustration()
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))

        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

        // Description
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.25,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))
    }
}

@Composable
private fun TourBottomBar(
    currentPage: Int,
    pageCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val indicatorAccessibility = stringResource(
        R.string.app_tour_step_indicator,
        currentPage + 1,
        pageCount
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
    ) {
        // Step Indicators
        Row(
            modifier = Modifier
                .semantics { contentDescription = indicatorAccessibility }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pageCount) { index ->
                val isSelected = index == currentPage
                val targetWidth by animateDpAsState(
                    targetValue = if (isSelected) 24.dp else 8.dp,
                    label = "indicator_width"
                )

                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .width(targetWidth)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            }
                        )
                )
            }
        }

        // Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentPage > 0) {
                OutlinedButton(
                    onClick = onPrevious,
                    shape = ButtonShape,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.app_tour_btn_previous))
                }
            }

            Button(
                onClick = onNext,
                shape = ButtonShape,
                modifier = Modifier.weight(if (currentPage > 0) 1.5f else 1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (currentPage == pageCount - 1) {
                        stringResource(R.string.app_tour_btn_finish)
                    } else {
                        stringResource(R.string.app_tour_btn_next)
                    },
                    fontWeight = FontWeight.SemiBold
                )
                if (currentPage < pageCount - 1) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
