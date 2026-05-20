package com.example.mybussines

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.example.mybussines.brokers.kraken.KrakenLoginActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class LoginActivity : BaseActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DataCleaner.clearAllData(this)

        setContentView(R.layout.activity_login)

        // Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Guzik Google
        findViewById<Button>(R.id.btnGoogleLogin).setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

        // Guzik Kraken - otwiera formularz
        findViewById<Button>(R.id.btnKrakenLogin).setOnClickListener {
            startActivity(Intent(this, KrakenLoginActivity::class.java))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val name = account?.displayName
                val email = account?.email
                val photo = account?.photoUrl?.toString()

                Toast.makeText(this, "Witaj $name!", Toast.LENGTH_SHORT).show()

                // Po udanym logowaniu Google (w onActivityResult):
                startActivity(Intent(this, MainActivity::class.java).apply {
                    putExtra("LOGIN_TYPE", "GOOGLE")
                    putExtra("USERNAME", name ?: email ?: "Użytkownik")
                    putExtra("EMAIL", email ?: "")
                    putExtra("AVATAR", photo ?: "")
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                })
                finish()

            } catch (e: ApiException) {
                Toast.makeText(this, "Błąd logowania: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}