package com.example.mybussines.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.mybussines.brokers.kraken.KrakenApi
import com.example.mybussines.brokers.kraken.KrakenAuth
import com.example.mybussines.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class DocumentsFragment : Fragment() {

    private lateinit var tvBalance: TextView
    private val TAG = "DocumentsFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_documents, container, false)
        tvBalance = view.findViewById(R.id.tvEmail)
        loadBitcoinBalance()
        return view
    }

    private fun loadBitcoinBalance() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Loading Bitcoin balance...")

                val masterKey = MasterKey.Builder(requireContext())
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                val prefs = EncryptedSharedPreferences.create(
                    requireContext(),
                    "kraken_secure_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )

                val apiKey = prefs.getString("KRAKEN_API_KEY", null)
                val apiSecret = prefs.getString("KRAKEN_API_SECRET", null)

                if (apiKey.isNullOrEmpty() || apiSecret.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        tvBalance.text = "Brak kluczy API"
                    }
                    return@launch
                }

                val retrofit = Retrofit.Builder()
                    .baseUrl("https://api.kraken.com")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val api = retrofit.create(KrakenApi::class.java)

                // Pobierz saldo
                val nonce = System.currentTimeMillis().toString()
                val apiPath = "/0/private/Balance"
                val postData = "nonce=$nonce"
                val signature = KrakenAuth.signRequest(apiPath, nonce, postData, apiSecret)

                val balanceResponse = api.getBalance(apiKey, signature, nonce)

                if (!balanceResponse.isSuccessful || balanceResponse.body()?.result == null) {
                    withContext(Dispatchers.Main) {
                        tvBalance.text = "Błąd: ${balanceResponse.code()}"
                    }
                    return@launch
                }

                val result = balanceResponse.body()!!.result!!

                // Szukaj BTC - spróbuj różne klucze
                var btcBalance: String? = null
                val btcKeys = listOf("XXBT", "XBT", "BTC")

                for (key in btcKeys) {
                    if (key in result) {
                        btcBalance = result[key]
                        Log.d(TAG, "Found BTC with key: $key = $btcBalance")
                        break
                    }
                }

                withContext(Dispatchers.Main) {
                    if (btcBalance != null) {
                        // Wyświetl pełną wartość bez zaokrąglania
                        tvBalance.text = "$btcBalance BTC"
                        Log.d(TAG, "Displayed: $btcBalance BTC")
                    } else {
                        tvBalance.text = "Brak Bitcoin"
                        Log.d(TAG, "Bitcoin not found. Available keys: ${result.keys}")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Exception", e)
                withContext(Dispatchers.Main) {
                    tvBalance.text = "Błąd: ${e.message}"
                }
            }
        }
    }
}