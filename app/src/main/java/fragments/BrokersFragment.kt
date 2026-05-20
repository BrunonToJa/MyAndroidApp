package com.example.mybussines.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.mybussines.brokers.kraken.KrakenLoginActivity
import com.example.mybussines.R
import com.example.mybussines.brokers.xtb.XtbSessionStore
import com.example.mybussines.brokers.xtb.XtbWebViewActivity

class BrokersFragment : Fragment() {

    private lateinit var xtbSessionStore: XtbSessionStore

    private val xtbLoginLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            handleXtbReturn()
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_brokers, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        xtbSessionStore = XtbSessionStore(requireContext())

        view.findViewById<CardView>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<Button>(R.id.btnXTB).setOnClickListener {
            val intent = Intent(requireContext(), XtbWebViewActivity::class.java)
            xtbLoginLauncher.launch(intent)
        }

        view.findViewById<Button>(R.id.btnKraken).setOnClickListener {
            startActivity(
                Intent(requireContext(), KrakenLoginActivity::class.java)
            )
        }
    }

    private fun handleXtbReturn() {
        val session = xtbSessionStore.getSession()
        val requests = xtbSessionStore.getCapturedRequests()

        Log.d("XTB_SESSION", "isLoggedIn=${session?.isLoggedIn}")
        Log.d("XTB_SESSION", "cookiesPresent=${!session?.cookies.isNullOrBlank()}")
        Log.d("XTB_SESSION", "capturedRequests=${requests.size}")

        val important = requests.filter {
            val url = it.url.lowercase()
            url.contains("subscribebalancesummary") ||
                    url.contains("subscribepersonsummary") ||
                    url.contains("subscribeportfoliopositiongroups") ||
                    url.contains("subscribereportshistory") ||
                    url.contains("getclosedpositions") ||
                    url.contains("getclosedpositionsnetprofit") ||
                    url.contains("subscribetiles") ||
                    url.contains("subscribeordergroups")
        }

        if (important.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Nie złapano jeszcze kluczowych endpointów",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        Toast.makeText(
            requireContext(),
            "Złapano ${important.size} kluczowych endpointów",
            Toast.LENGTH_LONG
        ).show()

        important.forEachIndexed { index, req ->
            Log.d("XTB_IMPORTANT_RESULT", "[$index] ${req.method} ${req.url}")
            Log.d("XTB_IMPORTANT_RESULT_BODY", req.body ?: "null")
            Log.d("XTB_IMPORTANT_RESULT_RESPONSE", req.responsePreview ?: "null")
        }
    }
}