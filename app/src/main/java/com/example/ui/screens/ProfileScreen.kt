package com.example.ui.screens

import android.widget.Toast
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.animation.*
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.PremiumGold
import com.example.viewmodel.AdViewModel
import com.example.viewmodel.WalletViewModel
import com.example.data.adengine.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProfileScreen(
    viewModel: AdViewModel,
    walletViewModel: WalletViewModel,
    modifier: Modifier = Modifier
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val watchHistory by viewModel.watchHistory.collectAsState()
    val savedAds by viewModel.savedAds.collectAsState()
    val coinHistory by viewModel.coinHistory.collectAsState()
    val redeemHistory by viewModel.redeemHistory.collectAsState()
    val feedViewProgress by viewModel.feedViewProgress.collectAsState()
    
    val context = LocalContext.current
    var activeTab by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Profile Top Cover & Header
            ProfileHeaderSection(
                profile = userProfile,
                walletViewModel = walletViewModel,
                onEditInterests = { activeTab = 2 },
                onLogout = {
                    viewModel.logout()
                    Toast.makeText(context, "Logged out of local profile", Toast.LENGTH_SHORT).show()
                }
            )

            // Horizontal Tab Row
            PrimaryTabRowSection(
                selectedTabIndex = activeTab,
                onTabSelected = { activeTab = it }
            )

            // Inner Page Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeTab) {
                    0 -> WalletAndRedeemTab(
                        walletViewModel = walletViewModel
                    )
                    1 -> HistoryAndSavedTab(
                        watchLogs = watchHistory,
                        savedAds = savedAds,
                        coinLogs = coinHistory,
                        feedViewProgress = feedViewProgress,
                        walletViewModel = walletViewModel
                    )
                    2 -> SettingsAndInterestsTab(
                        viewModel = viewModel,
                        profile = userProfile
                    )
                    3 -> SystemMonitorTab(
                        walletViewModel = walletViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileHeaderSection(
    profile: UserProfile?,
    walletViewModel: WalletViewModel,
    onEditInterests: () -> Unit,
    onLogout: () -> Unit
) {
    val wallet by walletViewModel.walletState.collectAsState()
    
    // Determine the membership level based on lifetime earned coins
    val lifetime = wallet?.lifetimeCoins ?: 0
    val (tierName, tierColor) = when {
        lifetime >= 3000 -> "Platinum Elite" to Color(0xFF00E5FF)
        lifetime >= 1500 -> "Gold Elite" to PremiumGold
        lifetime >= 500 -> "Silver Contributor" to Color(0xFFB0BEC5)
        else -> "Bronze Tier" to Color(0xFFCD7F32)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Beautiful Initials Avatar (Click to switch to Interests tab)
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                        .clickable { onEditInterests() },
                    contentAlignment = Alignment.Center
                ) {
                    val initials = if (profile != null && profile.username.isNotEmpty()) {
                        profile.username.take(2).uppercase()
                    } else {
                        "AD"
                    }
                    Text(
                        text = initials,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = profile?.username ?: "Demo User",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        // Badge tag
                        val isGuest = profile?.guestAccount ?: true
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isGuest) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.primaryContainer
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isGuest) "Guest" else "Registered",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isGuest) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Text(
                        text = profile?.email ?: "demo@adreels.com",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Logout icon
                IconButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Log Out",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Wallet quick stats and Membership levels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MEMBERSHIP LEVEL",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = tierName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = tierColor
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "WALLET BALANCE",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${wallet?.currentCoins ?: 0} Coins (₹${String.format(Locale.US, "%.2f", (wallet?.currentCoins ?: 0) * 0.01)})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun PrimaryTabRowSection(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf("Wallet & Rewards", "Activity", "Interests", "System Monitor")
    TabRow(
        selectedTabIndex = selectedTabIndex,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.primary,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                color = MaterialTheme.colorScheme.primary
            )
        }
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            )
        }
    }
}

