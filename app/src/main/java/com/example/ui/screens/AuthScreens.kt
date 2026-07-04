package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.utils.ValidationManager
import com.example.data.utils.ValidationResult
import com.example.viewmodel.AdViewModel
import com.example.ui.components.SponzaLogo

@Composable
fun LoginScreen(
    viewModel: AdViewModel,
    onNavigateToInterests: (username: String, email: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLoginTab by remember { mutableStateOf(true) }
    
    // Login Fields State
    var loginEmailOrMobile by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginPasswordVisible by remember { mutableStateOf(false) }

    // Register Fields State
    var registerFullName by remember { mutableStateOf("") }
    var registerEmail by remember { mutableStateOf("") }
    var registerMobile by remember { mutableStateOf("") }
    var registerPassword by remember { mutableStateOf("") }
    var registerConfirmPassword by remember { mutableStateOf("") }
    var registerPasswordVisible by remember { mutableStateOf(false) }
    var registerConfirmPasswordVisible by remember { mutableStateOf(false) }

    // Error Feedback State
    var errorMessage by remember { mutableStateOf("") }
    var showForgotDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Elegant Gold Badge Hero Logo
            SponzaLogo(size = 100.dp)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Sponza",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            Text(
                text = "Watch Relevant Ads. Earn Rewards. Transparently.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // High Fidelity M3 Segmented Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isLoginTab) MaterialTheme.colorScheme.primary
                            else Color.Transparent
                        )
                        .clickable {
                            isLoginTab = true
                            errorMessage = ""
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Login",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isLoginTab) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (!isLoginTab) MaterialTheme.colorScheme.primary
                            else Color.Transparent
                        )
                        .clickable {
                            isLoginTab = false
                            errorMessage = ""
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Register",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (!isLoginTab) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Dynamic Inputs based on selected Tab
            if (isLoginTab) {
                // Email or Mobile Field
                OutlinedTextField(
                    value = loginEmailOrMobile,
                    onValueChange = {
                        loginEmailOrMobile = it
                        errorMessage = ""
                    },
                    label = { Text("Email or Mobile Number") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = "User Account Icon") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("username_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Field
                OutlinedTextField(
                    value = loginPassword,
                    onValueChange = {
                        loginPassword = it
                        errorMessage = ""
                    },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Lock Icon") },
                    trailingIcon = {
                        IconButton(onClick = { loginPasswordVisible = !loginPasswordVisible }) {
                            Icon(
                                imageVector = if (loginPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (loginPasswordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (loginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_password_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Forgot Password?",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { showForgotDialog = true }
                            .padding(4.dp)
                    )
                }

            } else {
                // Full Name
                OutlinedTextField(
                    value = registerFullName,
                    onValueChange = {
                        registerFullName = it
                        errorMessage = ""
                    },
                    label = { Text("Full Name") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = "Name Icon") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Email
                OutlinedTextField(
                    value = registerEmail,
                    onValueChange = {
                        registerEmail = it
                        errorMessage = ""
                    },
                    label = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email Icon") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("email_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Mobile Number
                OutlinedTextField(
                    value = registerMobile,
                    onValueChange = {
                        registerMobile = it
                        errorMessage = ""
                    },
                    label = { Text("Mobile Number (10-digit)") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone Icon") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Password
                OutlinedTextField(
                    value = registerPassword,
                    onValueChange = {
                        registerPassword = it
                        errorMessage = ""
                    },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Lock Icon") },
                    trailingIcon = {
                        IconButton(onClick = { registerPasswordVisible = !registerPasswordVisible }) {
                            Icon(
                                imageVector = if (registerPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    },
                    visualTransformation = if (registerPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Confirm Password
                OutlinedTextField(
                    value = registerConfirmPassword,
                    onValueChange = {
                        registerConfirmPassword = it
                        errorMessage = ""
                    },
                    label = { Text("Confirm Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Confirm Lock Icon") },
                    trailingIcon = {
                        IconButton(onClick = { registerConfirmPasswordVisible = !registerConfirmPasswordVisible }) {
                            Icon(
                                imageVector = if (registerConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle confirm password visibility"
                            )
                        }
                    },
                    visualTransformation = if (registerConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    )
                )
            }

            // Error Message UI Feedback
            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Action Button
            Button(
                onClick = {
                    if (isLoginTab) {
                        // Validate Login
                        val valResult = ValidationManager.validateLogin(loginEmailOrMobile, loginPassword)
                        if (valResult is ValidationResult.Error) {
                            errorMessage = valResult.message
                        } else {
                            viewModel.loginUser(
                                loginEmailOrMobile.trim(),
                                loginPassword,
                                onSuccess = { profile ->
                                    Toast.makeText(context, "Welcome back, ${profile.fullName}!", Toast.LENGTH_SHORT).show()
                                    onNavigateToInterests(profile.username, profile.email)
                                },
                                onError = { err ->
                                    errorMessage = err
                                }
                            )
                        }
                    } else {
                        // Validate Registration
                        val valResult = ValidationManager.validateRegistration(
                            registerFullName.trim(),
                            registerEmail.trim(),
                            registerMobile.trim(),
                            registerPassword,
                            registerConfirmPassword
                        )
                        if (valResult is ValidationResult.Error) {
                            errorMessage = valResult.message
                        } else {
                            val generatedUsername = registerEmail.substringBefore("@")
                            viewModel.registerNewUser(
                                username = generatedUsername,
                                fullName = registerFullName.trim(),
                                email = registerEmail.trim(),
                                mobile = registerMobile.trim(),
                                passwordRaw = registerPassword,
                                onSuccess = { profile ->
                                    Toast.makeText(context, "Registration successful!", Toast.LENGTH_SHORT).show()
                                    onNavigateToInterests(profile.username, profile.email)
                                },
                                onError = { err ->
                                    errorMessage = err
                                }
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("login_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = if (isLoginTab) "Continue to Platform" else "Register & Continue",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Guest Mode Option
            OutlinedButton(
                onClick = {
                    viewModel.continueAsGuest { profile ->
                        Toast.makeText(context, "Continuing as ${profile.fullName}", Toast.LENGTH_SHORT).show()
                        onNavigateToInterests(profile.username, profile.email)
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("guest_button"),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.secondary
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.outline,
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                        )
                    )
                )
            ) {
                Icon(
                    imageVector = Icons.Default.NoAccounts,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Continue as Guest",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Terms of Use & Privacy Policy Footer Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Privacy Policy",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier
                        .clickable { showPrivacyDialog = true }
                        .padding(6.dp)
                )
                Text(
                    text = "•",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Text(
                    text = "Terms of Use",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier
                        .clickable { showTermsDialog = true }
                        .padding(6.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Forgot Password Prototype Dialog
    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LockReset,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Password Reset Request", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
            },
            text = {
                Text(
                    text = "Password Reset is unavailable in Prototype. This feature will be connected to the cloud authentication backend later.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = { showForgotDialog = false }) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Privacy Policy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = {
                Text("Privacy Policy", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            },
            text = {
                Box(modifier = Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Sponza respects your local privacy. In this offline prototype version, all data, including your passwords, name, mobile number, chosen categories, watched advertisements statistics, and wallet coins histories, are fully securely encrypted/hashed and stored locally on your physical device. We do not transmit, analyze, or share any of your private statistics, preferences, or activities with third parties.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Terms of Use Dialog
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = {
                Text("Terms of Use", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            },
            text = {
                Box(modifier = Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        text = "By accessing Sponza, you acknowledge that this is a simulated reward prototype app. All earned coins, cashback balances, vouchers, and UPI withdrawals listed on this platform are for demonstration and product evaluation purposes. These balances hold no real-world financial value and cannot be redeemed for legal currency outside this prototype environment.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTermsDialog = false }) {
                    Text("I Understand", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun InterestSelectionScreen(
    username: String,
    email: String,
    viewModel: AdViewModel,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoriesList = listOf(
        "Gaming", "Education", "Technology", "Finance", "Business", "Fashion",
        "Fitness", "Automobile", "Travel", "Food", "Movies", "Music",
        "Sports", "Health", "Books", "News"
    )

    val selectedCategories = remember { mutableStateListOf<String>() }
    var errorMsg by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome, $username!",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Select your interests to curate your rewarded advertisement feed. Select at least 3 categories to get a 200 Coin Signup Bonus!",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(categoriesList) { category ->
                    val isSelected = selectedCategories.contains(category)
                    InterestPillItem(
                        category = category,
                        isSelected = isSelected,
                        onToggle = {
                            if (isSelected) {
                                selectedCategories.remove(category)
                            } else {
                                selectedCategories.add(category)
                            }
                            if (selectedCategories.size >= 3) {
                                errorMsg = ""
                            }
                        }
                    )
                }
            }

            if (errorMsg.isNotEmpty()) {
                Text(
                    text = errorMsg,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (selectedCategories.size < 3) {
                        errorMsg = "Please choose at least 3 interests to continue."
                    } else if (selectedCategories.size > 10) {
                        errorMsg = "You can select up to 10 interests maximum."
                    } else {
                        viewModel.registerUser(username, email, selectedCategories.toList())
                        onComplete()
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("interests_continue_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "Let's Earn Rewards",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun InterestPillItem(
    category: String,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onToggle() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = category,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
            )

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected Icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
