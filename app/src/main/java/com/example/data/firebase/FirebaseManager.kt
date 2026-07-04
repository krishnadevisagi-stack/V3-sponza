package com.example.data.firebase

import android.content.Context
import com.example.data.adengine.ApplicationLogManager
import com.example.data.model.*
import com.example.data.repository.WalletRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

object FirebaseManager {
    private const val TAG = "FirebaseManager"

    // Checks if Firebase services are successfully initialized with google-services.json
    val isFirebaseAvailable: Boolean
        get() = try {
            FirebaseApp.getInstance()
            true
        } catch (e: Exception) {
            false
        }

    fun getAuth(): FirebaseAuth? {
        return if (isFirebaseAvailable) {
            FirebaseAuth.getInstance()
        } else {
            null
        }
    }

    fun getFirestore(): FirebaseFirestore? {
        return if (isFirebaseAvailable) {
            FirebaseFirestore.getInstance()
        } else {
            null
        }
    }

    /**
     * Synchronize user profile and coin balance with Firebase Firestore
     */
    fun syncUserProfile(userProfile: UserProfile) {
        val firestore = getFirestore() ?: run {
            ApplicationLogManager.i(TAG, "Firebase not initialized. Simulating user profile sync for ${userProfile.email}")
            return
        }

        val userDoc = firestore.collection("users").document(userProfile.email)
        val data = mapOf(
            "email" to userProfile.email,
            "username" to userProfile.username,
            "fullName" to userProfile.fullName,
            "mobile" to userProfile.mobile,
            "coins" to userProfile.coins,
            "walletBalance" to userProfile.walletBalance,
            "walletId" to userProfile.walletId,
            "guestAccount" to userProfile.guestAccount,
            "lastLogin" to userProfile.lastLogin,
            "updatedAt" to System.currentTimeMillis()
        )

        userDoc.set(data)
            .addOnSuccessListener {
                ApplicationLogManager.i(TAG, "Successfully synced user profile to Firebase: ${userProfile.email}")
            }
            .addOnFailureListener { e ->
                ApplicationLogManager.d(TAG, "Failed to sync user profile to Firebase: ${e.message}")
            }
    }

    /**
     * Creates a new UPI cashout withdrawal request on Firebase Firestore
     */
    fun createUpiRequest(withdrawal: WithdrawalHistoryEntity) {
        val firestore = getFirestore() ?: run {
            ApplicationLogManager.i(TAG, "Firebase not initialized. Simulated UPI Cashout uploaded to WebAdmin (Pending)")
            return
        }

        val requestDoc = firestore.collection("requests_upi").document(withdrawal.withdrawalId)
        val data = mapOf(
            "requestId" to withdrawal.withdrawalId,
            "userId" to withdrawal.userId,
            "upiId" to withdrawal.upiId,
            "amountRupees" to withdrawal.amountRupees,
            "coinsCost" to withdrawal.coinsCost,
            "status" to withdrawal.status, // "Pending"
            "timestamp" to withdrawal.timestamp
        )

        requestDoc.set(data)
            .addOnSuccessListener {
                ApplicationLogManager.i(TAG, "UPI cashout request uploaded successfully to Firebase WebAdmin console: ${withdrawal.withdrawalId}")
            }
            .addOnFailureListener { e ->
                ApplicationLogManager.d(TAG, "Failed to upload UPI cashout request: ${e.message}")
            }
    }

    /**
     * Creates a new Voucher redemption request on Firebase Firestore
     */
    fun createVoucherRequest(redeem: RedeemHistoryEntity) {
        val firestore = getFirestore() ?: run {
            ApplicationLogManager.i(TAG, "Firebase not initialized. Simulated Voucher redemption uploaded to WebAdmin (Pending)")
            return
        }

        val requestDoc = firestore.collection("requests_vouchers").document(redeem.redeemId)
        val data = mapOf(
            "requestId" to redeem.redeemId,
            "userId" to redeem.userId,
            "brandName" to redeem.brandName,
            "coinsCost" to redeem.coinsCost,
            "estimatedValueRupees" to redeem.estimatedValueRupees,
            "status" to redeem.status, // "Pending"
            "voucherCode" to redeem.voucherCode, // Empty or "Awaiting Approval" initially
            "timestamp" to redeem.timestamp
        )

        requestDoc.set(data)
            .addOnSuccessListener {
                ApplicationLogManager.i(TAG, "Voucher request uploaded successfully to Firebase WebAdmin console: ${redeem.redeemId}")
            }
            .addOnFailureListener { e ->
                ApplicationLogManager.d(TAG, "Failed to upload Voucher request: ${e.message}")
            }
    }

