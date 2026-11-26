package com.example.doglens.ui.premium

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.doglens.R

class PremiumFragment : Fragment(R.layout.fragment_premium) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val goBack = View.OnClickListener { parentFragmentManager.popBackStack() }
        view.findViewById<View>(R.id.btnPremiumBack).setOnClickListener(goBack)
        view.findViewById<View>(R.id.btnPremiumBackToProfile).setOnClickListener(goBack)
    }
}
