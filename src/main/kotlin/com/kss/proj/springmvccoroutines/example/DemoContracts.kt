package com.kss.proj.springmvccoroutines.example

data class LoginRequest(
    val username: String,
    val password: String,
    val email: String
)

data class LoginResponse(
    val token: String,
    val refreshToken: String,
    val expiresIn: Long
)

data class PaymentRequest(
    val amount: Double,
    val currency: String,
    val creditCardNumber: String,
    val cvv: String,
    val cardholderName: String,
    val email: String
)

data class PaymentResponse(
    val transactionId: String,
    val status: String,
    val maskedCard: String
)

data class UserProfileRequest(
    val name: String,
    val email: String,
    val phone: String,
    val ssn: String,
    val apiKey: String
)

data class UserProfileResponse(
    val id: String,
    val name: String,
    val status: String
)
