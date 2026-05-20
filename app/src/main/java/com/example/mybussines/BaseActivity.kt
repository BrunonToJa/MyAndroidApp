package com.example.mybussines

import android.graphics.Color
import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun setContentView(layoutResID: Int) {
        // Utwórz główny kontener
        val rootContainer = FrameLayout(this)

        // CZARNE TŁO (bez orb, bez fal)
        rootContainer.setBackgroundColor(Color.parseColor("#070B14"))

        // Dodaj zawartość z XML (inflate)
        val contentView = layoutInflater.inflate(layoutResID, rootContainer, false)
        rootContainer.addView(contentView)

        super.setContentView(rootContainer)
    }
}