    /**
     * Syncs request approvals from Firestore back into local SQLite Room database.
     * If the admin updates request statuses in Firebase Firestore, this will update the app.
     */
    fun syncPendingRequests(
        userId: String,
        walletRepository: WalletRepository,
        scope: CoroutineScope,
        onComplete: () -> Unit = {}
    ) {
        val firestore = getFirestore()
        if (firestore == null) {
            // Firebase not configured yet, run offline-simulation of admin approval flow
            simulateOfflineApproval(userId, walletRepository, scope)
            onComplete()
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                // 1. Sync UPI Cashout Requests
                firestore.collection("requests_upi")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        scope.launch(Dispatchers.IO) {
                            for (doc in querySnapshot.documents) {
                                val requestId = doc.getString("requestId") ?: continue
                                val status = doc.getString("status") ?: "Pending"
                                
                                // Fetch local withdrawal record
                                val localWithdrawals = walletRepository.getWithdrawals(userId).first()
                                val local = localWithdrawals.find { it.withdrawalId == requestId }
                                
                                if (local != null && local.status != status) {
                                    val updated = local.copy(status = status)
                                    walletRepository.insertWithdrawalRecord(updated)
                                    
                                    // Log Wallet Activity
                                    walletRepository.insertActivityRecord(
                                        WalletActivityEntity(
                                            activityId = UUID.randomUUID().toString(),
                                            userId = userId,
                                            title = "UPI Cashout $status",
                                            description = "Admin reviewed UPI payout request: ₹${local.amountRupees}",
                                            amountCoins = local.coinsCost,
                                            type = "WITHDRAW",
                                            timestamp = System.currentTimeMillis()
                                        )
                                    )
                                    ApplicationLogManager.i(TAG, "UPI Cashout $requestId status updated to $status by Admin.")
                                }
                            }
                        }
                    }

                // 2. Sync Voucher Redemption Requests
                firestore.collection("requests_vouchers")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        scope.launch(Dispatchers.IO) {
                            for (doc in querySnapshot.documents) {
                                val requestId = doc.getString("requestId") ?: continue
                                val status = doc.getString("status") ?: "Pending"
                                val voucherCode = doc.getString("voucherCode") ?: ""

                                // Fetch local redeem records
                                val localRedemptions = walletRepository.getRedemptions(userId).first()
                                val local = localRedemptions.find { it.redeemId == requestId }

                                if (local != null && (local.status != status || local.voucherCode != voucherCode)) {
                                    val updated = local.copy(status = status, voucherCode = voucherCode)
                                    walletRepository.insertRedeemRecord(updated)

                                    // Log Wallet Activity
                                    walletRepository.insertActivityRecord(
                                        WalletActivityEntity(
                                            activityId = UUID.randomUUID().toString(),
                                            userId = userId,
                                            title = "${local.brandName} Voucher $status",
                                            description = if (status == "Approved") {
                                                "Voucher Code: $voucherCode"
                                            } else {
                                                "Voucher request status: $status"
                                            },
                                            amountCoins = local.coinsCost,
                                            type = "REDEEM",
                                            timestamp = System.currentTimeMillis()
                                        )
                                    )
                                    ApplicationLogManager.i(TAG, "Voucher $requestId status updated to $status with code by Admin.")
                                }
                            }
                            onComplete()
                        }
                    }
                    .addOnFailureListener {
                        onComplete()
                    }
            } catch (e: Exception) {
                ApplicationLogManager.d(TAG, "Error syncing from Firebase: ${e.message}")
                onComplete()
            }
        }
    }

    /**
     * Simulates admin approval for local development if Firebase is not linked yet.
     * Approves "Pending" requests that are older than 15 seconds to demonstrate the real-world flow.
     */
    private fun simulateOfflineApproval(userId: String, walletRepository: WalletRepository, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            
            // 1. Simulate UPI Cashout Approvals
            val localWithdrawals = walletRepository.getWithdrawals(userId).first()
            for (w in localWithdrawals) {
                if (w.status == "Pending" && (now - w.timestamp) > 15000) {
                    val approved = w.copy(status = "Approved")
                    walletRepository.insertWithdrawalRecord(approved)

                    walletRepository.insertActivityRecord(
                        WalletActivityEntity(
                            activityId = UUID.randomUUID().toString(),
                            userId = userId,
                            title = "UPI Cashout Approved (Simulated)",
                            description = "Admin reviewed and successfully paid ₹${w.amountRupees} to ${w.upiId}",
                            amountCoins = w.coinsCost,
                            type = "WITHDRAW",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    ApplicationLogManager.i(TAG, "Simulated Approval: UPI Cashout ${w.withdrawalId} marked Approved.")
                }
            }

            // 2. Simulate Voucher Approvals
            val localRedemptions = walletRepository.getRedemptions(userId).first()
            for (r in localRedemptions) {
                if (r.status == "Pending" && (now - r.timestamp) > 15000) {
                    val randCode = "${r.brandName.take(3).uppercase()}-${(10000..99999).random()}-${(1000..9999).random()}"
                    val approved = r.copy(status = "Approved", voucherCode = randCode)
                    walletRepository.insertRedeemRecord(approved)

                    walletRepository.insertActivityRecord(
                        WalletActivityEntity(
                            activityId = UUID.randomUUID().toString(),
                            userId = userId,
                            title = "${r.brandName} Voucher Approved (Simulated)",
                            description = "Voucher code generated: $randCode",
                            amountCoins = r.coinsCost,
                            type = "REDEEM",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    ApplicationLogManager.i(TAG, "Simulated Approval: Voucher ${r.redeemId} marked Approved with code: $randCode.")
                }
            }
        }
    }
}
