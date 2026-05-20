package com.example.mybussines.brokers.kraken

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.mybussines.BaseActivity
import com.example.mybussines.MainActivity
import com.example.mybussines.R
import kotlinx.coroutines.*

class KrakenLoginActivity : BaseActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kraken_login)

        val etApiKey = findViewById<EditText>(R.id.etApiKey)
        val etApiSecret = findViewById<EditText>(R.id.etApiSecret)
        val btnLogin = findViewById<Button>(R.id.btnKrakenSubmit)
        val btnBack = findViewById<Button>(R.id.btnBack)



        btnLogin.setOnClickListener {
            val apiKey = etApiKey.text.toString().trim()
            val apiSecret = etApiSecret.text.toString().trim()

            if (apiKey.isEmpty() || apiSecret.isEmpty()) {
                Toast.makeText(this, "Wypełnij oba pola", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            verifyKrakenCredentials(apiKey, apiSecret)
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun verifyKrakenCredentials(apiKey: String, apiSecret: String) {
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    // PRAWDZIWE SPRAWDZENIE Z API KRAKEN
                    val nonce = System.currentTimeMillis().toString()
                    val apiPath = "/0/private/Balance"
                    val postData = "nonce=$nonce"
                    val signature = KrakenAuth.signRequest(apiPath, nonce, postData, apiSecret)

                    val retrofit = Retrofit.Builder()
                        .baseUrl("https://api.kraken.com/")
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()

                    val api = retrofit.create(KrakenApi::class.java)
                    val response = api.getBalance(apiKey, signature, nonce)

                    if (response.isSuccessful && response.body()?.error?.isEmpty() == true) {
                        "OK"
                    } else {
                        "Błąd: Nieprawidłowe klucze API"
                    }
                }

                if (result == "OK") {
                    saveKrakenCredentials(apiKey, apiSecret)
                    Toast.makeText(this@KrakenLoginActivity, "Zalogowano przez Kraken!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@KrakenLoginActivity, MainActivity::class.java).apply {
                        putExtra("LOGIN_TYPE", "KRAKEN")
                        putExtra("USERNAME", "Kraken User")
                        putExtra("EMAIL", apiKey.take(8) + "...")
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                    finish()
                } else {
                    Toast.makeText(this@KrakenLoginActivity, result, Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Toast.makeText(this@KrakenLoginActivity, "Błąd połączenia: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveKrakenCredentials(apiKey: String, apiSecret: String) {
        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val prefs = EncryptedSharedPreferences.create(
            this,
            "kraken_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        prefs.edit()
            .putString("KRAKEN_API_KEY", apiKey)
            .putString("KRAKEN_API_SECRET", apiSecret)
            .apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}