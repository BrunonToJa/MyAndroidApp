package com.example.mybussines.fragments

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.mybussines.R

class HomeFragment : Fragment() {

    companion object {
        fun newInstance(email: String): HomeFragment {
            val fragment = HomeFragment()
            val args = Bundle()
            args.putString("EMAIL", email)
            fragment.arguments = args
            return fragment
        }
    }
}