package com.example.mybussines

interface LoginResultListener {
    fun onLoginSuccess(username: String, avatarUrl: String)
    fun onLoginError(error: String)
}