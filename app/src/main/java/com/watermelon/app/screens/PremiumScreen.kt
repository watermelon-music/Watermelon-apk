package com.watermelon.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.watermelon.core.designsystem.theme.WatermelonRed
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
                title = { Text("Premium") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isPremium) {
            PremiumActiveContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
            )
        } else {
            PremiumUpsellContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                isLoading = isLoading,
                onStartCheckout = onStartCheckout
            )
        }
    }
}

@Composable
private fun PremiumUpsellContent(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    onStartCheckout: (Int, String, String) -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(WatermelonRed, Color.Black)
                    )
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

        Text(
            text = "Go Premium",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Unlock unlimited playlists, HQ audio, and 10+ themes.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        PremiumFeatureItem(text = "Unlimited Playlists")
        PremiumFeatureItem(text = "High Quality Audio (256kbps+)")
        PremiumFeatureItem(text = "10+ Premium Themes")
        PremiumFeatureItem(text = "Family Sharing (5 members)")

        Spacer(modifier = Modifier.height(8.dp))

        val plans = listOf(
            Triple("ind_mo", "Individual Monthly", 4900),
            Triple("ind_yr", "Individual Yearly", 50000),
            Triple("fam_mo", "Family Monthly (5)", 10000),
            Triple("fam_yr", "Family Yearly (5)", 100000)
        )

        plans.forEach { (key, label, paise) ->
            PlanCard(
                label = label,
                price = when (key) {
                    "ind_mo" -> "₹49 / month"
                    "ind_yr" -> "₹500 / year"
                    "fam_mo" -> "₹100 / month"
                    "fam_yr" -> "₹1000 / year"
                    else -> ""
                },
                onClick = {
                    if (!isLoading) {
                        viewModel.createOrder(key, paise) { orderId ->
                            onStartCheckout(orderId, paise, label)
                        }
                    }
                }
            )
        }

        if (isLoading) {
            Spacer(modifier = Modifier.height(8.dp))
            CircularProgressIndicator(color = WatermelonRed)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PlanCard(label: String, price: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = price,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = WatermelonRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Pay", color = Color.White)
            }
        }
    }
}

@Composable
private fun PremiumFeatureItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = WatermelonRed,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PremiumActiveContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Premium Active",
            style = MaterialTheme.typography.headlineMedium,
            color = WatermelonRed
        )
        Spacer(modifier = Modifier.height(16.dp))
        PremiumFeatureItem(text = "Unlimited playlists enabled")
        PremiumFeatureItem(text = "HQ audio enabled")
        PremiumFeatureItem(text = "All themes unlocked")
    }
}

/* Checkout config helper (Razorpay expects JSONObject) */
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
