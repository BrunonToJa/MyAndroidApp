package com.example.mybussines

import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class GoogleLoginManager(private val activity: Activity) {

    private val googleSignInClient: GoogleSignInClient
    private var listener: LoginResultListener? = null

    companion object {
        const val RC_SIGN_IN = 9001
    }

    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()
        googleSignInClient = GoogleSignIn.getClient(activity, gso)
    }

    fun setListener(listener: LoginResultListener) {
        this.listener = listener
    }

    fun startLogin() {
        val signInIntent = googleSignInClient.signInIntent
        activity.startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val name = account?.displayName
                val email = account?.email
                val photo = account?.photoUrl?.toString()

                listener?.onLoginSuccess(
                    name ?: email ?: "Użytkownik Google",
                    photo ?: ""
                )
            } catch (e: ApiException) {
                listener?.onLoginError("Błąd Google: ${e.statusCode}")
            }
        }
    }
}