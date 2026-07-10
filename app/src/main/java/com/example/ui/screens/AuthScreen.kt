package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MockData
import com.example.ui.viewmodel.ConnectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: ConnectViewModel,
    onAuthSuccess: () -> Unit
) {
    var isLoginTab by remember { mutableStateOf(true) }
    val loginError by viewModel.loginError.collectAsStateWithLifecycle()
    var isLoading by remember { mutableStateOf(false) }

    // Form states
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("Kathmandu") }
    var interests by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var cityMenuExpanded by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // 1. Sleek Gradient Banner Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Decorative concentric circle background shapes
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(150.dp))
                )
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(90.dp))
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ConnectWithoutContact,
                        contentDescription = "Logo",
                        tint = Color.White,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Connect",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Discover & join activities near you",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 2. Tab Selection (Custom Rounded Capsule Style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Login Tab Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            if (isLoginTab) MaterialTheme.colorScheme.primary else Color.Transparent
                        )
                        .clickable { isLoginTab = true }
                        .testTag("tab_login"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Login",
                        color = if (isLoginTab) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                // Sign Up Tab Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            if (!isLoginTab) MaterialTheme.colorScheme.primary else Color.Transparent
                        )
                        .clickable { isLoginTab = false }
                        .testTag("tab_signup"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sign Up",
                        color = if (!isLoginTab) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            // 3. Error Banner
            if (loginError != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .testTag("auth_error_card")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = loginError ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 4. Fields Content Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isLoginTab) {
                        // --- LOGIN FORM ---
                        Text(
                            text = "Welcome Back",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Email
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_email_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )

                        // Password
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle password visibility"
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_password_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                isLoading = true
                                viewModel.login(email, password) { success ->
                                    isLoading = false
                                    if (success) {
                                        onAuthSuccess()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("login_submit_button"),
                            shape = RoundedCornerShape(26.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Log In", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }

                        // Demo Account Fast Access
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Or quick login with a demo account:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                email = "ayush@connect.com"
                                password = "password123"
                                isLoading = true
                                viewModel.login("ayush@connect.com", "password123") { success ->
                                    isLoading = false
                                    if (success) {
                                        onAuthSuccess()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("quick_login_button"),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ayush Karki (Demo)", fontWeight = FontWeight.Bold)
                        }

                    } else {
                        // --- SIGN UP FORM ---
                        Text(
                            text = "Create Account",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Name
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Full Name *") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_name_input"),
                            singleLine = true
                        )

                        // Email
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address *") },
                            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_email_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )

                        // Password
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password *") },
                            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_password_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            visualTransformation = PasswordVisualTransformation()
                        )

                        // City Select Dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = city,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Selected City *") },
                                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { cityMenuExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { cityMenuExpanded = true }
                                    .testTag("signup_city_dropdown")
                            )

                            DropdownMenu(
                                expanded = cityMenuExpanded,
                                onDismissRequest = { cityMenuExpanded = false }
                            ) {
                                MockData.cities.forEach { cityName ->
                                    DropdownMenuItem(
                                        text = { Text(cityName) },
                                        onClick = {
                                            city = cityName
                                            cityMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Bio
                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            label = { Text("Short Bio") },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_bio_input"),
                            maxLines = 3
                        )

                        // Interests
                        OutlinedTextField(
                            value = interests,
                            onValueChange = { interests = it },
                            label = { Text("Interests (comma separated)") },
                            placeholder = { Text("e.g. Hiking, Cafés, Cycling") },
                            leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_interests_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                isLoading = true
                                viewModel.signup(
                                    name = name,
                                    email = email,
                                    password = password,
                                    city = city,
                                    bio = bio,
                                    interests = interests
                                ) { success ->
                                    isLoading = false
                                    if (success) {
                                        onAuthSuccess()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("signup_submit_button"),
                            shape = RoundedCornerShape(26.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Register & Join", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
