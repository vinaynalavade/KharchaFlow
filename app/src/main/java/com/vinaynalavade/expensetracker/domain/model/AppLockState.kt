package com.vinaynalavade.expensetracker.domain.model

/**
 * Represents the current App Lock runtime security state.
 */
sealed interface AppLockState {
    /** App Lock is disabled by user in Settings. Normal unrestricted access. */
    data object Disabled : AppLockState

    /** App Lock is enabled and current session is authenticated/unlocked. */
    data object Unlocked : AppLockState

    /** App Lock is enabled and session is locked. Gating UnlockScreen must be shown. */
    data object Locked : AppLockState
}
