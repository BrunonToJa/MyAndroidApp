package com.example.mybussines

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object DataCleaner {

    fun clearAllData(context: Context) {
        clearKrakenData(context)
        clearAppData(context)
        clearGoogleData(context)
    }

    private fun clearKrakenData(context: Context) {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val prefs = EncryptedSharedPreferences.create(
                context,
                "kraken_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            prefs.edit().clear().apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun clearAppData(context: Context) {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val prefs = EncryptedSharedPreferences.create(
                context,
                "app_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            prefs.edit().clear().apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun clearGoogleData(context: Context) {
        // Wylogowanie z Google Sign-In
        try {
            val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(
                context,
                com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                    com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                ).build()
            )
            googleSignInClient.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}