package com.example.data.repository

import com.example.data.database.*
import com.example.data.model.*
import com.example.data.config.AdConfig
import com.example.data.firebase.FirebaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

/**
 * -----------------------------------------------------------------
 * USER PROFILE & PREFERENCES REPOSITORY
 * -----------------------------------------------------------------
 * Purpose: Centrally stores and manages user identity, sessions, and historic activity.
 * Responsibilities:
 *   - Load, save, and update user info, selected categories, and watch milestones.
 *   - Access and record bookmarked ads, redemption history, and point logs.
 * Dependencies:
 *   - Room DAOs: [UserDao], [WatchHistoryDao], [SavedAdDao], [RedeemHistoryDao], [CoinHistoryDao]
 * Future Extension: Connect with Google Sign-In or OAuth2.0 authentication endpoints.
 */
class UserRepository(
    private val userDao: UserDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val savedAdDao: SavedAdDao,
    private val redeemHistoryDao: RedeemHistoryDao,
    private val coinHistoryDao: CoinHistoryDao,
    private val walletDao: WalletDao? = null,
    private val walletActivityDao: WalletActivityDao? = null,
    private val rewardHistoryDao: RewardHistoryDao? = null
) {

    val userProfile: Flow<UserProfile?> = userDao.getUserProfile()
    val watchHistory: Flow<List<WatchHistory>> = watchHistoryDao.getWatchHistory()
    val savedAds: Flow<List<SavedAd>> = savedAdDao.getSavedAds()
    val redeemHistory: Flow<List<RedeemHistory>> = redeemHistoryDao.getRedeemHistory()
    val coinHistory: Flow<List<CoinHistory>> = coinHistoryDao.getCoinHistory()

    /**
     * Checks if the user is registered and logged in
     */
    suspend fun isUserLoggedIn(): Boolean = withContext(Dispatchers.IO) {
        val user = userProfile.firstOrNull()
        user?.isLoggedIn == true
    }

    /**
     * Set selected categories for the logged-in user or newly registered user
     */
    suspend fun registerUserProfile(username: String, email: String, categories: List<String>) = withContext(Dispatchers.IO) {
        val categoriesStr = categories.joinToString(",")
        val currentUser = userProfile.firstOrNull()
        if (currentUser != null) {
            val updatedUser = currentUser.copy(
                username = if (currentUser.guestAccount) username else currentUser.username,
                email = if (currentUser.guestAccount) email else currentUser.email,
                selectedCategories = categoriesStr,
                profileCreated = true,
                isLoggedIn = true
            )
            userDao.insertUserProfile(updatedUser)
            FirebaseManager.syncUserProfile(updatedUser)
        } else {
            // Fallback: create fresh profile
            val walletId = "W-${(100000..999999).random()}"
            val profile = UserProfile(
                id = 0,
                username = username,
                fullName = username,
                email = email,
                mobile = "9999999999",
                passwordHash = "",
                guestAccount = false,
                walletId = walletId,
                selectedCategories = categoriesStr,
                profileCreated = true,
                isLoggedIn = true,
                coins = AdConfig.INITIAL_SIGNON_BONUS_COINS,
                walletBalance = AdConfig.INITIAL_SIGNON_BONUS_COINS * AdConfig.COINS_TO_USD_RATIO
            )
            userDao.logoutAllUsers()
            userDao.insertUserProfile(profile)
            FirebaseManager.syncUserProfile(profile)

            coinHistoryDao.insertCoinHistory(
                CoinHistory(
                    title = "Welcome Sign-Up Bonus",
                    amount = AdConfig.INITIAL_SIGNON_BONUS_COINS
                )
            )
        }
    }

    /**
     * Login a registered user with email/mobile and password
     */
    suspend fun loginUser(emailOrMobile: String, passwordRaw: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        val hash = com.example.data.utils.SecurityUtils.hashPassword(passwordRaw)
        // Check by email first
        var user = userDao.getUserByEmail(emailOrMobile)
        if (user == null) {
            // Check by mobile
            user = userDao.getUserByMobile(emailOrMobile)
        }

        if (user == null) {
            return@withContext Result.failure(Exception("User not found."))
        }
        if (user.passwordHash != hash) {
            return@withContext Result.failure(Exception("Incorrect password."))
        }

        // Logout all users first to ensure only one active session
        userDao.logoutAllUsers()
        val loggedInUser = user.copy(
            isLoggedIn = true,
            lastLogin = System.currentTimeMillis()
        )
        userDao.insertUserProfile(loggedInUser)
        FirebaseManager.syncUserProfile(loggedInUser)
        Result.success(loggedInUser)
    }

    /**
     * Register a new user profile locally
     */
    suspend fun registerUser(
        username: String,
        fullName: String,
        email: String,
        mobile: String,
        passwordRaw: String
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        // Check if email already registered
        val existingEmail = userDao.getUserByEmail(email)
        if (existingEmail != null) {
            return@withContext Result.failure(Exception("Email is already registered."))
        }
        // Check if mobile already registered
        val existingMobile = userDao.getUserByMobile(mobile)
        if (existingMobile != null) {
            return@withContext Result.failure(Exception("Mobile number is already registered."))
        }

        val hash = com.example.data.utils.SecurityUtils.hashPassword(passwordRaw)
        val walletId = "W-${(100000..999999).random()}"
        val profile = UserProfile(
            id = 0, // autoGenerate
            username = username,
            fullName = fullName,
            email = email,
            mobile = mobile,
            passwordHash = hash,
            guestAccount = false,
            walletId = walletId,
            selectedCategories = "",
            createdDate = System.currentTimeMillis(),
            lastLogin = System.currentTimeMillis(),
            profileCreated = true,
            coins = AdConfig.INITIAL_SIGNON_BONUS_COINS,
            walletBalance = AdConfig.INITIAL_SIGNON_BONUS_COINS * AdConfig.COINS_TO_USD_RATIO,
            isLoggedIn = true
        )
        userDao.logoutAllUsers()
        userDao.insertUserProfile(profile)
        FirebaseManager.syncUserProfile(profile)

        // Insert coin history entry for sign up bonus
        coinHistoryDao.insertCoinHistory(
            CoinHistory(
                title = "Welcome Sign-Up Bonus",
                amount = AdConfig.INITIAL_SIGNON_BONUS_COINS
            )
        )

        val insertedUser = userDao.getUserByEmail(email) ?: profile
        Result.success(insertedUser)
    }

    /**
     * Continue anonymously as a Guest
     */
    suspend fun loginAsGuest(): UserProfile = withContext(Dispatchers.IO) {
        val rand = (10000..99999).random()
        val guestUsername = "Guest-$rand"
        val walletId = "W-G-$rand"
        
        val profile = UserProfile(
            id = 0,
            username = guestUsername,
            fullName = "Guest User",
            email = "$guestUsername@adreels.local",
            mobile = "9999999999",
            passwordHash = "",
            guestAccount = true,
            walletId = walletId,
            selectedCategories = "",
            createdDate = System.currentTimeMillis(),
            lastLogin = System.currentTimeMillis(),
            profileCreated = false,
            coins = 0,
            walletBalance = 0.0,
            isLoggedIn = true
        )
        userDao.logoutAllUsers()
        userDao.insertUserProfile(profile)

        userDao.getUserByEmail(profile.email) ?: profile
    }

    /**
     * Convert an active Guest session into a Registered User, preserving wallet, categories, and logs
     */
    suspend fun convertGuestToRegistered(
        fullName: String,
        email: String,
        mobile: String,
        passwordRaw: String
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        val currentGuest = userProfile.firstOrNull()
        if (currentGuest == null || !currentGuest.guestAccount) {
            return@withContext Result.failure(Exception("No active guest session found."))
        }

        // Check conflicts
        val existingEmail = userDao.getUserByEmail(email)
        if (existingEmail != null) {
            return@withContext Result.failure(Exception("Email is already registered."))
        }
        val existingMobile = userDao.getUserByMobile(mobile)
        if (existingMobile != null) {
            return@withContext Result.failure(Exception("Mobile number is already registered."))
        }

        val hash = com.example.data.utils.SecurityUtils.hashPassword(passwordRaw)
        val updatedProfile = currentGuest.copy(
            username = email.substringBefore("@"),
            fullName = fullName,
            email = email,
            mobile = mobile,
            passwordHash = hash,
            guestAccount = false,
            profileCreated = true,
            coins = currentGuest.coins + AdConfig.INITIAL_SIGNON_BONUS_COINS, // Retain accumulated coins + give bonus!
            walletBalance = (currentGuest.coins + AdConfig.INITIAL_SIGNON_BONUS_COINS) * AdConfig.COINS_TO_USD_RATIO
        )
        userDao.insertUserProfile(updatedProfile)
        FirebaseManager.syncUserProfile(updatedProfile)

        // Insert coin history entry for sign up bonus
        coinHistoryDao.insertCoinHistory(
            CoinHistory(
                title = "Guest to User Upgrade Bonus",
                amount = AdConfig.INITIAL_SIGNON_BONUS_COINS
            )
        )

        Result.success(updatedProfile)
    }

    /**
     * Add reward coins for watching ads
     */
    suspend fun rewardCoinsForAd(adId: String, brandName: String, coinsReward: Int, isVideo: Boolean) = withContext(Dispatchers.IO) {
        val currentUser = userDao.getActiveUserProfile() ?: return@withContext
        val newCoins = currentUser.coins + coinsReward
        val newBalance = newCoins * AdConfig.COINS_TO_USD_RATIO
        
        userDao.updateCoins(newCoins, newBalance)

        val updatedUser = userDao.getActiveUserProfile()
        if (updatedUser != null) {
            FirebaseManager.syncUserProfile(updatedUser)
        }

        // Log coin history
        val adType = if (isVideo) "Video Reel" else "Brand Card"
        coinHistoryDao.insertCoinHistory(
            CoinHistory(
                title = "Watched $adType: $brandName",
                amount = coinsReward
            )
        )

        // Log watch history
        watchHistoryDao.insertWatchHistory(
            WatchHistory(
                adId = adId,
                brandName = brandName,
                coinsEarned = coinsReward,
                category = currentUser.selectedCategories
            )
        )

        // Sync with WalletEntity in the Wallet system
        val userId = currentUser.email.ifBlank { "guest" }
        if (walletDao != null) {
            val wallet = walletDao.getWallet(userId)
            if (wallet != null) {
                val updatedWallet = wallet.copy(
                    currentCoins = wallet.currentCoins + coinsReward,
                    todayCoins = wallet.todayCoins + coinsReward,
                    weeklyCoins = wallet.weeklyCoins + coinsReward,
                    lifetimeCoins = wallet.lifetimeCoins + coinsReward,
                    redeemableCoins = wallet.redeemableCoins + coinsReward,
                    lastUpdated = System.currentTimeMillis()
                )
                walletDao.insertWallet(updatedWallet)
            } else {
                val newWallet = WalletEntity(
                    userId = userId,
                    currentCoins = coinsReward,
                    todayCoins = coinsReward,
                    weeklyCoins = coinsReward,
                    lifetimeCoins = coinsReward,
                    pendingCoins = 0,
                    redeemableCoins = coinsReward,
                    lastUpdated = System.currentTimeMillis()
                )
                walletDao.insertWallet(newWallet)
            }
        }

        if (rewardHistoryDao != null) {
            rewardHistoryDao.insertReward(
                RewardHistoryEntity(
                    rewardId = "REWARD_${java.util.UUID.randomUUID().toString().take(8)}",
                    adId = adId,
                    userId = userId,
                    amountCoins = coinsReward,
                    sourceType = if (isVideo) "REEL" else "FEED",
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        if (walletActivityDao != null) {
            walletActivityDao.insertActivity(
                WalletActivityEntity(
                    activityId = java.util.UUID.randomUUID().toString(),
                    userId = userId,
                    title = "Watched $adType: $brandName",
                    description = "Earned coins from validated ad watch",
                    amountCoins = coinsReward,
                    type = "REWARD",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Update user selected interest categories
     */
    suspend fun updateSelectedCategories(categories: List<String>) = withContext(Dispatchers.IO) {
        val categoriesStr = categories.joinToString(",")
        userDao.updateCategories(categoriesStr)
    }

    /**
     * Save/Bookmark an advertisement
     */
    suspend fun saveAd(ad: DummyAd) = withContext(Dispatchers.IO) {
        savedAdDao.insertSavedAd(
            SavedAd(
                adId = ad.id,
                category = ad.category,
                brandName = ad.brandName,
                title = ad.title,
                description = ad.description,
                mediaUrl = ad.mediaUrl,
                isVideo = ad.isVideo
            )
        )
    }

    /**
     * Unsave/Unbookmark an advertisement
     */
    suspend fun unsaveAd(adId: String) = withContext(Dispatchers.IO) {
        savedAdDao.deleteSavedAd(adId)
    }

    /**
     * Check if ad is saved
     */
    suspend fun isAdSaved(adId: String): Boolean = withContext(Dispatchers.IO) {
        savedAdDao.isAdSaved(adId)
    }

    /**
     * Redeem/Withdraw local points for rewards
     */
    suspend fun redeemRewardPoints(itemTitle: String, coinsCost: Int): Boolean = withContext(Dispatchers.IO) {
        val currentUser = userProfile.firstOrNull() ?: return@withContext false
        if (currentUser.coins < coinsCost) return@withContext false

        val newCoins = currentUser.coins - coinsCost
        val newBalance = newCoins * AdConfig.COINS_TO_USD_RATIO
        userDao.updateCoins(newCoins, newBalance)

        // Log redeem history
        redeemHistoryDao.insertRedeemHistory(
            RedeemHistory(
                itemTitle = itemTitle,
                coinsSpent = coinsCost,
                status = "Completed"
            )
        )

        // Log negative coin history
        coinHistoryDao.insertCoinHistory(
            CoinHistory(
                title = "Redeemed: $itemTitle",
                amount = -coinsCost
            )
        )
        true
    }

    /**
     * Withdraw cash balance
     */
    suspend fun withdrawCashBalance(withdrawAmount: Double, paymentMethod: String): Boolean = withContext(Dispatchers.IO) {
        val currentUser = userProfile.firstOrNull() ?: return@withContext false
        val coinEquivalent = (withdrawAmount / AdConfig.COINS_TO_USD_RATIO).toInt()
        if (currentUser.coins < coinEquivalent) return@withContext false

        val newCoins = currentUser.coins - coinEquivalent
        val newBalance = newCoins * AdConfig.COINS_TO_USD_RATIO
        userDao.updateCoins(newCoins, newBalance)

        // Log redeem/withdraw history
        redeemHistoryDao.insertRedeemHistory(
            RedeemHistory(
                itemTitle = "Cash Out to $paymentMethod",
                coinsSpent = coinEquivalent,
                status = "Processing" // Processing because it represents real-world withdrawal processing!
            )
        )

        // Log negative coin history
        coinHistoryDao.insertCoinHistory(
            CoinHistory(
                title = "Withdrew $${String.format("%.2f", withdrawAmount)} ($paymentMethod)",
                amount = -coinEquivalent
            )
        )
        true
    }

    /**
     * Log out of current local session non-destructively
     */
    suspend fun logout() = withContext(Dispatchers.IO) {
        userDao.logoutAllUsers()
    }

    /**
     * Delete all user profiles, histories, saved ads, and logs from local SQLite Room DB
     */
    suspend fun clearAllLocalData() = withContext(Dispatchers.IO) {
        userDao.clearUser()
        watchHistoryDao.clearWatchHistory()
        savedAdDao.getSavedAds().firstOrNull()?.forEach {
            savedAdDao.deleteSavedAd(it.adId)
        }
        redeemHistoryDao.clearRedeemHistory()
        coinHistoryDao.clearCoinHistory()
    }
}
