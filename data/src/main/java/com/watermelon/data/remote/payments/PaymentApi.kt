package com.watermelon.data.remote.payments

import retrofit2.http.Body
import retrofit2.http.POST

interface PaymentApi {

    @POST("payments/create")
    suspend fun createOrder(@Body request: CreateOrderRequest): CreateOrderResponse

    @POST("payments/verify")
    suspend fun verifyPayment(@Body request: VerifyPaymentRequest): VerifyPaymentResponse
}

data class CreateOrderRequest(
    val userId: String,
    val amount: Int,
    val currency: String = "INR",
    val plan: String
)

data class CreateOrderResponse(
    val success: Boolean,
    val orderId: String?,
    val amount: Int?,
    val currency: String?,
    val message: String? = null
)

data class VerifyPaymentRequest(
    val orderId: String,
    val paymentId: String,
    val signature: String,
    val userId: String,
    val plan: String
)

data class VerifyPaymentResponse(
    val success: Boolean,
    val message: String? = null
)
