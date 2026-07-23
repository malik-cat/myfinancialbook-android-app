package com.myfinancialbook.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myfinancialbook.app.R
import com.myfinancialbook.app.util.AuthManager
import com.myfinancialbook.app.util.SecurityManager
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    vm: LedgerViewModel,
    onGoogleSignInRequest: () -> Unit = {}
) {
    var step by remember { mutableIntStateOf(0) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isRegister by remember { mutableStateOf(false) }
    var pinCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val ctx = androidx.compose.ui.platform.LocalContext.current

    val googleSignInEvent by vm.googleSignInEvent.collectAsState()

    LaunchedEffect(googleSignInEvent) {
        if (googleSignInEvent > 0 && step == 1) {
            step = 3
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when {
                step == 0 -> WelcomeStep(onNext = { step = 1 })

                step == 1 -> MethodSelectionStep(
                    onSelectEmail = { step = 2 },
                    onSelectGoogle = {
                        scope.launch {
                            if (AuthManager.isSignedIn()) {
                                step = 3
                            } else {
                                onGoogleSignInRequest()
                            }
                        }
                    }
                )

                step == 2 -> EmailSignInStep(
                    email = email, onEmailChange = { email = it },
                    password = password, onPasswordChange = { password = it },
                    confirmPassword = confirmPassword, onConfirmPasswordChange = { confirmPassword = it },
                    isRegister = isRegister, onToggleMode = { isRegister = !isRegister; error = null },
                    isLoading = isLoading, error = error,
                    onSubmit = {
                        scope.launch {
                            isLoading = true; error = null
                            if (isRegister && password != confirmPassword) {
                                error = "Passwords do not match"; isLoading = false; return@launch
                            }
                            val result = if (isRegister)
                                AuthManager.createUserWithEmail(email.trim(), password)
                            else
                                AuthManager.signInWithEmail(email.trim(), password)
                            isLoading = false
                            result.fold(
                                onSuccess = { step = 3 },
                                onFailure = { e -> error = e.localizedMessage ?: "Authentication failed" }
                            )
                        }
                    }
                )

                step == 3 -> SetPinStep(
                    pinCode = pinCode,
                    onPinChange = { pinCode = it },
                    onComplete = { pin, confirm ->
                        if (pin == confirm && pin.length == 4) {
                            SecurityManager.setBackupPin(ctx, pin)
                            vm.completeOnboarding()
                        }
                    }
                )
            }
        }

        if (step > 0 && step < 3) {
            TextButton(
                onClick = { step--; error = null },
                modifier = Modifier.padding(8.dp)
            ) {
                Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Back")
            }
        }

        if (isLoading) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    Text("Please wait...", color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun WelcomeStep(onNext: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            painter = painterResource(id = R.drawable.app_icon),
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = Color.Unspecified
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "Financial Book",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A2E),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Track your money, grow your business",
            fontSize = 15.sp,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A2E))
        ) {
            Text("Get Started", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun MethodSelectionStep(
    onSelectEmail: () -> Unit,
    onSelectGoogle: () -> Unit
) {
    Text("Welcome back", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E))
    Text("Choose how to continue", fontSize = 14.sp, color = Color(0xFF6B7280), modifier = Modifier.padding(top = 6.dp, bottom = 40.dp))

    // Google button
    OutlinedCard(
        onClick = onSelectGoogle,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFFDADCE0))
    ) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.google_logo),
                contentDescription = "Google",
                modifier = Modifier.size(22.dp),
                tint = Color.Unspecified
            )
            Spacer(Modifier.width(12.dp))
            Text("Continue with Google", fontWeight = FontWeight.Medium, fontSize = 15.sp, color = Color(0xFF3C4043))
        }
    }

    Spacer(Modifier.height(16.dp))

    // Divider with "or"
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(Modifier.weight(1f), color = Color(0xFFE0E0E0))
        Text("  or  ", color = Color(0xFF9E9E9E), fontSize = 13.sp)
        HorizontalDivider(Modifier.weight(1f), color = Color(0xFFE0E0E0))
    }

    Spacer(Modifier.height(16.dp))

    // Email button
    OutlinedCard(
        onClick = onSelectEmail,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFFDADCE0))
    ) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Email,
                contentDescription = "Email",
                modifier = Modifier.size(22.dp),
                tint = Color(0xFF5F6368)
            )
            Spacer(Modifier.width(12.dp))
            Text("Continue with Email", fontWeight = FontWeight.Medium, fontSize = 15.sp, color = Color(0xFF3C4043))
        }
    }
}

