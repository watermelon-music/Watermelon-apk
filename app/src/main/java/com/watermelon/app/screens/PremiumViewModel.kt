package com.watermelon.app.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watermelon.data.remote.payments.CreateOrderRequest
import com.watermelon.data.remote.payments.PaymentApi
import com.watermelon.data.remote.payments.VerifyPaymentRequest
import com.watermelon.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

enum class StudentVerificationStatus { IDLE, PENDING, APPROVED }

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val paymentApi: PaymentApi
) : ViewModel() {

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _studentStatus = MutableStateFlow(StudentVerificationStatus.IDLE)
    val studentStatus: StateFlow<StudentVerificationStatus> = _studentStatus.asStateFlow()

    private var _pendingPlan: String = ""
    val pendingPlan: String get() = _pendingPlan

    init {
        checkPremiumStatus()
    }

    private fun checkPremiumStatus() {
        _isPremium.value = false
    }

    fun createOrder(plan: String, amount: Int, onOrderCreated: (String) -> Unit) {
        _pendingPlan = plan
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val userId = authRepository.getCurrentUserId() ?: "anonymous"
                val response = paymentApi.createOrder(
                    CreateOrderRequest(
                        userId = userId,
                        amount = amount,
                        currency = "INR",
                        plan = plan
                    )
                )
                val orderId = response.orderId
                if (response.success && orderId != null) {
                    onOrderCreated(orderId)
                } else {
                    _error.value = response.message ?: "Failed to create order"
                }
            } catch (e: Exception) {
                Timber.e(e, "Create order failed")
                _error.value = "Network error: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onPaymentSuccess(paymentId: String, orderId: String, signature: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val userId = authRepository.getCurrentUserId() ?: "anonymous"
                val response = paymentApi.verifyPayment(
                    VerifyPaymentRequest(
                        orderId = orderId,
                        paymentId = paymentId,
                        signature = signature,
                        userId = userId,
                        plan = _pendingPlan
                    )
                )
                if (response.success) {
                    _isPremium.value = true
                    _error.value = null
                } else {
                    _error.value = response.message ?: "Payment verification failed"
                }
            } catch (e: Exception) {
                Timber.e(e, "Payment verification failed")
                _error.value = e.localizedMessage ?: "Verification error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onPaymentError(code: Int, message: String) {
        _error.value = "Payment failed ($code): $message"
    }

    fun submitStudentVerification(email: String) {
        viewModelScope.launch {
            _studentStatus.value = StudentVerificationStatus.PENDING
            _error.value = null
            // In production, send email to backend for manual review
            // For now, simulate pending state
            Timber.d("Student verification submitted: $email")
        }
    }

    fun clearError() {
        _error.value = null
    }
}