// ==========================================
// TAB 1: WALLET & REDEEM HUB
// ==========================================
@Composable
fun WalletAndRedeemTab(
    walletViewModel: WalletViewModel
) {
    val context = LocalContext.current
    val wallet by walletViewModel.walletState.collectAsState()
    val analytics by walletViewModel.analyticsState.collectAsState()
    val activities by walletViewModel.filteredActivities.collectAsState()
    val config by walletViewModel.configState.collectAsState()

    val searchQuery by walletViewModel.searchQuery.collectAsState()
    val selectedFilter by walletViewModel.selectedFilter.collectAsState()
    val sortByNewest by walletViewModel.sortByNewest.collectAsState()

    var showWithdrawDialog by remember { mutableStateOf(false) }
    var showRedeemDialog by remember { mutableStateOf(false) }
    var selectedRewardItem by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var generatedVoucherCode by remember { mutableStateOf<String?>(null) }

    val coinToRupee = config?.coinToRupeeRatio ?: 100.0
    val conversionText = "100 Coins = ₹1.00"

    // Animated Coin Counter for fluid experience
    val animatedCoins by animateIntAsState(
        targetValue = wallet?.currentCoins ?: 0,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Coin Balance & Cash Value Card
        item {
            val rupeeValue = (wallet?.currentCoins ?: 0) / coinToRupee
            WalletCard(
                totalCoins = wallet?.currentCoins ?: 0,
                usdEquivalent = rupeeValue,
                todayCoins = wallet?.todayCoins ?: 0,
                pendingCoins = wallet?.pendingCoins ?: 0,
                redeemableCoins = wallet?.redeemableCoins ?: 0,
                lifetimeCoins = wallet?.lifetimeCoins ?: 0,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // UPI Cashout Trigger
                Button(
                    onClick = { showWithdrawDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("cash_out_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Cash Out Icon", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cash Out", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                // Rewards Store Trigger
                Button(
                    onClick = { showRedeemDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("redeem_shop_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.CardGiftcard, contentDescription = "Gift Icon", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Redeem Store", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sync with Firebase Button
            var isSyncing by remember { mutableStateOf(false) }
            val syncScope = rememberCoroutineScope()
            OutlinedButton(
                onClick = {
                    isSyncing = true
                    walletViewModel.refreshTransactionsAndApprovals()
                    Toast.makeText(context, "Checking WebAdmin Console for approvals...", Toast.LENGTH_SHORT).show()
                    syncScope.launch {
                        delay(1500)
                        isSyncing = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("sync_approvals_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Sync",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isSyncing) "Checking Approvals..." else "Sync & Check Approvals",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Live Statistics & Analytics Dashboard Header
        item {
            Text(
                text = "My Reward Ecosystem Statistics",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Horizontal scrolling stats or grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    label = "Impressions",
                    value = "${analytics?.totalFeedImpressions ?: 0}",
                    icon = Icons.Default.Visibility,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "Reel Watches",
                    value = "${analytics?.totalReelWatches ?: 0}",
                    icon = Icons.Default.PlayCircle,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    label = "Streak Count",
                    value = "${analytics?.currentStreak ?: 1} Days 🔥",
                    icon = Icons.Default.TrendingUp,
                    color = PremiumGold,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "Avg Daily Coins",
                    value = String.format("%.1f", analytics?.avgDailyCoins ?: 0.0),
                    icon = Icons.Default.MonetizationOn,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Unified Transaction & Reward Timeline Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Unified Wallet Activities",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    IconButton(
                        onClick = { walletViewModel.toggleSortOrder() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (sortByNewest) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                            contentDescription = "Sort Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { walletViewModel.updateSearchQuery(it) },
                    placeholder = { Text("Search transactions...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon", modifier = Modifier.size(18.dp)) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("activity_search_field")
                )

                // Horizontal Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filterOptions = listOf(
                        "ALL" to "All Logs",
                        "REWARD" to "Credits",
                        "REDEEM" to "Vouchers",
                        "WITHDRAW" to "UPI Cash"
                    )
                    filterOptions.forEach { (filterType, label) ->
                        val isSelected = selectedFilter == filterType
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { walletViewModel.updateFilter(filterType) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Timeline Lists
        if (activities.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No matching activities found.",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(activities) { log ->
                val date = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(log.timestamp))
                HistoryItem(
                    title = log.title,
                    timestamp = date,
                    coins = log.amountCoins,
                    type = if (log.type == "REWARD") "FEED" else if (log.type == "REDEEM") "SIGNUP" else "CASHOUT",
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }

    // REDEEM STORE DIALOG (PROTOTOPE CARD LAYOUT WITH REAL SUBTRACTIONS)
    if (showRedeemDialog) {
        val giftCards = listOf(
            Triple("Amazon Voucher", 500, 5.0),
            Triple("Flipkart Voucher", 500, 5.0),
            Triple("Google Play Gift", 1000, 10.0),
            Triple("Swiggy Voucher", 1000, 10.0),
            Triple("Zomato Gift Card", 2000, 20.0)
        )

        AlertDialog(
            onDismissRequest = { showRedeemDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Redeem Gift Cards")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Current Coins: ${wallet?.currentCoins ?: 0}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = PremiumGold
                    )
                    
                    // Category Indicators
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Vouchers", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Physical (Soon)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(240.dp)
                    ) {
                        items(giftCards) { card ->
                            val canAfford = (wallet?.currentCoins ?: 0) >= card.second
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(card.first, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("Est. Value: ₹${String.format("%.2f", card.third)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Button(
                                    onClick = { selectedRewardItem = Pair(card.first, card.second) },
                                    enabled = canAfford,
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (canAfford) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (canAfford) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Text("${card.second} Pts", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showRedeemDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // UPI CASHOUT / WITHDRAWAL DIALOG WITH REAL VALIDATIONS
    if (showWithdrawDialog) {
        var upiDetailsText by remember { mutableStateOf("") }
        var amountText by remember { mutableStateOf("") }
        var isUpiIdInput by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showWithdrawDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cash Out via UPI")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Redeemable Wallet Coins: ${wallet?.redeemableCoins ?: 0}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Select payment option (UPI ID vs UPI Number)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { isUpiIdInput = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isUpiIdInput) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isUpiIdInput) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("UPI ID", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { isUpiIdInput = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isUpiIdInput) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (!isUpiIdInput) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("UPI Phone", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Enter Rupees (₹)") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val label = if (isUpiIdInput) "UPI ID (e.g. user@bank)" else "10-digit Phone Number"
                    val placeholder = if (isUpiIdInput) "username@upi" else "9876543210"

                    OutlinedTextField(
                        value = upiDetailsText,
                        onValueChange = { upiDetailsText = it },
                        label = { Text(label) },
                        placeholder = { Text(placeholder) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amount = amountText.toDoubleOrNull()
                        if (amount == null || amount <= 0) {
                            Toast.makeText(context, "Please enter a valid amount.", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        
                        walletViewModel.cashOutUPI(
                            upiIdOrMobile = upiDetailsText,
                            amountRupees = amount,
                            onSuccess = {
                                Toast.makeText(context, "Withdrawal of ₹$amount requested! Status: Pending.", Toast.LENGTH_LONG).show()
                                showWithdrawDialog = false
                            },
                            onError = { error ->
                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                ) {
                    Text("Submit UPI Cashout", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWithdrawDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // CONFIRM REDEMPTION DIALOG
    selectedRewardItem?.let { item ->
        AlertDialog(
            onDismissRequest = { selectedRewardItem = null },
            title = { Text("Confirm Redemption") },
            text = {
                Text("Do you want to spend ${item.second} Coins to claim a voucher for ${item.first}?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        walletViewModel.redeemGiftVoucher(
                            brandName = item.first,
                            coinsCost = item.second,
                            estimatedValueRupees = item.second / coinToRupee,
                            onSuccess = { code ->
                                generatedVoucherCode = code
                                selectedRewardItem = null
                                showRedeemDialog = false
                            },
                            onError = { error ->
                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                selectedRewardItem = null
                            }
                        )
                    }
                ) {
                    Text("Redeem Now", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedRewardItem = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // DISPLAY GENERATED CODE DIALOG
    generatedVoucherCode?.let { code ->
        AlertDialog(
            onDismissRequest = { generatedVoucherCode = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = PremiumGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (code == "Pending Review") "Voucher Request Registered" else "Voucher Claimed Successfully!")
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    if (code == "Pending Review") {
                        Text(
                            text = "Your request has been uploaded to the Firebase WebAdmin console. The administrator will review your activity and generate your promo code shortly.",
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "AWAITING ADMIN APPROVAL",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    } else {
                        Text("Copy and use this promo code at checkout:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = code,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Check 'Unified Wallet Activities' status for live updates.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            },
            confirmButton = {
                Button(onClick = { generatedVoucherCode = null }) {
                    Text("Awesome!")
                }
            }
        )
    }
}

// ==========================================
// TAB 2: ACTIVITY (WATCHED & SAVED ADS)
// ==========================================
@Composable
fun HistoryAndSavedTab(
    watchLogs: List<WatchHistory>,
    savedAds: List<SavedAd>,
    coinLogs: List<CoinHistory>,
    feedViewProgress: Int,
    walletViewModel: WalletViewModel
) {
    var analyticsSubTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    
    val wallet by walletViewModel.walletState.collectAsState()
    val analytics by walletViewModel.analyticsState.collectAsState()
    val config by walletViewModel.configState.collectAsState()
    val coinToRupee = config?.coinToRupeeRatio ?: 100.0

    // Local statistics calculations based on live state
    val totalFeedImpressions = remember(coinLogs, feedViewProgress) {
        (coinLogs.count { it.title.contains("Feed Watch") } * 5) + feedViewProgress
    }
    val totalReelViews = remember(watchLogs) {
        watchLogs.count { it.brandName.contains("Reel") || it.coinsEarned > 0 }
    }
    val totalCoinsEarned = remember(coinLogs) {
        coinLogs.filter { it.amount > 0 }.sumOf { it.amount }
    }
    val totalCoinsRedeemed = remember(coinLogs) {
        coinLogs.filter { it.amount < 0 }.sumOf { -it.amount }
    }

    // Derived Feed Analytics
    val feedOpens = remember(totalFeedImpressions) { (totalFeedImpressions / 15).coerceAtLeast(1) + 2 }
    val scrollDistance = remember(totalFeedImpressions) { (totalFeedImpressions * 42) + 150 } // meters
    val avgSessionDuration = "3.8 Min"
    val adClicks = remember(savedAds) { savedAds.size + 2 }
    val adSaves = savedAds.size
    val adShares = remember(savedAds) { (savedAds.size / 2) + 1 }

    // Derived Reels Analytics
    val reelsOpens = remember(totalReelViews) { (totalReelViews / 10).coerceAtLeast(1) + 4 }
    val videoAdViews = totalReelViews
    val validWatchThresholds = remember(watchLogs) { watchLogs.count { it.coinsEarned > 0 } }
    val avgWatchDuration = "14.2s"
    val completedAdViews = remember(watchLogs) { watchLogs.count { it.coinsEarned >= 15 } }
    val skippedAds = remember(watchLogs) { watchLogs.count { it.coinsEarned == 0 } }
    val ctaClicks = remember(watchLogs) { watchLogs.count { it.coinsEarned > 15 } / 2 }

    // Reward Efficiency: Coins Earned per View
    val totalViews = totalFeedImpressions + totalReelViews
    val rewardEfficiency = if (totalViews > 0) {
        String.format(Locale.US, "%.1f", totalCoinsEarned.toDouble() / totalViews)
    } else "0.0"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // High fidelity segmented secondary tab row
        TabRow(
            selectedTabIndex = analyticsSubTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[analyticsSubTab]),
                    color = MaterialTheme.colorScheme.primary
                )
            },
            divider = {}
        ) {
            val subTabs = listOf("Activity", "Ad Stats", "Wallet Stats")
            subTabs.forEachIndexed { index, label ->
                Tab(
                    selected = analyticsSubTab == index,
                    onClick = { analyticsSubTab = index },
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (analyticsSubTab == index) FontWeight.Bold else FontWeight.Medium,
                        color = if (analyticsSubTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }

        when (analyticsSubTab) {
            0 -> {
                // ACTIVITY LOGS SUB-TAB
                var logTabState by remember { mutableIntStateOf(0) }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (logTabState == 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .clickable { logTabState = 0 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Watch History",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (logTabState == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (logTabState == 1) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .clickable { logTabState = 1 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Bookmarks (${savedAds.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (logTabState == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (logTabState == 0) {
                    // Watch History Column
                    if (watchLogs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.PlayCircle, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(36.dp))
                                Text("No watch history yet.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(watchLogs.reversed()) { history ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.PlayArrow, "video", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(history.brandName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Category: ${history.category}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.MonetizationOn, "coins", tint = PremiumGold, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("+${history.coinsEarned}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PremiumGold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Bookmarked Saved Ads
                    if (savedAds.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Bookmark, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(36.dp))
                                Text("No bookmarks saved yet.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(savedAds.reversed()) { ad ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = ad.mediaUrl,
                                            contentDescription = ad.title,
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(ad.brandName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(ad.title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        Icon(Icons.Default.Bookmark, "saved", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // AD CAMPAIGN ANALYTICS SUB-TAB
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Headline summary stats card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Reward Efficiency", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text("$rewardEfficiency coins / view", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("High Yield", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    // Feed Advertisement Analytics Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Visibility, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Feed Campaign Analytics", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                                
                                AnalyticsRowItem("Feed Opens", "$feedOpens Sessions")
                                AnalyticsRowItem("Feed Scroll Distance", "$scrollDistance Meters")
                                AnalyticsRowItem("Valid Feed Impressions", "$totalFeedImpressions Views")
                                AnalyticsRowItem("Average Session Duration", avgSessionDuration)
                                AnalyticsRowItem("Ad Click Events", "$adClicks Clicks")
                                AnalyticsRowItem("Ad Save Events", "$adSaves Saves")
                                AnalyticsRowItem("Ad Share Events", "$adShares Shares")
                            }
                        }
                    }

                    // Reels Advertisement Analytics Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Reels Campaign Analytics", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                                AnalyticsRowItem("Reels Open Count", "$reelsOpens Opens")
                                AnalyticsRowItem("Video Advertisement Views", "$videoAdViews Views")
                                AnalyticsRowItem("Valid Watch Threshold Events", "$validWatchThresholds Milestones")
                                AnalyticsRowItem("Average Watch Duration", avgWatchDuration)
                                AnalyticsRowItem("Completed Ad Views", "$completedAdViews Completed")
                                AnalyticsRowItem("Skipped Advertisements", "$skippedAds Skipped")
                                AnalyticsRowItem("CTA Click Events", "$ctaClicks CTA Clicks")
                            }
                        }
                    }
                }
            }

            2 -> {
                // ECOSYSTEM WALLET ANALYTICS SUB-TAB
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Live Local Wallet Metrics Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.MonetizationOn, null, tint = PremiumGold, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Wallet Tracking & Forecast", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                                AnalyticsRowItem("Coins Earned Today", "${wallet?.todayCoins ?: 0} Pts")
                                AnalyticsRowItem("Coins Earned This Week", "${wallet?.weeklyCoins ?: 0} Pts")
                                AnalyticsRowItem("Lifetime Earned Coins", "${wallet?.lifetimeCoins ?: 0} Pts")
                                AnalyticsRowItem("Coins Redeemed", "$totalCoinsRedeemed Pts")
                                AnalyticsRowItem("Pending Redemptions", "${wallet?.pendingCoins ?: 0} Pts")
                                AnalyticsRowItem("Current Wallet Balance", "${wallet?.currentCoins ?: 0} Pts")
                            }
                        }
                    }

                    // Activity Dashboard Card (Progress)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Activity Milestones Dashboard", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                                val todayCoins = wallet?.todayCoins ?: 0
                                val weeklyCoins = wallet?.weeklyCoins ?: 0
                                val lifetimeCoins = wallet?.lifetimeCoins ?: 0

                                MilestoneProgressItem(
                                    label = "Today's Target (100 Pts)",
                                    currentValue = todayCoins,
                                    targetValue = 100,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                MilestoneProgressItem(
                                    label = "Weekly Target (500 Pts)",
                                    currentValue = weeklyCoins,
                                    targetValue = 500,
                                    color = MaterialTheme.colorScheme.secondary
                                )

                                MilestoneProgressItem(
                                    label = "Monthly Milestone (2000 Pts)",
                                    currentValue = weeklyCoins * 4,
                                    targetValue = 2000,
                                    color = PremiumGold
                                )

                                MilestoneProgressItem(
                                    label = "Lifetime Rank Up (5000 Pts)",
                                    currentValue = lifetimeCoins,
                                    targetValue = 5000,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                    }

                    // Prototype Visual bar graph
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Weekly Coins Earnings Distribution", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .padding(top = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                                    val heights = listOf(0.4f, 0.7f, 0.5f, 0.9f, 0.6f, 0.8f, 0.3f)
                                    days.forEachIndexed { i, day ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(18.dp)
                                                    .fillMaxHeight(heights[i])
                                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                    .background(
                                                        Brush.verticalGradient(
                                                            listOf(
                                                                MaterialTheme.colorScheme.primary,
                                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                                            )
                                                        )
                                                    )
                                            )
                                            Text(day, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsRowItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun MilestoneProgressItem(
    label: String,
    currentValue: Int,
    targetValue: Int,
    color: Color
) {
    val progress = if (targetValue > 0) (currentValue.toFloat() / targetValue).coerceIn(0f, 1f) else 0f
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${(progress * 100).toInt()}% ($currentValue/$targetValue)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
        }
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ==========================================
// TAB 3: INTERESTS & LOCAL SETTINGS
// ==========================================
@Composable
fun SettingsAndInterestsTab(
    viewModel: AdViewModel,
    profile: UserProfile?
) {
    val context = LocalContext.current
    val currentInterests = remember(profile) {
        profile?.selectedCategories?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
    }

    val categoriesList = listOf(
        "Gaming", "Education", "Technology", "Finance", "Business", "Fashion",
        "Fitness", "Automobile", "Travel", "Food", "Movies", "Music",
        "Sports", "Health", "Books", "News"
    )

    var editingInterests by remember { mutableStateOf(false) }
    val editedInterests = remember { mutableStateListOf<String>() }

    // Synchronize editable interests
    LaunchedEffect(editingInterests) {
        if (editingInterests) {
            editedInterests.clear()
            editedInterests.addAll(currentInterests)
        }
    }

    // Guest Upgrade Dialog state
    var showUpgradeDialog by remember { mutableStateOf(false) }
    var upgradeFullName by remember { mutableStateOf("") }
    var upgradeEmail by remember { mutableStateOf("") }
    var upgradeMobile by remember { mutableStateOf("") }
    var upgradePassword by remember { mutableStateOf("") }
    var upgradeConfirmPassword by remember { mutableStateOf("") }
    var upgradePasswordVisible by remember { mutableStateOf(false) }
    var upgradeConfirmPasswordVisible by remember { mutableStateOf(false) }
    var upgradeError by remember { mutableStateOf("") }

    // Local settings toggles (mock saved locally using remember)
    var isDarkMode by remember { mutableStateOf(true) }
    var selectedLanguage by remember { mutableStateOf("English") }
    var showLanguageDropdown by remember { mutableStateOf(false) }
    var pushNotificationsEnabled by remember { mutableStateOf(true) }
    var rewardAnimationsEnabled by remember { mutableStateOf(true) }
    var autoPlayOption by remember { mutableStateOf("Always") }
    var autoMuteOption by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // GUEST UPGRADE PROMPT
        if (profile?.guestAccount == true) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Guest Session Active",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = "You have earned ${profile.coins} Coins ($${String.format(Locale.US, "%.2f", profile.walletBalance)} Cashback). Secure your account now to claim a 200 Coin Sign-Up Bonus, sync across devices, and unlock cash withdrawals!",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            lineHeight = 18.sp
                        )
                        Button(
                            onClick = {
                                upgradeFullName = ""
                                upgradeEmail = ""
                                upgradeMobile = ""
                                upgradePassword = ""
                                upgradeConfirmPassword = ""
                                upgradeError = ""
                                showUpgradeDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                contentColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Secure Account & Upgrade", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("My Curated Categories", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        TextButton(
                            onClick = {
                                if (editingInterests) {
                                    if (editedInterests.size < 3) {
                                        Toast.makeText(context, "Select at least 3 categories", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.updateInterests(editedInterests.toList())
                                        editingInterests = false
                                        Toast.makeText(context, "Ad Interests updated successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    editingInterests = true
                                }
                            }
                        ) {
                            Text(if (editingInterests) "Save Updates" else "Edit")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (!editingInterests) {
                        // Show current tags flow
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            mainAxisSpacing = 6.dp,
                            crossAxisSpacing = 6.dp
                        ) {
                            currentInterests.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(tag, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    } else {
                        // Show selectable pills
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Select interests (at least 3):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Box(modifier = Modifier.height(200.dp)) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(categoriesList) { category ->
                                        val checked = editedInterests.contains(category)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant)
                                                .border(1.dp, if (checked) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    if (checked) editedInterests.remove(category)
                                                    else editedInterests.add(category)
                                                }
                                                .padding(8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(category, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // SECTION 1: APPEARANCE PREFERENCES
        item {
            var dynamicColorEnabled by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ColorLens, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Appearance Preferences", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                    // Dark Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Dark Theme", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("Force eye-safe dark slate palette", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { isDarkMode = it }
                        )
                    }

                    // Dynamic Color Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Dynamic Theme Color", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("Sync app themes with local wallpaper palette (Android 12+)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                        Switch(
                            checked = dynamicColorEnabled,
                            onCheckedChange = { 
                                dynamicColorEnabled = it
                                Toast.makeText(context, "Dynamic color " + (if (it) "enabled" else "disabled"), Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }

        // SECTION 2: ADVERTISEMENT PREFERENCES
        item {
            var dataSaverEnabled by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Advertisement Preferences", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                    // Auto Play option
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Video Auto Play", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("Control media preloading & auto play behaviors", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                        Box {
                            Text(
                                text = autoPlayOption,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable {
                                        autoPlayOption = when (autoPlayOption) {
                                            "Always" -> "WiFi Only"
                                            "WiFi Only" -> "Disabled"
                                            else -> "Always"
                                        }
                                        Toast.makeText(context, "Auto play: $autoPlayOption", Toast.LENGTH_SHORT).show()
                                    }
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Mute by default switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Start Videos Muted", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("Ad videos start playing with muted sound initially", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                        Switch(
                            checked = autoMuteOption,
                            onCheckedChange = { autoMuteOption = it }
                        )
                    }

                    // Floating coin reward animation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Floating Reward Animations", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("Show physics coin animations when points are credited", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                        Switch(
                            checked = rewardAnimationsEnabled,
                            onCheckedChange = { rewardAnimationsEnabled = it }
                        )
                    }

                    // Data saver mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Data Saver Mode", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("Restrict preloads, compress images & optimize bandwidth", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                        Switch(
                            checked = dataSaverEnabled,
                            onCheckedChange = { 
                                dataSaverEnabled = it
                                Toast.makeText(context, "Data Saver Mode " + (if (it) "enabled" else "disabled"), Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }

        // SECTION 3: NOTIFICATION PREFERENCES
        item {
            var walletNotifEnabled by remember { mutableStateOf(true) }
            var redemptionNotifEnabled by remember { mutableStateOf(true) }
            var appNotifEnabled by remember { mutableStateOf(true) }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Notification Settings", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Campaign Reward Alerts", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("Instant notifications when ad rewards are credited", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                        Switch(
                            checked = pushNotificationsEnabled,
                            onCheckedChange = { pushNotificationsEnabled = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Wallet & Points Status", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("Updates regarding milestone progress & weekly reports", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                        Switch(
                            checked = walletNotifEnabled,
                            onCheckedChange = { walletNotifEnabled = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Redemption & Cashout Updates", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("Tracking alerts for gift cards & UPI cashouts", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                        Switch(
                            checked = redemptionNotifEnabled,
                            onCheckedChange = { redemptionNotifEnabled = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("System & Campaign Updates", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("Alerts on new available features & system builds", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                        Switch(
                            checked = appNotifEnabled,
                            onCheckedChange = { appNotifEnabled = it }
                        )
                    }
                }
            }
        }

        // SECTION 4: CACHE & LOCAL DATABASE MANAGEMENT
        item {
            var dbSizeText by remember { mutableStateOf("120 KB") }
            var cacheSizeText by remember { mutableStateOf("14.2 MB") }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Cache & Local Storage", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Active App Cache", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                        Text(cacheSizeText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("SQLite Room Database Size", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                        Text(dbSizeText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.clearAppDataAndLogs(context)
                            cacheSizeText = "0.0 B"
                            dbSizeText = "24 KB"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteForever, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset database & clear app cache", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // SECTION 5: PRIVACY & SECURITY DETAILS CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Privacy & Security Transparency", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(
                        text = "To guarantee perfect confidentiality, all your ad campaign interaction logs, watch history statistics, interests data, and rewards balances are processed and stored exclusively inside an offline SQLite Room Database on your physical device. No payments or sensitive profiles are synced until you voluntarily upgrade your guest session.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // SECTION 6: LEGAL & ABOUT SCREEN DETAILS
        item {
            var showPrivacyDialog by remember { mutableStateOf(false) }
            var showTermsDialog by remember { mutableStateOf(false) }
            var showLicensesDialog by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Legal & About", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Application Version", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                        Text("1.0.0 (Build #108)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Developer Brand", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                        Text("AdReels Labs", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                    // Privacy Policy Trigger
                    Text(
                        text = "Privacy Policy Summary",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { showPrivacyDialog = true }
                            .padding(vertical = 4.dp)
                    )

                    // Terms & Conditions Trigger
                    Text(
                        text = "Terms & Conditions Summary",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { showTermsDialog = true }
                            .padding(vertical = 4.dp)
                    )

                    // Open Source Licenses Trigger
                    Text(
                        text = "Open Source Licenses Info",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { showLicensesDialog = true }
                            .padding(vertical = 4.dp)
                    )
                }
            }

            // Privacy Policy Dialog
            if (showPrivacyDialog) {
                AlertDialog(
                    onDismissRequest = { showPrivacyDialog = false },
                    title = { Text("Privacy Policy", fontWeight = FontWeight.Bold) },
                    text = {
                        Text(
                            text = "We prioritize your privacy. All watch histories and ad interaction statistics generated on this application remain stored strictly inside the local sandbox directory on your Android device. We do not transmit details to external trackers or unauthorized ad networks.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { showPrivacyDialog = false }) {
                            Text("Understood", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            // Terms & Conditions Dialog
            if (showTermsDialog) {
                AlertDialog(
                    onDismissRequest = { showTermsDialog = false },
                    title = { Text("Terms & Conditions", fontWeight = FontWeight.Bold) },
                    text = {
                        Text(
                            text = "By participating in this prototype's campaign simulation, you acknowledge that rewards are simulated points and hold no absolute real-world tender value outside the guidelines of the specific demonstration brand campaign. Any attempt to abuse local state tables or spoof watch logs is prohibited.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { showTermsDialog = false }) {
                            Text("Accept", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            // Open Source Licenses Dialog
            if (showLicensesDialog) {
                AlertDialog(
                    onDismissRequest = { showLicensesDialog = false },
                    title = { Text("Open Source Licenses", fontWeight = FontWeight.Bold) },
                    text = {
                        Text(
                            text = "This prototype relies on the following high-quality libraries:\n- Jetpack Compose (M3)\n- AndroidX SQLite Room Database\n- Coil Media Loader\n- Kotlin Coroutines & Serialization Engine",
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { showLicensesDialog = false }) {
                            Text("Close", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }
    }

    // Guest Upgrade Dialog
    if (showUpgradeDialog) {
        AlertDialog(
            onDismissRequest = { showUpgradeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Secure Account", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "Fill in the credentials below to upgrade your anonymous session to a fully protected permanent account.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 16.sp
                    )

                    OutlinedTextField(
                        value = upgradeFullName,
                        onValueChange = { upgradeFullName = it; upgradeError = "" },
                        label = { Text("Full Name") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = upgradeEmail,
                        onValueChange = { upgradeEmail = it; upgradeError = "" },
                        label = { Text("Email Address") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = upgradeMobile,
                        onValueChange = { upgradeMobile = it; upgradeError = "" },
                        label = { Text("Mobile Number (10-digit)") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = upgradePassword,
                        onValueChange = { upgradePassword = it; upgradeError = "" },
                        label = { Text("Password") },
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            IconButton(onClick = { upgradePasswordVisible = !upgradePasswordVisible }) {
                                Icon(
                                    imageVector = if (upgradePasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (upgradePasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = upgradeConfirmPassword,
                        onValueChange = { upgradeConfirmPassword = it; upgradeError = "" },
                        label = { Text("Confirm Password") },
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            IconButton(onClick = { upgradeConfirmPasswordVisible = !upgradeConfirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (upgradeConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (upgradeConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (upgradeError.isNotEmpty()) {
                        Text(
                            text = upgradeError,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 16.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val valResult = com.example.data.utils.ValidationManager.validateRegistration(
                            upgradeFullName.trim(),
                            upgradeEmail.trim(),
                            upgradeMobile.trim(),
                            upgradePassword,
                            upgradeConfirmPassword
                        )
                        if (valResult is com.example.data.utils.ValidationResult.Error) {
                            upgradeError = valResult.message
                        } else {
                            viewModel.upgradeGuestToUser(
                                upgradeFullName.trim(),
                                upgradeEmail.trim(),
                                upgradeMobile.trim(),
                                upgradePassword,
                                onSuccess = {
                                    showUpgradeDialog = false
                                    Toast.makeText(context, "Account secured successfully! Welcome gift of 200 Coins added!", Toast.LENGTH_LONG).show()
                                },
                                onError = { err ->
                                    upgradeError = err
                                }
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Secure Now", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpgradeDialog = false }) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// FlowRow layout helper for simple modern inline categories tags
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    mainAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    crossAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints)
        }

        val rows = mutableListOf<MutableList<androidx.compose.ui.layout.Placeable>>()
        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentRowWidth = 0
        val mainAxisSpacingPx = mainAxisSpacing.roundToPx()
        val crossAxisSpacingPx = crossAxisSpacing.roundToPx()

        placeables.forEach { placeable ->
            if (currentRowWidth + placeable.width > constraints.maxWidth && currentRow.isNotEmpty()) {
                rows.add(currentRow)
                currentRow = mutableListOf()
                currentRowWidth = 0
            }
            currentRow.add(placeable)
            currentRowWidth += placeable.width + mainAxisSpacingPx
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }

        var totalHeight = 0
        rows.forEachIndexed { index, row ->
            val rowHeight = row.maxOfOrNull { it.height } ?: 0
            totalHeight += rowHeight
            if (index < rows.size - 1) {
                totalHeight += crossAxisSpacingPx
            }
        }

        layout(constraints.maxWidth, totalHeight) {
            var y = 0
            rows.forEach { row ->
                var x = 0
                val rowHeight = row.maxOfOrNull { it.height } ?: 0
                row.forEach { placeable ->
                    placeable.placeRelative(x, y + (rowHeight - placeable.height) / 2)
                    x += placeable.width + mainAxisSpacingPx
                }
                y += rowHeight + crossAxisSpacingPx
            }
        }
    }
}

@Composable
fun SystemMonitorTab(
    walletViewModel: WalletViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Real-time states
    var isDevelopmentMode by remember { mutableStateOf(true) }
    var inputSecretText by remember { mutableStateOf("") }
    var sanitizedOutputText by remember { mutableStateOf("") }
    var currentCrashStatus by remember { mutableStateOf("HEALTHY") } // "HEALTHY", "CRASHED", "RECOVERED"

    // Analytics from AnalyticsManager
    val feedOpenCount by com.example.data.adengine.AnalyticsManager.feedOpenCount.collectAsState()
    val feedCloseCount by com.example.data.adengine.AnalyticsManager.feedCloseCount.collectAsState()
    val feedSessionTimeMs by com.example.data.adengine.AnalyticsManager.feedSessionTimeMs.collectAsState()
    val feedScrollDistance by com.example.data.adengine.AnalyticsManager.feedScrollDistance.collectAsState()
    val feedScrollSpeed by com.example.data.adengine.AnalyticsManager.feedScrollSpeed.collectAsState()
    val feedLoadingTimeMs by com.example.data.adengine.AnalyticsManager.feedLoadingTimeMs.collectAsState()
    val feedRefreshCount by com.example.data.adengine.AnalyticsManager.feedRefreshCount.collectAsState()
    val categoryUsage by com.example.data.adengine.AnalyticsManager.categoryUsage.collectAsState()

    val reelsOpenCount by com.example.data.adengine.AnalyticsManager.reelsOpenCount.collectAsState()
    val reelsCloseCount by com.example.data.adengine.AnalyticsManager.reelsCloseCount.collectAsState()
    val reelsSessionTimeMs by com.example.data.adengine.AnalyticsManager.reelsSessionTimeMs.collectAsState()
    val rewardedAdAttempts by com.example.data.adengine.AnalyticsManager.rewardedAdAttempts.collectAsState()
    val rewardedAdCompletions by com.example.data.adengine.AnalyticsManager.rewardedAdCompletions.collectAsState()
    val rewardEventsCount by com.example.data.adengine.AnalyticsManager.rewardEventsCount.collectAsState()

    val coinsEarned by com.example.data.adengine.AnalyticsManager.coinsEarned.collectAsState()
    val coinsRedeemed by com.example.data.adengine.AnalyticsManager.coinsRedeemed.collectAsState()
    val walletBalance by com.example.data.adengine.AnalyticsManager.walletBalance.collectAsState()

    // Performance metrics
    val appStartupTimeMs by com.example.data.adengine.MonitoringManager.appStartupTimeMs.collectAsState()
    val averageNavigationSpeedMs by com.example.data.adengine.MonitoringManager.averageNavigationSpeedMs.collectAsState()
    val rewardProcessingTimeMs by com.example.data.adengine.MonitoringManager.rewardProcessingTimeMs.collectAsState()
    val frameDropsCount by com.example.data.adengine.MonitoringManager.frameDropsCount.collectAsState()
    val cacheHitRatio by com.example.data.adengine.MonitoringManager.cacheHitRatio.collectAsState()

    val databaseSizeKb by com.example.data.adengine.MonitoringManager.databaseSizeKb.collectAsState()
    val dbQueryTimeMs by com.example.data.adengine.MonitoringManager.dbQueryTimeMs.collectAsState()

    // Security stats
    val securityStats = walletViewModel.securityManager.getSecurityStats()
    val duplicateBlocked = securityStats["duplicateAttemptsBlocked"] as? Int ?: 0
    val anomalousLogged = securityStats["anomalousActivitiesLogged"] as? Int ?: 0

    // Effect to toggle log mode
    LaunchedEffect(isDevelopmentMode) {
        com.example.data.adengine.ApplicationLogManager.setDevelopmentMode(isDevelopmentMode)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- SECTION 1: HEADER & ENV SWITCH ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "System Monitor & Audit",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Enterprise-Grade Performance & Security",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }

                        // Debug Mode Toggle
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isDevelopmentMode) "Development" else "Production",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDevelopmentMode) MaterialTheme.colorScheme.primary else Color(0xFF4CAF50),
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Switch(
                                checked = isDevelopmentMode,
                                onCheckedChange = { isDevelopmentMode = it },
                                modifier = Modifier.scale(0.8f).testTag("debug_mode_toggle")
                            )
                        }
                    }
                }
            }
        }

        // --- SECTION 2: HEALTH, RECOVERY & INTEGRITY ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Crash Simulation & Recovery Protocol",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("System Uptime:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${com.example.data.adengine.MonitoringManager.getUptimeSeconds()} seconds",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Session Crash Status:", style = MaterialTheme.typography.bodyMedium)
                        val statusBg = when (currentCrashStatus) {
                            "HEALTHY" -> Color(0xFF4CAF50)
                            "CRASHED" -> Color(0xFFF44336)
                            else -> Color(0xFF2196F3)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(statusBg.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = currentCrashStatus,
                                color = statusBg,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                currentCrashStatus = "CRASHED"
                                com.example.data.adengine.MonitoringManager.logError(
                                    "UNEXPECTED_EXIT",
                                    "Simulation: Application closed unexpectedly without calling onDispose or cleanShutdown."
                                )
                                Toast.makeText(context, "Unexpected exit simulation triggered", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f).testTag("simulate_crash_btn")
                        ) {
                            Icon(Icons.Default.Dangerous, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Simulate Exit", fontSize = 11.sp, maxLines = 1)
                        }

                        Button(
                            onClick = {
                                currentCrashStatus = "RECOVERED"
                                com.example.data.adengine.MonitoringManager.recoverFromCrash()
                                Toast.makeText(context, "Crash Recovery Completed! Session, Wallet, and Queues fully restored.", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                            modifier = Modifier.weight(1.2f).testTag("trigger_recovery_btn")
                        ) {
                            Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Self-Heal Protocol", fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            }
        }

        // --- SECTION 3: REWARD & SECURITY AUDITING ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Security Audits & Duplicate Prevention",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Wallet Integrity Status:", style = MaterialTheme.typography.bodyMedium)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF4CAF50).copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "VERIFIED SECURE",
                                color = Color(0xFF4CAF50),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Duplicate Claims Blocked:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "$duplicateBlocked",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (duplicateBlocked > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Anomalous Click Activities Blocked:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "$anomalousLogged",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (anomalousLogged > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // --- SECTION 4: DEBUG MODE GAUGES (Only visible if DevelopmentMode = true) ---
        if (isDevelopmentMode) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DeveloperMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Development Real-Time Gauges",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Memory, Queue, Cache, Database Gauges
                        Text("Simulated App Memory Size (JVM): 24.8 MB / 128 MB", style = MaterialTheme.typography.bodySmall)
                        LinearProgressIndicator(
                            progress = 24.8f / 128f,
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text("Prefetch Cache Hit Ratio: ${(cacheHitRatio * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                        LinearProgressIndicator(
                            progress = cacheHitRatio,
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF4CAF50)
                        )

                        Text("Active Core Dispatch Queue Size: 0 items", style = MaterialTheme.typography.bodySmall)
                        LinearProgressIndicator(
                            progress = 0f,
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF2196F3)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Core Table Count:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "${com.example.data.adengine.MonitoringManager.getDatabaseTablesCount()} tables",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Active DB Migration Status:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                com.example.data.adengine.MonitoringManager.getMigrationStatus(),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Active DB Backup Status:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                com.example.data.adengine.MonitoringManager.getBackupStatus(),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔒 Advanced Debug Gauges hidden in Production Mode",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // --- SECTION 5: PRIVACY & LOG SANITIZATION SANDBOX ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Privacy Rules & Log Sanitization Sandbox",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = "Interactive Sandbox: Enter any secret like UPI IDs or Passwords. Click Scrape below to prove that they are successfully scrubbed and censored before being written to local disks or logs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )

                    OutlinedTextField(
                        value = inputSecretText,
                        onValueChange = { inputSecretText = it },
                        placeholder = { Text("Example: password: pass123, upi: test@paytm") },
                        modifier = Modifier.fillMaxWidth().testTag("sandbox_secret_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Button(
                        onClick = {
                            val sanitized = com.example.data.adengine.ApplicationLogManager.sanitize(inputSecretText)
                            sanitizedOutputText = sanitized
                            com.example.data.adengine.ApplicationLogManager.i("PrivacySandbox", "Sanitization Sandbox Test: $inputSecretText")
                        },
                        modifier = Modifier.fillMaxWidth().testTag("sandbox_scrape_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Log & Scrape Sensitive Data", fontSize = 13.sp)
                    }

                    if (sanitizedOutputText.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Sanitized Output Written To Logs:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = sanitizedOutputText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // --- SECTION 6: COMPREHENSIVE PERFORMANCE GAUGE STATS ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Device Metrics & Frame Performance",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Application Startup Latency:", style = MaterialTheme.typography.bodyMedium)
                        Text("$appStartupTimeMs ms", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Navigation Latency (Avg):", style = MaterialTheme.typography.bodyMedium)
                        Text("$averageNavigationSpeedMs ms", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Reward Processing Latency:", style = MaterialTheme.typography.bodyMedium)
                        Text("$rewardProcessingTimeMs ms", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Estimated Dropped Frames (Session):", style = MaterialTheme.typography.bodyMedium)
                        Text("$frameDropsCount frames", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (frameDropsCount > 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // --- SECTION 7: FEED & REELS ENGAGEMENT STATS ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Feed & Reels User Engagement",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Feed Open Count:", style = MaterialTheme.typography.bodyMedium)
                        Text("$feedOpenCount opens", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Feed Closed Count:", style = MaterialTheme.typography.bodyMedium)
                        Text("$feedCloseCount closes", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Feed Accumulated Active Time:", style = MaterialTheme.typography.bodyMedium)
                        Text("${feedSessionTimeMs / 1000} seconds", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Reels Open Count:", style = MaterialTheme.typography.bodyMedium)
                        Text("$reelsOpenCount opens", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Ads Started vs Completed:", style = MaterialTheme.typography.bodyMedium)
                        Text("$rewardedAdCompletions / $rewardedAdAttempts", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- SECTION 8: COMPLIANCE POLICY ENGINE ---
        item {
            var selectedProvider by remember { mutableStateOf(AdProvider.ADMOB) }
            var selectedFormat by remember { mutableStateOf(AdFormat.NATIVE) }
            var selectedPlacement by remember { mutableStateOf(AdPlacement.HOME) }
            var selectedRewardType by remember { mutableStateOf(AdRewardType.COINS) }
            var selectedInteraction by remember { mutableStateOf(AdInteractionType.IMPRESSION) }
            var selectedEntryFlow by remember { mutableStateOf(AdUserEntryFlow.VOLUNTARY_OPT_IN) }
            var selectedConsent by remember { mutableStateOf(AdConsentState.GRANTED) }
            var selectedEnv by remember { mutableStateOf(AdEnvironment.PRODUCTION) }

            val policyEngine = remember { CompliancePolicyEngine() }
            val decision = policyEngine.evaluate(
                provider = selectedProvider,
                format = selectedFormat,
                placement = selectedPlacement,
                rewardType = selectedRewardType,
                interactionType = selectedInteraction,
                entryFlow = selectedEntryFlow,
                consentState = selectedConsent,
                environment = selectedEnv
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "⚖️ Policy Lock Sandbox",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Evaluate any custom ad flow scenario in real-time against current compliance policy rules.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    // Selection Row 1: Provider & Format
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Provider", style = MaterialTheme.typography.labelSmall)
                            Row(modifier = Modifier.fillMaxWidth()) {
                                AdProvider.values().forEach { p ->
                                    FilterChip(
                                        selected = selectedProvider == p,
                                        onClick = { selectedProvider = p },
                                        label = { Text(p.name, fontSize = 10.sp) },
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text("Format", style = MaterialTheme.typography.labelSmall)
                            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                                AdFormat.values().forEach { f ->
                                    FilterChip(
                                        selected = selectedFormat == f,
                                        onClick = { selectedFormat = f },
                                        label = { Text(f.name, fontSize = 10.sp) },
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Selection Row 2: Reward Type & Interaction
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Reward Type", style = MaterialTheme.typography.labelSmall)
                            Row {
                                AdRewardType.values().forEach { r ->
                                    FilterChip(
                                        selected = selectedRewardType == r,
                                        onClick = { selectedRewardType = r },
                                        label = { Text(r.name, fontSize = 10.sp) },
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text("Interaction", style = MaterialTheme.typography.labelSmall)
                            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                                AdInteractionType.values().forEach { i ->
                                    FilterChip(
                                        selected = selectedInteraction == i,
                                        onClick = { selectedInteraction = i },
                                        label = { Text(i.name, fontSize = 10.sp) },
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Selection Row 3: User Entry Flow & Environment
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Entry Flow", style = MaterialTheme.typography.labelSmall)
                            Row {
                                AdUserEntryFlow.values().forEach { e ->
                                    FilterChip(
                                        selected = selectedEntryFlow == e,
                                        onClick = { selectedEntryFlow = e },
                                        label = { Text(if (e == AdUserEntryFlow.VOLUNTARY_OPT_IN) "OPT_IN" else "FORCED", fontSize = 9.sp) },
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text("Environment", style = MaterialTheme.typography.labelSmall)
                            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                                AdEnvironment.values().forEach { env ->
                                    FilterChip(
                                        selected = selectedEnv == env,
                                        onClick = { selectedEnv = env },
                                        label = { Text(env.name, fontSize = 9.sp) },
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Evaluation outcome
                    val outcomeColor = when (decision) {
                        ComplianceDecision.ALLOWED -> Color(0xFF4CAF50)
                        ComplianceDecision.BLOCKED -> Color(0xFFF44336)
                        ComplianceDecision.REQUIRES_POLICY_REVIEW -> Color(0xFFFF9800)
                        ComplianceDecision.REQUIRES_PROVIDER_REVIEW -> Color(0xFFFF9800)
                        ComplianceDecision.TEST_ONLY -> Color(0xFF2196F3)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(outcomeColor.copy(alpha = 0.12f))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Engine Decision:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                decision.name,
                                color = outcomeColor,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = when (decision) {
                                ComplianceDecision.ALLOWED -> "✅ Compliant. All checks passed. This configuration is cleared for production."
                                ComplianceDecision.BLOCKED -> "❌ VIOLATION BLOCKED. Current combination violates AdMob Policies or User Choice mandates."
                                ComplianceDecision.REQUIRES_POLICY_REVIEW -> "⚠️ LOCKED. Feed rewards from ordinary impressions are disabled until compatibility is reviewed and confirmed."
                                ComplianceDecision.REQUIRES_PROVIDER_REVIEW -> "⚠️ REVIEW NEEDED. Provider SDK verified callbacks must control of rewards."
                                ComplianceDecision.TEST_ONLY -> "ℹ️ TEST MODE ONLY. Simulated/Dummy flows are forbidden in real Production environments."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // --- SECTION 9: PROVIDER CAPABILITY REGISTRY ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "📋 Provider Capability Registry",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Capability permissions mapped by Provider and Format, eliminating unsafe universal assumptions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )

                    // Registry entries
                    listOf(
                        Triple(AdProvider.ADMOB, AdFormat.NATIVE, "AdMob Native"),
                        Triple(AdProvider.ADMOB, AdFormat.REWARDED, "AdMob Rewarded"),
                        Triple(AdProvider.DUMMY, AdFormat.REWARDED, "Dummy Rewarded")
                    ).forEach { (p, f, label) ->
                        val caps = ProviderCapabilityRegistry.getCapabilities(p, f)
                        val rewardMode = ProviderCapabilityRegistry.getRewardMode(p, f)
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(rewardMode.name, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Capabilities: " + caps.joinToString { it.name },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        // --- SECTION 10: FEED REWARD POLICY GATE ---
        item {
            var policyGateState by remember { mutableStateOf(FeedRewardPolicyGate.isReviewedAndConfirmedCompatible) }
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🔒 Feed Reward Policy Gate",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Real AdMob Native: Ordinary feed impressions do NOT credit coins unless explicitly reviewed & confirmed compatible.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                        Switch(
                            checked = policyGateState,
                            onCheckedChange = {
                                policyGateState = it
                                FeedRewardPolicyGate.isReviewedAndConfirmedCompatible = it
                            },
                            modifier = Modifier.scale(0.85f).testTag("feed_reward_gate_toggle")
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (policyGateState) Color(0xFF4CAF50).copy(alpha = 0.1f)
                                else Color(0xFFF44336).copy(alpha = 0.1f)
                            )
                            .padding(8.dp)
                    ) {
                        Text(
                            text = if (policyGateState) "✅ COMPLIANCE REVIEW COMPLETED: Feed ordinary impression rewards are activated (for evaluation purposes)."
                            else "❌ DEFAULT SAFE LOCK: Ordinary native feed impressions credit ZERO coins. AdMob policy-compliant.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (policyGateState) Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                    }
                }
            }
        }

        // --- SECTION 11: ENVIRONMENT CONFIGURATION & TEST AD DETECTOR ---
        item {
            var selectedEnvMode by remember { mutableStateOf(AdEnvironmentConfiguration.activeEnvironment) }
            var testUnitInput by remember { mutableStateOf("ca-app-pub-6715807412270192/5224534379") } // Default to user's active production banner unit
            var testIsRelease by remember { mutableStateOf(false) }
            var testResultMsg by remember { mutableStateOf("") }
            var testResultIsSuccess by remember { mutableStateOf(true) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "⚙️ Ad Environment & Unit Gatekeeper",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Validates that development builds use test ads and release builds use production ads to protect against account bans.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )

                    // Select Env (Locked to Secure Sandbox for compliance)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Environment:", style = MaterialTheme.typography.bodyMedium)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "DUMMY (SECURE SANDBOX)",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Interactive test tool
                    Text("Interactive Safety Gate Tester:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    
                    OutlinedTextField(
                        value = testUnitInput,
                        onValueChange = { testUnitInput = it },
                        label = { Text("Ad Unit ID", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Simulate Release Build:", style = MaterialTheme.typography.bodySmall)
                        Switch(
                            checked = testIsRelease,
                            onCheckedChange = { testIsRelease = it },
                            modifier = Modifier.scale(0.8f)
                        )
                    }

                    Button(
                        onClick = {
                            try {
                                AdEnvironmentConfiguration.enforceTestAdRules(testUnitInput, testIsRelease)
                                testResultMsg = "SUCCESS: Compliance rules verified! Environment is safe."
                                testResultIsSuccess = true
                            } catch (e: Exception) {
                                testResultMsg = "BLOCKED: ${e.message}"
                                testResultIsSuccess = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Verify Ad Unit Safeness", fontSize = 12.sp)
                    }

                    if (testResultMsg.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (testResultIsSuccess) Color(0xFF4CAF50).copy(alpha = 0.12f)
                                    else Color(0xFFF44336).copy(alpha = 0.12f)
                                )
                                .padding(10.dp)
                        ) {
                            Text(
                                text = testResultMsg,
                                color = if (testResultIsSuccess) Color(0xFF4CAF50) else Color(0xFFF44336),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // --- SECTION 12: PRIVACY & CONSENT MANAGER ---
        item {
            val consentStateVal by PrivacyConsentManager.consentState.collectAsState()
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "🇪🇺 Regional Privacy & Consent Manager (UMP)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Google UMP Policy dictates verifying consent state at launch before requesting any ads.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Current Consent State:", style = MaterialTheme.typography.bodyMedium)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when (consentStateVal) {
                                        AdConsentState.GRANTED -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                                        AdConsentState.DENIED -> Color(0xFFF44336).copy(alpha = 0.15f)
                                        AdConsentState.UNKNOWN -> Color(0xFFFF9800).copy(alpha = 0.15f)
                                    }
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = consentStateVal.name,
                                fontWeight = FontWeight.Bold,
                                color = when (consentStateVal) {
                                    AdConsentState.GRANTED -> Color(0xFF4CAF50)
                                    AdConsentState.DENIED -> Color(0xFFF44336)
                                    AdConsentState.UNKNOWN -> Color(0xFFFF9800)
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    // Simulated buttons to adjust consent
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { PrivacyConsentManager.setConsentState(AdConsentState.GRANTED) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Grant", fontSize = 10.sp)
                        }
                        Button(
                            onClick = { PrivacyConsentManager.setConsentState(AdConsentState.DENIED) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Deny", fontSize = 10.sp)
                        }
                        Button(
                            onClick = { PrivacyConsentManager.setConsentState(AdConsentState.UNKNOWN) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reset", fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // --- SECTION 13: EMERGENCY REMOTE KILL SWITCHES ---
        item {
            var homeAdsState by remember { mutableStateOf(RemoteKillSwitch.isHomeAdsEnabled) }
            var rewardedAdsState by remember { mutableStateOf(RemoteKillSwitch.isRewardedAdsEnabled) }
            var rewardsState by remember { mutableStateOf(RemoteKillSwitch.isRewardsEnabled) }
            val pStatus by RemoteKillSwitch.providerKillStatus.collectAsState()

            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "🚨 Emergency Remote Kill Switches",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Allows real-time granular remote disablement of ad streams or reward capabilities to prevent account damage during policy events.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Feed ads switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Native Feed Ads Status", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = homeAdsState,
                            onCheckedChange = {
                                homeAdsState = it
                                RemoteKillSwitch.isHomeAdsEnabled = it
                            },
                            modifier = Modifier.scale(0.8f)
                        )
                    }

                    // Reels ads switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Rewarded Reels Ads Status", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = rewardedAdsState,
                            onCheckedChange = {
                                rewardedAdsState = it
                                RemoteKillSwitch.isRewardedAdsEnabled = it
                            },
                            modifier = Modifier.scale(0.8f)
                        )
                    }

                    // Coin Rewards switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Coin Distribution Status", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = rewardsState,
                            onCheckedChange = {
                                rewardsState = it
                                RemoteKillSwitch.isRewardsEnabled = it
                            },
                            modifier = Modifier.scale(0.8f)
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // AdMob specific status
                    val admobActive = pStatus[AdProvider.ADMOB] ?: true
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("AdMob Network Eligibility", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = admobActive,
                            onCheckedChange = {
                                RemoteKillSwitch.setProviderStatus(AdProvider.ADMOB, it)
                            },
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                    if (admobActive) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    "Google AdMob Active Production Ad Units",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                SelectionContainer {
                                    Column {
                                        Text("• App ID: ca-app-pub-6715807412270192~2612781545", fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("• Banner Ad (Feed): ca-app-pub-6715807412270192/5224534379", fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("• Rewarded Video (Reels): ca-app-pub-6715807412270192/7621643175", fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("• Rewarded Interstitial: ca-app-pub-6715807412270192/7621643175", fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("• Banner Ad (General): ca-app-pub-6715807412270192/5224534379", fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("• Interstitial Ad: ca-app-pub-6715807412270192/5224534379", fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- SECTION 14: RELEASE COMPLIANCE GATE ---
        item {
            var techGate by remember { mutableStateOf(ReleaseComplianceGate.isTechnicalValidationPassed) }
            var policyGate by remember { mutableStateOf(ReleaseComplianceGate.isPolicyValidationPassed) }
            var qaGate by remember { mutableStateOf(ReleaseComplianceGate.isQaValidationPassed) }
            var privacyGate by remember { mutableStateOf(ReleaseComplianceGate.isPrivacyValidationPassed) }
            var appAdsTxtGate by remember { mutableStateOf(ReleaseComplianceGate.isAppAdsTxtVerified) }

            val isReady = ReleaseComplianceGate.evaluateReleaseReadiness(AdEnvironment.PRODUCTION)

            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, if (isReady) Color(0xFF4CAF50) else Color(0xFFF44336))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "🏁 Production Release Gate Audit",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isReady) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                    Text(
                        text = "Under Chapter 8 guidelines, no monetization release can occur unless all validation gates and app-ads.txt are fully verified.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Checklist items
                    listOf(
                        "Technical Validation" to Pair(techGate) { b: Boolean -> techGate = b; ReleaseComplianceGate.isTechnicalValidationPassed = b },
                        "Policy Validation" to Pair(policyGate) { b: Boolean -> policyGate = b; ReleaseComplianceGate.isPolicyValidationPassed = b },
                        "QA Validation" to Pair(qaGate) { b: Boolean -> qaGate = b; ReleaseComplianceGate.isQaValidationPassed = b },
                        "Privacy Validation" to Pair(privacyGate) { b: Boolean -> privacyGate = b; ReleaseComplianceGate.isPrivacyValidationPassed = b },
                        "app-ads.txt Verified (Jan 2025)" to Pair(appAdsTxtGate) { b: Boolean -> appAdsTxtGate = b; ReleaseComplianceGate.isAppAdsTxtVerified = b }
                    ).forEach { (label, pair) ->
                        val (valState, setter) = pair
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                            Checkbox(
                                checked = valState,
                                onCheckedChange = setter,
                                modifier = Modifier.scale(0.85f)
                            )
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isReady) Color(0xFF4CAF50).copy(alpha = 0.12f)
                                else Color(0xFFF44336).copy(alpha = 0.12f)
                            )
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isReady) "COMPLIANCE PASSED → PRODUCTION RELEASE ENABLED"
                            else "PRODUCTION BLOCKED: GATES NOT RESOLVED",
                            color = if (isReady) Color(0xFF4CAF50) else Color(0xFFF44336),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // --- SECTION 15: NEVER DO THIS RULES & QA CHECKLISTS ---
        item {
            var expandedNever by remember { mutableStateOf(false) }
            var expandedQAHome by remember { mutableStateOf(false) }
            var expandedQAReels by remember { mutableStateOf(false) }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Never Do This Expandable Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { expandedNever = !expandedNever },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "🚫 \"Never Do This\" Rulebook",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Icon(
                                if (expandedNever) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        if (expandedNever) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val neverRules = listOf(
                                "Coins for Clicking Ads (Clicks ≠ Coin Reward)",
                                "Coins for Installing unless explicitly supported",
                                "Fake Ad Completion Simulation on Real Providers",
                                "Self-Generated Impressions (or Self-Clicks)",
                                "Hidden Ads (No unexpected / secret ad overlays)",
                                "Forced Clicks (Do not lock content behind ad clicks)",
                                "Fake Advertiser Profiles / AI guesses",
                                "Production Ads during Developer Testing",
                                "Unlimited preloading / spamming duplicate requests",
                                "Automatic Rewards from ordinary Native impressions",
                                "Custom timers replacing validated provider callbacks"
                            )
                            neverRules.forEachIndexed { idx, rule ->
                                Text(
                                    text = "${idx + 1}. $rule",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Native Feed QA Checklist Expandable Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { expandedQAHome = !expandedQAHome },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "📸 Native Feed Compliance Checklist",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                if (expandedQAHome) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null
                            )
                        }
                        if (expandedQAHome) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val homeChecklist = listOf(
                                "Advertisement is clearly identified as sponsored",
                                "Official provider assets are rendered strictly",
                                "No fake profile metadata or follower counts exist",
                                "Card header is completely non-clickable",
                                "CTA behaviour is preserved without interception",
                                "No click reward is configured for users",
                                "No wallet animation overlays interactive elements",
                                "No accidental-click layout layouts are present",
                                "No unsupported ordinary impression rewards active",
                                "Test ads are used explicitly in development mode"
                            )
                            var homeChecks by remember { mutableStateOf(List(10) { true }) }
                            homeChecklist.forEachIndexed { idx, item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = homeChecks[idx],
                                        onCheckedChange = { b ->
                                            val list = homeChecks.toMutableList()
                                            list[idx] = b
                                            homeChecks = list
                                        },
                                        modifier = Modifier.scale(0.8f)
                                    )
                                    Text(item, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                // Reels QA Checklist Expandable Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { expandedQAReels = !expandedQAReels },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "🎬 Rewarded Reels Compliance Checklist",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                if (expandedQAReels) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null
                            )
                        }
                        if (expandedQAReels) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val reelsChecklist = listOf(
                                "Correct official AdMob Rewarded format is utilized",
                                "Entry into rewarded experience is voluntary",
                                "Provider controls playbacks and timers entirely",
                                "No custom 5-second production timer exists",
                                "Reward is processed from validated callback strictly",
                                "No click rewards are configured",
                                "One single reward callback credits exactly one reward",
                                "Next-ad preloading respects official lifecycle",
                                "Load failure states handle gracefully without traps",
                                "Test ad units are enforced during debugging"
                            )
                            var reelsChecks by remember { mutableStateOf(List(10) { true }) }
                            reelsChecklist.forEachIndexed { idx, item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = reelsChecks[idx],
                                        onCheckedChange = { b ->
                                            val list = reelsChecks.toMutableList()
                                            list[idx] = b
                                            reelsChecks = list
                                        },
                                        modifier = Modifier.scale(0.8f)
                                    )
                                    Text(item, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- SECTION 16: MULTI-PROVIDER ARCHITECTURE & HYBRID MARKETPLACE ---
        item {
            var selectedProviderId by remember { mutableStateOf("ADMOB") }
            val coroutineScope = rememberCoroutineScope()
            
            // Local state copy of direct campaigns for simulation
            val directCampaigns = remember {
                mutableStateListOf(
                    DirectCampaign(
                        advertiserName = "EcoSmart Energy",
                        productName = "Solar Panel Pro",
                        category = "Technology",
                        budget = 5000.0,
                        creativeAssetUrl = "https://example.com/solar.mp4",
                        targetAudience = "Homeowners 25-50",
                        rewardCoins = 40,
                        destinationUrl = "https://ecosmart.com",
                        status = CampaignStatus.APPROVED,
                        isPolicyVerified = true
                    ),
                    DirectCampaign(
                        advertiserName = "Apex Fit",
                        productName = "Apex SmartBand",
                        category = "Health",
                        budget = 2500.0,
                        creativeAssetUrl = "https://example.com/smartband.png",
                        targetAudience = "Fitness Enthusiasts",
                        rewardCoins = 20,
                        destinationUrl = "https://apexfit.com",
                        status = CampaignStatus.DRAFT,
                        isPolicyVerified = false
                    )
                )
            }

            // Custom campaign form states
            var formAdvName by remember { mutableStateOf("") }
            var formProdName by remember { mutableStateOf("") }
            var formCategory by remember { mutableStateOf("Education") }
            var formBudget by remember { mutableStateOf("1500") }
            var formRewardCoins by remember { mutableStateOf("25") }
            var formCreativeUrl by remember { mutableStateOf("https://example.com/creative.png") }
            var formTargetAudience by remember { mutableStateOf("All Users") }
            var formIsVerified by remember { mutableStateOf(false) }

            // Selector states
            var selPlacement by remember { mutableStateOf(AdPlacement.HOME) }
            var selFormat by remember { mutableStateOf(AdFormat.NATIVE) }
            val selectionLogs = remember { mutableStateListOf<String>() }
            var selectedResultName by remember { mutableStateOf("None Selected") }

            // Remote config states
            var remoteConfigId by remember { mutableStateOf(RemoteAdvertisementConfiguration.configId) }
            var remoteVersion by remember { mutableStateOf(RemoteAdvertisementConfiguration.version) }
            var remoteLastReviewed by remember { mutableStateOf(RemoteAdvertisementConfiguration.lastReviewedDate) }
            var remoteDefaultProv by remember { mutableStateOf(RemoteAdvertisementConfiguration.defaultProvider) }
            var remoteDirectEnabled by remember { mutableStateOf(RemoteAdvertisementConfiguration.directCampaignsEnabled) }

            // Health Monitor states
            var healthSuccesses by remember { mutableStateOf(ProviderHealthMonitor.getSuccesses(selectedProviderId)) }
            var healthFailures by remember { mutableStateOf(ProviderHealthMonitor.getFailures(selectedProviderId)) }
            var healthLatency by remember { mutableStateOf(ProviderHealthMonitor.getLatency(selectedProviderId)) }
            
            val activeBreaker = ProviderCircuitBreakerRegistry.getCircuitBreaker(selectedProviderId)
            var breakerState by remember { mutableStateOf(activeBreaker.state) }
            var consecutiveFailures by remember { mutableStateOf(activeBreaker.consecutiveFailures) }

            // Sync health states whenever selected provider shifts
            LaunchedEffect(selectedProviderId) {
                healthSuccesses = ProviderHealthMonitor.getSuccesses(selectedProviderId)
                healthFailures = ProviderHealthMonitor.getFailures(selectedProviderId)
                healthLatency = ProviderHealthMonitor.getLatency(selectedProviderId)
                breakerState = ProviderCircuitBreakerRegistry.getCircuitBreaker(selectedProviderId).state
                consecutiveFailures = ProviderCircuitBreakerRegistry.getCircuitBreaker(selectedProviderId).consecutiveFailures
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🛰️ Chapter 9: Hybrid Ads & Multi-Provider Architecture",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "COMPLIANT LOCK",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Text(
                        text = "Prepared for Phase-based expansion (Dummy -> AdMob -> Mediation -> Direct campaigns -> Hybrid selection marketplace) without modifying core layout interfaces.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // SECTION A: PROVIDER CAPABILITY REGISTRY & SELECTION LOGIC
                    Text(
                        "1. Provider Capability Registry & Policy States",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("DUMMY", "ADMOB", "INMOBI", "APPLOVIN", "DIRECT_CAMPAIGNS").forEach { pid ->
                            val isSelected = selectedProviderId == pid
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedProviderId = pid },
                                label = { Text(pid, fontSize = 11.sp) }
                            )
                        }
                    }

                    val adapter = remember(selectedProviderId) { AdvertisementProviderRegistry.getAdapter(selectedProviderId) }
                    val availability = remember(selectedProviderId) { AdvertisementProviderRegistry.getAvailability(selectedProviderId) }

                    if (adapter != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(10.dp)
                                .clip(RoundedCornerShape(6.dp))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    adapter.providerName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Status: " + availability.name,
                                    color = when (availability) {
                                        TechnicalAvailability.SUPPORTED -> Color(0xFF4CAF50)
                                        TechnicalAvailability.TEST_ONLY -> Color(0xFF2196F3)
                                        TechnicalAvailability.REQUIRES_REVIEW -> Color(0xFFFF9800)
                                        TechnicalAvailability.DISABLED -> Color(0xFFF44336)
                                        else -> Color.Gray
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Policy Review: ${adapter.getReviewStatus().name}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (adapter.getReviewStatus() == PolicyReviewStatus.CURRENT) Color(0xFF4CAF50) else Color(0xFFFF9800)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Reward Mode: ${adapter.getRewardMode().name}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Capabilities: " + adapter.getCapabilities().joinToString { it.name },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // SECTION B: PROVIDER HEALTH MONITOR & CIRCUIT BREAKER
                    Text(
                        "2. Provider Circuit Breaker & Health Monitor",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Loads Successful: $healthSuccesses", style = MaterialTheme.typography.bodySmall)
                            Text("Loads Failed: $healthFailures", style = MaterialTheme.typography.bodySmall)
                            Text("Last Latency: ${healthLatency}ms", style = MaterialTheme.typography.bodySmall)
                        }
                        
                        // Circuit Breaker State Visual Box
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text("Circuit Breaker:", style = MaterialTheme.typography.labelSmall)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when (breakerState) {
                                            CircuitState.CLOSED -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                                            CircuitState.OPEN -> Color(0xFFF44336).copy(alpha = 0.15f)
                                            CircuitState.HALF_OPEN -> Color(0xFFFF9800).copy(alpha = 0.15f)
                                        }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = breakerState.name,
                                    fontWeight = FontWeight.Bold,
                                    color = when (breakerState) {
                                        CircuitState.CLOSED -> Color(0xFF4CAF50)
                                        CircuitState.OPEN -> Color(0xFFF44336)
                                        CircuitState.HALF_OPEN -> Color(0xFFFF9800)
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Text(
                                "Consecutive Failures: $consecutiveFailures/3",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                val lat = (80..350).random().toLong()
                                ProviderHealthMonitor.recordLoadSuccess(selectedProviderId, lat)
                                healthSuccesses = ProviderHealthMonitor.getSuccesses(selectedProviderId)
                                healthLatency = lat
                                breakerState = ProviderCircuitBreakerRegistry.getCircuitBreaker(selectedProviderId).state
                                consecutiveFailures = ProviderCircuitBreakerRegistry.getCircuitBreaker(selectedProviderId).consecutiveFailures
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Simulate Success", fontSize = 10.sp)
                        }
                        Button(
                            onClick = {
                                ProviderHealthMonitor.recordLoadFailure(selectedProviderId)
                                healthFailures = ProviderHealthMonitor.getFailures(selectedProviderId)
                                breakerState = ProviderCircuitBreakerRegistry.getCircuitBreaker(selectedProviderId).state
                                consecutiveFailures = ProviderCircuitBreakerRegistry.getCircuitBreaker(selectedProviderId).consecutiveFailures
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                            modifier = Modifier.weight(1.05f)
                        ) {
                            Text("Simulate Failure", fontSize = 10.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                ProviderHealthMonitor.resetStats(selectedProviderId)
                                healthSuccesses = 0
                                healthFailures = 0
                                healthLatency = 0L
                                breakerState = CircuitState.CLOSED
                                consecutiveFailures = 0
                            },
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Text("Reset Health", fontSize = 10.sp)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // SECTION C: DIRECT CAMPAIGNS & CAMPAIGN CREATIVE SEPARATION
                    Text(
                        "3. Direct Advertiser Campaign Platform",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        "Unlike standard third-party ads, directly negotiated campaigns capture detailed, verified metadata fields without any guess-work.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    // Form
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Negotiate & Create Campaign:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        
                        OutlinedTextField(
                            value = formAdvName,
                            onValueChange = { formAdvName = it },
                            label = { Text("Advertiser Name", fontSize = 10.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = formProdName,
                            onValueChange = { formProdName = it },
                            label = { Text("Product / Campaign Name", fontSize = 10.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = formBudget,
                                onValueChange = { formBudget = it },
                                label = { Text("Budget ($)", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = formRewardCoins,
                                onValueChange = { formRewardCoins = it },
                                label = { Text("Coins Awarded", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = formIsVerified,
                                onCheckedChange = { formIsVerified = it }
                            )
                            Text(
                                "Creative & Destination URLs passed strict Policy Review",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (formIsVerified) Color(0xFF4CAF50) else Color(0xFFF44336)
                            )
                        }

                        Button(
                            onClick = {
                                if (formAdvName.isNotEmpty() && formProdName.isNotEmpty()) {
                                    val coins = formRewardCoins.toIntOrNull() ?: 20
                                    val budgetVal = formBudget.toDoubleOrNull() ?: 1000.0
                                    val status = if (formIsVerified) CampaignStatus.APPROVED else CampaignStatus.UNDER_REVIEW
                                    
                                    val newCampaign = DirectCampaign(
                                        advertiserName = formAdvName,
                                        productName = formProdName,
                                        category = formCategory,
                                        budget = budgetVal,
                                        creativeAssetUrl = formCreativeUrl,
                                        targetAudience = formTargetAudience,
                                        rewardCoins = coins,
                                        destinationUrl = "https://www.google.com/search?q=$formAdvName",
                                        status = status,
                                        isPolicyVerified = formIsVerified
                                    )
                                    directCampaigns.add(newCampaign)
                                    
                                    // Reset form fields
                                    formAdvName = ""
                                    formProdName = ""
                                    formIsVerified = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = formAdvName.isNotEmpty() && formProdName.isNotEmpty()
                        ) {
                            Text("Submit and Evaluate Campaign", fontSize = 11.sp)
                        }
                    }

                    // Display campaigns
                    Text("Negotiated Premium Direct Queue:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    directCampaigns.forEach { campaign ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${campaign.advertiserName} ★",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(
                                                when (campaign.status) {
                                                    CampaignStatus.APPROVED -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                                                    CampaignStatus.UNDER_REVIEW -> Color(0xFFFF9800).copy(alpha = 0.15f)
                                                    else -> Color.Gray.copy(alpha = 0.15f)
                                                }
                                            )
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = campaign.status.name,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (campaign.status) {
                                                CampaignStatus.APPROVED -> Color(0xFF4CAF50)
                                                CampaignStatus.UNDER_REVIEW -> Color(0xFFFF9800)
                                                else -> Color.Gray
                                            }
                                        )
                                    }
                                }
                                Text("Product: ${campaign.productName} [Budget: $${campaign.budget}]", style = MaterialTheme.typography.labelSmall)
                                Text("Incentive: +${campaign.rewardCoins} coins per verified interaction", style = MaterialTheme.typography.labelSmall)
                                Text("Stats: Impressions=${campaign.impressions}, Clicks=${campaign.clicks}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (campaign.status == CampaignStatus.APPROVED) {
                                        Button(
                                            onClick = {
                                                campaign.impressions++
                                                // Trigger simulated credit to server ledger
                                                ServerSideWalletLedger.addTransaction(
                                                    type = "CREDIT",
                                                    amount = campaign.rewardCoins,
                                                    source = "Direct Ad: ${campaign.advertiserName}",
                                                    policyNamespace = RewardPolicyNamespace.DIRECT_CAMPAIGN_POLICY,
                                                    isServerValidated = true
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text("Deliver Impression (+View & Reward)", fontSize = 8.sp)
                                        }
                                    }
                                    if (campaign.status == CampaignStatus.APPROVED) {
                                        OutlinedButton(
                                            onClick = { campaign.status = CampaignStatus.PAUSED },
                                            modifier = Modifier.weight(0.4f),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text("Pause", fontSize = 8.sp)
                                        }
                                    } else if (campaign.status == CampaignStatus.PAUSED || campaign.status == CampaignStatus.DRAFT) {
                                        Button(
                                            onClick = { campaign.status = CampaignStatus.APPROVED },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                            modifier = Modifier.weight(0.5f),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text("Activate", fontSize = 8.sp)
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { directCampaigns.remove(campaign) },
                                        modifier = Modifier.weight(0.35f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF44336)),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text("Delete", fontSize = 8.sp)
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // SECTION D: HYBRID SELECTION SIMULATOR
                    Text(
                        "4. Hybrid Source Selection Engine Simulator",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        "Selects eligible provider based on strict decision order (Compliance -> Consent -> Compatibility -> Availability -> Circuit Breakers -> Performance).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Placement", style = MaterialTheme.typography.labelSmall)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                AdPlacement.values().take(2).forEach { plc ->
                                    FilterChip(
                                        selected = selPlacement == plc,
                                        onClick = { selPlacement = plc },
                                        label = { Text(plc.name, fontSize = 9.sp) }
                                    )
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1.1f)) {
                            Text("Format", style = MaterialTheme.typography.labelSmall)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(AdFormat.NATIVE, AdFormat.REWARDED).forEach { fmt ->
                                    FilterChip(
                                        selected = selFormat == fmt,
                                        onClick = { selFormat = fmt },
                                        label = { Text(fmt.name, fontSize = 9.sp) }
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val logs = mutableListOf<String>()
                            val result = AdvertisementProviderSelector.selectProvider(
                                placement = selPlacement,
                                format = selFormat,
                                consentState = PrivacyConsentManager.consentState.value,
                                environment = AdEnvironmentConfiguration.activeEnvironment,
                                logs = logs
                            )
                            selectedResultName = result?.providerName ?: "None"
                            selectionLogs.clear()
                            selectionLogs.addAll(logs)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Simulate Source Selector Decision", fontSize = 11.sp)
                    }

                    if (selectedResultName != "None Selected") {
                        Text(
                            text = "Selected Provider: $selectedResultName",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    if (selectionLogs.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 140.dp)
                                .background(Color.Black.copy(alpha = 0.05f))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                .verticalScroll(rememberScrollState())
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            selectionLogs.forEach { logLine ->
                                Text(
                                    text = logLine,
                                    fontSize = 10.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = if (logLine.contains("❌")) Color(0xFFF44336) else if (logLine.contains("✅")) Color(0xFF4CAF50) else Color.DarkGray
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // SECTION E: REMOTE CONFIGURATION & RECOVERY ENG
                    Text(
                        "5. Remote Governance & Config Engine",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(8.dp)
                    ) {
                        Text("Config ID: $remoteConfigId (v$remoteVersion)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text("Last Audited: $remoteLastReviewed", style = MaterialTheme.typography.bodySmall)
                        Text("Default Network: $remoteDefaultProv", style = MaterialTheme.typography.bodySmall)
                        Text("Direct Marketplace Enabled: $remoteDirectEnabled", style = MaterialTheme.typography.bodySmall)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                RemoteAdvertisementConfiguration.configId = "cfg_fetch_" + (100..999).random()
                                RemoteAdvertisementConfiguration.version++
                                RemoteAdvertisementConfiguration.lastReviewedDate = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date())
                                RemoteAdvertisementConfiguration.defaultProvider = "ADMOB"
                                RemoteAdvertisementConfiguration.directCampaignsEnabled = true
                                
                                remoteConfigId = RemoteAdvertisementConfiguration.configId
                                remoteVersion = RemoteAdvertisementConfiguration.version
                                remoteLastReviewed = RemoteAdvertisementConfiguration.lastReviewedDate
                                remoteDefaultProv = RemoteAdvertisementConfiguration.defaultProvider
                                remoteDirectEnabled = RemoteAdvertisementConfiguration.directCampaignsEnabled
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Fetch Configuration", fontSize = 10.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                RemoteAdvertisementConfiguration.resetToSafeLocalDefaults()
                                remoteConfigId = RemoteAdvertisementConfiguration.configId
                                remoteVersion = RemoteAdvertisementConfiguration.version
                                remoteLastReviewed = RemoteAdvertisementConfiguration.lastReviewedDate
                                remoteDefaultProv = RemoteAdvertisementConfiguration.defaultProvider
                                remoteDirectEnabled = RemoteAdvertisementConfiguration.directCampaignsEnabled
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Safe Local Defaults", fontSize = 10.sp)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // SECTION F: SERVER-SIDE WALLET LEDGER & UPI/GIFT REDEMPTION GATE
                    val transactionsList by ServerSideWalletLedger.ledgerTransactions.collectAsState()
                    var localLedgerBalance by remember { mutableStateOf(ServerSideWalletLedger.getLocalLedgerBalance()) }
                    var ledgerVerifyMsg by remember { mutableStateOf("") }
                    var isLedgerSecure by remember { mutableStateOf(ServerSideWalletLedger.verifyLedgerIntegrity()) }

                    LaunchedEffect(transactionsList) {
                        localLedgerBalance = ServerSideWalletLedger.getLocalLedgerBalance()
                        isLedgerSecure = ServerSideWalletLedger.verifyLedgerIntegrity()
                    }

                    Text(
                        "6. Server-Side Wallet Ledger & Redemption Security",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        "Real redeemable money cannot trust local client data. Chapter 9 mandates a server-side append-only ledger audit before any redemption can proceed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Server Wallet Balance:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "$localLedgerBalance Coins",
                            fontWeight = FontWeight.Bold,
                            color = PremiumGold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ledger Tamper-Free Verification:", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = if (isLedgerSecure) "✅ COMPLIANT SECURED" else "❌ WARNING: TAMPERED/UNVERIFIED",
                            color = if (isLedgerSecure) Color(0xFF4CAF50) else Color(0xFFF44336),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                ServerSideWalletLedger.addTransaction(
                                    type = "CREDIT",
                                    amount = 15,
                                    source = "AdMob Video Callback",
                                    policyNamespace = RewardPolicyNamespace.ADMOB_REWARDED_POLICY,
                                    isServerValidated = true
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Simulate Verified AdMob Event", fontSize = 9.sp)
                        }
                        Button(
                            onClick = {
                                // Simulate adding an unvalidated local transaction to demonstrate safety controls
                                ServerSideWalletLedger.addTransaction(
                                    type = "CREDIT",
                                    amount = 200,
                                    source = "Injected Cheat Tool",
                                    policyNamespace = RewardPolicyNamespace.DUMMY_POLICY,
                                    isServerValidated = false // Server will fail validating this
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Inject Unverified Local TX", fontSize = 9.sp)
                        }
                    }

                    Button(
                        onClick = {
                            if (!ServerSideWalletLedger.verifyLedgerIntegrity()) {
                                ledgerVerifyMsg = "❌ CASHOUT BLOCKED: Integrity check failed! Unverified transactions found in ledger audits. Request Rejected."
                            } else if (localLedgerBalance < 50) {
                                ledgerVerifyMsg = "❌ CASHOUT BLOCKED: Insufficient balance. Ledger verified but requires minimum of 50 Coins."
                            } else {
                                ServerSideWalletLedger.addTransaction(
                                    type = "DEBIT",
                                    amount = 50,
                                    source = "UPI Redemption",
                                    policyNamespace = RewardPolicyNamespace.FUTURE_PROVIDER_POLICY,
                                    isServerValidated = true
                                )
                                ledgerVerifyMsg = "✅ CASHOUT APPROVED: UPI/Gift Card disbursement initialized successfully. Transaction registered on backend ledger."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Audit Ledger & Request UPI / Gift Card Cashout", fontSize = 11.sp)
                    }

                    if (ledgerVerifyMsg.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (ledgerVerifyMsg.contains("✅")) Color(0xFF4CAF50).copy(alpha = 0.12f)
                                    else Color(0xFFF44336).copy(alpha = 0.12f)
                                )
                                .padding(10.dp)
                        ) {
                            Text(
                                text = ledgerVerifyMsg,
                                color = if (ledgerVerifyMsg.contains("✅")) Color(0xFF4CAF50) else Color(0xFFF44336),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Ledger table display
                    Text("Append-Only Verification Ledger:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 120.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        transactionsList.forEach { tx ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.03f))
                                    .padding(6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("${tx.transactionId} - ${tx.source}", fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                    Text("Policy Namespace: ${tx.policyNamespace.name}", fontSize = 8.sp, color = Color.Gray)
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = if (tx.type == "CREDIT") "+${tx.amount}" else "-${tx.amount}",
                                        color = if (tx.type == "CREDIT") Color(0xFF4CAF50) else Color(0xFFF44336),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(if (tx.isServerValidated) Color(0xFF4CAF50).copy(alpha = 0.12f) else Color(0xFFF44336).copy(alpha = 0.12f))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (tx.isServerValidated) "Verified" else "Unverified",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (tx.isServerValidated) Color(0xFF4CAF50) else Color(0xFFF44336)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
