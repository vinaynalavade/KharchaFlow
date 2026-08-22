package com.vinaynalavade.expensetracker

import com.vinaynalavade.expensetracker.core.backup.BackupCategory
import com.vinaynalavade.expensetracker.core.backup.BackupData
import com.vinaynalavade.expensetracker.core.backup.BackupPreferences
import com.vinaynalavade.expensetracker.core.backup.BackupTransaction
import com.vinaynalavade.expensetracker.core.backup.JsonBackupParser
import com.vinaynalavade.expensetracker.core.result.AppError
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.model.GoogleAccountInfo
import com.vinaynalavade.expensetracker.domain.model.GoogleBackupMetadata
import com.vinaynalavade.expensetracker.domain.model.GoogleBackupState
import com.vinaynalavade.expensetracker.domain.repository.GoogleDriveBackupRepository
import com.vinaynalavade.expensetracker.domain.usecase.GetGoogleBackupStateUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GoogleDriveBackupRepositoryTest {

    private lateinit var fakeRepository: FakeGoogleDriveBackupRepository
    private lateinit var getGoogleBackupStateUseCase: GetGoogleBackupStateUseCase

    @Before
    fun setUp() {
        fakeRepository = FakeGoogleDriveBackupRepository()
        getGoogleBackupStateUseCase = GetGoogleBackupStateUseCase(fakeRepository)
    }

    @Test
    fun testInitialStateIsDisconnected() = runBlocking {
        val state = getGoogleBackupStateUseCase().first()
        assertEquals(GoogleBackupState.Disconnected, state)
    }

    @Test
    fun testConnectedStateEmitsAccountInfoAndTimestamp() = runBlocking {
        val testAccount = GoogleAccountInfo(
            email = "user@gmail.com",
            displayName = "Test User",
            photoUrl = "https://example.com/photo.jpg"
        )
        fakeRepository.saveConnectedAccount(testAccount)
        fakeRepository.setLastBackupTimestamp(1724300000000L)

        val state = getGoogleBackupStateUseCase().first()
        assertTrue(state is GoogleBackupState.Connected)
        val connected = state as GoogleBackupState.Connected
        assertEquals("user@gmail.com", connected.account.email)
        assertEquals("Test User", connected.account.displayName)
        assertEquals(1724300000000L, connected.lastBackupTimestamp)
        assertTrue(connected.cloudBackupExists)
    }

    @Test
    fun testDisconnectClearsAccountState() = runBlocking {
        val testAccount = GoogleAccountInfo(email = "user@gmail.com")
        fakeRepository.saveConnectedAccount(testAccount)
        fakeRepository.disconnect()

        val state = getGoogleBackupStateUseCase().first()
        assertEquals(GoogleBackupState.Disconnected, state)
    }

    @Test
    fun testBackupDataSerializationForCloudUpload() {
        val backupData = BackupData(
            backupVersion = BackupData.CURRENT_VERSION,
            appVersion = "1.0.1",
            createdAt = 1724300000000L,
            categories = listOf(
                BackupCategory(1L, "Food", "Restaurant", "#FF5722", "EXPENSE", true)
            ),
            transactions = listOf(
                BackupTransaction(101L, 25000L, "EXPENSE", 1L, "UPI", "Lunch", 1724300000000L)
            ),
            preferences = BackupPreferences(
                currencyCode = "INR",
                themeMode = "SYSTEM"
            )
        )

        val json = JsonBackupParser.toJson(backupData)
        assertNotNull(json)
        assertTrue(json.contains("\"backupVersion\": 1"))
        assertTrue(json.contains("\"Food\""))
        assertTrue(json.contains("25000"))

        val parsed = JsonBackupParser.fromJson(json)
        assertEquals(1, parsed.categories.size)
        assertEquals(1, parsed.transactions.size)
        assertEquals(25000L, parsed.transactions.first().amountSubunits)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testCorruptedCloudBackupRejection() {
        val malformedJson = "{\"backupVersion\": 1, \"categories\": []}"
        JsonBackupParser.fromJson(malformedJson)
    }

    @Test
    fun testGoogleAuthVerificationResultSuccess() {
        val account = GoogleAccountInfo(email = "test@gmail.com", displayName = "Test User")
        val verified = com.vinaynalavade.expensetracker.core.google.GoogleAuthVerificationResult.Verified(
            account = account,
            token = "fake_oauth_token_12345"
        )

        assertEquals("test@gmail.com", verified.account.email)
        assertEquals("fake_oauth_token_12345", verified.token)
    }

    @Test
    fun testGoogleAuthVerificationResultConsentRequired() {
        val account = GoogleAccountInfo(email = "test@gmail.com")
        val fakeIntent = android.content.Intent()
        val consent = com.vinaynalavade.expensetracker.core.google.GoogleAuthVerificationResult.ConsentRequired(
            consentIntent = fakeIntent,
            account = account
        )

        assertEquals(fakeIntent, consent.consentIntent)
        assertEquals("test@gmail.com", consent.account.email)
    }

    @Test
    fun testGoogleAuthVerificationResultError() {
        val error = com.vinaynalavade.expensetracker.core.google.GoogleAuthVerificationResult.Error(
            message = "Network error"
        )

        assertEquals("Network error", error.message)
    }

    private class FakeGoogleDriveBackupRepository : GoogleDriveBackupRepository {
        private val accountFlow = MutableStateFlow<GoogleAccountInfo?>(null)
        private val timestampFlow = MutableStateFlow<Long?>(null)

        fun setLastBackupTimestamp(timestamp: Long?) {
            timestampFlow.value = timestamp
        }

        override fun getConnectedAccount(): Flow<GoogleAccountInfo?> = accountFlow

        override fun getLastCloudBackupTimestamp(): Flow<Long?> = timestampFlow

        override suspend fun saveConnectedAccount(account: GoogleAccountInfo): AppResult<Unit> {
            accountFlow.value = account
            return AppResult.Success(Unit)
        }

        override suspend fun disconnect(): AppResult<Unit> {
            accountFlow.value = null
            timestampFlow.value = null
            return AppResult.Success(Unit)
        }

        override suspend fun getCloudBackupMetadata(): AppResult<GoogleBackupMetadata?> {
            return if (accountFlow.value != null) {
                AppResult.Success(
                    GoogleBackupMetadata(
                        fileId = "fake_file_id",
                        modifiedTime = timestampFlow.value ?: System.currentTimeMillis(),
                        sizeBytes = 1024L
                    )
                )
            } else {
                AppResult.Error(AppError.UnknownError("Not connected"))
            }
        }

        override suspend fun uploadBackup(backupData: BackupData): AppResult<GoogleBackupMetadata> {
            val now = System.currentTimeMillis()
            timestampFlow.value = now
            return AppResult.Success(
                GoogleBackupMetadata(
                    fileId = "fake_file_id",
                    modifiedTime = now,
                    sizeBytes = 1024L
                )
            )
        }

        override suspend fun downloadBackup(): AppResult<BackupData> {
            return AppResult.Success(
                BackupData(
                    backupVersion = 1,
                    appVersion = "1.0.1",
                    createdAt = System.currentTimeMillis(),
                    categories = listOf(BackupCategory(1L, "Food", "Restaurant", "#FF5722", "EXPENSE", true)),
                    transactions = emptyList(),
                    preferences = BackupPreferences()
                )
            )
        }
    }
}
