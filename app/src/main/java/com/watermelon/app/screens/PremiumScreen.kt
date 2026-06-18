package com.watermelon.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    onStartCheckout: (orderId: String, amountPaise: Int, planLabel: String) -> Unit = { _, _, _ -> }
) {
    val viewModel: PremiumViewModel = hiltViewModel()
    val isPremium by viewModel.isPremium.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    error?.let { msg ->
        LaunchedEffect(msg) {
            scope.launch { snackbarHostState.showSnackbar(msg) }
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Watermelon Plus",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        PremiumScreenContent(
            paddingValues = padding,
            isPremium = isPremium,
            isLoading = isLoading,
            onStartCheckout = onStartCheckout,
            viewModel = viewModel
        )
    }
}

@Composable
fun PremiumScreenContent(
    paddingValues: PaddingValues,
    isPremium: Boolean,
    isLoading: Boolean,
    onStartCheckout: (orderId: String, amountPaise: Int, planLabel: String) -> Unit,
    viewModel: PremiumViewModel
) {
    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
        // Background visual element
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-100).dp, y = (-100).dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFFF6B6B).copy(alpha = 0.15f), Color.Transparent)
                    )
                )
        )

        if (isPremium) {
            PremiumActiveContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            )
        } else {
            PremiumPlansContent(
                modifier = Modifier.fillMaxSize(),
                isLoading = isLoading,
                onPlanSelected = { key, paise, label ->
                    if (!isLoading) {
                        viewModel.createOrder(key, paise) { orderId ->
                            onStartCheckout(orderId, paise, label)
                        }
                    }
                },
                onStudentVerify = { email ->
                    viewModel.submitStudentVerification(email)
                },
                studentStatus = viewModel.studentStatus.collectAsState().value
            )
        }
    }
}

@Composable
private fun PremiumPlansContent(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    onPlanSelected: (String, Int, String) -> Unit,
    onStudentVerify: (String) -> Unit,
    studentStatus: StudentVerificationStatus
) {
    val scrollState = rememberScrollState()
    var showStudentDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Upgrade to Premium",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Experience music like never before with high-quality audio and no interruptions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "CHOOSE YOUR PLAN",
            style = MaterialTheme.typography.labelLarge.copy(
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Monthly
        PlanCardGradient(
            title = "Individual",
            price = "₹29",
            period = "/ month",
            subtitle = "Perfect for one person",
            features = listOf("Ad-free music", "Unlimited skips", "HQ audio (256kbps)"),
            gradient = Brush.linearGradient(listOf(Color(0xFFFE8C00), Color(0xFFF83600))),
            primaryColor = Color(0xFFF83600),
            onClick = { onPlanSelected("ind_mo", 2900, "Individual Monthly") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Yearly
        PlanCardGradient(
            title = "Best Value",
            price = "₹299",
            period = "/ year",
            subtitle = "Individual Yearly Plan",
            features = listOf("Everything in Monthly", "Offline downloads", "Premium themes"),
            gradient = Brush.linearGradient(listOf(Color(0xFF00C6FF), Color(0xFF0072FF))),
            primaryColor = Color(0xFF0072FF),
            badge = "14% OFF",
            onClick = { onPlanSelected("ind_yr", 29900, "Individual Yearly") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Family Monthly
        PlanCardGradient(
            title = "Family",
            price = "₹149",
            period = "/ month",
            subtitle = "Up to 5 accounts",
            features = listOf("5 separate accounts", "Family mix playlist", "Parental controls"),
            gradient = Brush.linearGradient(listOf(Color(0xFF6A11CB), Color(0xFF2575FC))),
            primaryColor = Color(0xFF2575FC),
            onClick = { onPlanSelected("fam_mo", 14900, "Family Monthly") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Student
        PlanCardGradient(
            title = "Student",
            price = "₹19",
            period = "/ month",
            subtitle = "Valid ID required",
            features = listOf("Full Individual features", "Verified student status"),
            gradient = Brush.linearGradient(listOf(Color(0xFF11998E), Color(0xFF38EF7D))),
            primaryColor = Color(0xFF11998E),
            isStudent = true,
            studentStatus = studentStatus,
            onClick = { showStudentDialog = true },
            onStudentVerify = onStudentVerify
        )

        if (isLoading) {
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(strokeWidth = 3.dp)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }

    if (showStudentDialog) {
        StudentVerificationDialog(
            onDismiss = { showStudentDialog = false },
            onSubmit = { email ->
                onStudentVerify(email)
                showStudentDialog = false
            }
        )
    }
}

@Composable
private fun PlanCardGradient(
    title: String,
    price: String,
    period: String,
    subtitle: String,
    features: List<String>,
    gradient: Brush,
    primaryColor: Color,
    badge: String? = null,
    isStudent: Boolean = false,
    studentStatus: StudentVerificationStatus = StudentVerificationStatus.IDLE,
    onClick: () -> Unit,
    onStudentVerify: (String) -> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                    if (badge != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = price,
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = period,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                features.forEach { feature ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                if (isStudent) {
                    Spacer(modifier = Modifier.height(12.dp))
                    when (studentStatus) {
                        StudentVerificationStatus.IDLE -> {
                            Button(
                                onClick = onClick,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.School, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Verify Student Status", color = Color.White)
                            }
                        }
                        StudentVerificationStatus.PENDING -> {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    "Verification Pending • 24h review",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        StudentVerificationStatus.APPROVED -> {
                            Button(
                                onClick = onClick,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Get Student Plan", color = primaryColor)
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Choose $title", color = primaryColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentVerificationDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    val isValid = email.contains("@") && email.length > 5

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Student Verification") },
        text = {
            Column {
                Text("Enter your student email address. We'll review it within 24 hours.")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Student Email") },
                    placeholder = { Text("you@university.edu") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(email) },
                enabled = isValid
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PremiumActiveContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(listOf(Color(0xFFFF6B6B), Color.Black))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "Premium",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Premium Active",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        listOf(
            "Ad-free music streaming",
            "Unlimited skips & playlists",
            "HQ audio (256kbps+)",
            "Offline downloads",
            "All themes unlocked"
        ).forEach { feature ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4ECDC4), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = feature, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/* Checkout config helper */
fun buildRazorpayOptions(orderId: String, amountPaise: Int, email: String, desc: String): JSONObject {
    return JSONObject().apply {
        put("key", "rzp_test_Sz9h9D9J7I7iie")
        put("name", "Watermelon Premium")
        put("description", desc)
        put("order_id", orderId)
        put("currency", "INR")
        put("amount", amountPaise)
        put("prefill.email", email)
        put("theme.color", "#FF0000")
        put("retry.enabled", true)
        put("retry.max_count", 2)
    }
}