@Composable
fun EmailSignInStep(
    email: String, onEmailChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    confirmPassword: String, onConfirmPasswordChange: (String) -> Unit,
    isRegister: Boolean, onToggleMode: () -> Unit,
    isLoading: Boolean, error: String?,
    onSubmit: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            if (isRegister) Icons.Default.PersonAdd else Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(48.dp).padding(bottom = 8.dp),
            tint = Color(0xFF1A1A2E)
        )
        Text(
            if (isRegister) "Create Account" else "Sign In",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A2E)
        )
        Text(
            if (isRegister) "Register with your email address" else "Sign in to your account",
            fontSize = 14.sp,
            color = Color(0xFF6B7280),
            modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
        )

        OutlinedTextField(
            value = email, onValueChange = onEmailChange,
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, null, tint = Color(0xFF9E9E9E)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF1A1A2E),
                focusedLabelColor = Color(0xFF1A1A2E)
            )
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password, onValueChange = onPasswordChange,
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color(0xFF9E9E9E)) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF1A1A2E),
                focusedLabelColor = Color(0xFF1A1A2E)
            )
        )

        if (isRegister) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = confirmPassword, onValueChange = onConfirmPasswordChange,
                label = { Text("Confirm Password") },
                leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color(0xFF9E9E9E)) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                isError = confirmPassword.isNotEmpty() && password != confirmPassword,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1A1A2E),
                    focusedLabelColor = Color(0xFF1A1A2E)
                )
            )
            if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                Text("Passwords do not match", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp))
            }
        }

        error?.let {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(it, color = Color(0xFFD32F2F), fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onSubmit,
            enabled = email.isNotBlank() && password.length >= 6 && !isLoading && (!isRegister || (confirmPassword == password)),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A2E))
        ) {
            Text(
                if (isRegister) "Create Account" else "Sign In",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = onToggleMode) {
            Text(
                if (isRegister) "Already have an account? Sign In"
                else "Don't have an account? Register",
                color = Color(0xFF1A1A2E),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun SetPinStep(pinCode: String, onPinChange: (String) -> Unit, onComplete: (String, String) -> Unit) {
    var confirmPin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = Color(0xFF1A1A2E)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Secure Your App",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A2E)
        )
        Text(
            "Set a 4-digit PIN to keep your data private",
            fontSize = 14.sp,
            color = Color(0xFF6B7280),
            modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
        )

        OutlinedTextField(
            value = pinCode,
            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) { onPinChange(it); showError = false } },
            label = { Text("Create PIN") },
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color(0xFF9E9E9E)) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 22.sp, fontWeight = FontWeight.Bold),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF1A1A2E),
                focusedLabelColor = Color(0xFF1A1A2E)
            )
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPin,
            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) { confirmPin = it; showError = false } },
            label = { Text("Confirm PIN") },
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color(0xFF9E9E9E)) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 22.sp, fontWeight = FontWeight.Bold),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            isError = confirmPin.length == 4 && pinCode != confirmPin,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF1A1A2E),
                focusedLabelColor = Color(0xFF1A1A2E)
            )
        )

        if (confirmPin.length == 4 && pinCode != confirmPin) {
            Text("PINs do not match", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp))
        }

        if (showError) {
            Text("PINs do not match", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp))
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                if (pinCode == confirmPin && pinCode.length == 4) {
                    onComplete(pinCode, confirmPin)
                } else {
                    showError = true
                }
            },
            enabled = pinCode.length == 4 && confirmPin.length == 4 && pinCode == confirmPin,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A2E))
        ) {
            Text("Set PIN & Start App", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}
