package com.example.mybussines

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.mybussines.brokers.kraken.KrakenLoginActivity
import com.example.mybussines.fragments.ChartFragment
import com.example.mybussines.fragments.DocumentsFragment
import com.example.mybussines.fragments.HomeFragment
import com.example.mybussines.fragments.ProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val loginType = intent.getStringExtra("LOGIN_TYPE")

        when (loginType) {
            "GOOGLE" -> {
                if (!isGoogleLoggedIn()) {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    return
                }
                saveLoginType("GOOGLE")
            }

            "KRAKEN" -> {
                if (!isKrakenLoggedIn()) {
                    startActivity(Intent(this, KrakenLoginActivity::class.java))
                    finish()
                    return
                }
                saveLoginType("KRAKEN")
            }

            else -> {
                val savedType = getSavedLoginType()
                when (savedType) {
                    "GOOGLE" -> {
                        if (!isGoogleLoggedIn()) {
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                            return
                        }
                    }

                    "KRAKEN" -> {
                        if (!isKrakenLoggedIn()) {
                            startActivity(Intent(this, KrakenLoginActivity::class.java))
                            finish()
                            return
                        }
                    }

                    else -> {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                        return
                    }
                }
            }
        }

        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_chart -> ChartFragment()
                R.id.nav_documents -> DocumentsFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> HomeFragment()
            }

            loadFragment(fragment)
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun isKrakenLoggedIn(): Boolean {
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

        val apiKey = prefs.getString("KRAKEN_API_KEY", null)
        val apiSecret = prefs.getString("KRAKEN_API_SECRET", null)

        return !apiKey.isNullOrEmpty() && !apiSecret.isNullOrEmpty()
    }

    private fun isGoogleLoggedIn(): Boolean {
        val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(this)
        return account != null && !account.isExpired
    }

    private fun saveLoginType(type: String) {
        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val prefs = EncryptedSharedPreferences.create(
            this,
            "app_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        prefs.edit().putString("LOGIN_TYPE", type).apply()
    }

    private fun getSavedLoginType(): String? {
        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val prefs = EncryptedSharedPreferences.create(
            this,
            "app_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        return prefs.getString("LOGIN_TYPE", null)
    }
}