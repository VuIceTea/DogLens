package com.example.doglens.ui.premium

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.doglens.R
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

class PremiumFragment : Fragment(R.layout.fragment_premium) {
    private val viewModel: PremiumViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val monthly = view.findViewById<View>(R.id.premiumPlanMonthly)
        val yearly = view.findViewById<View>(R.id.premiumPlanYearly)
        val checkout = view.findViewById<TextView>(R.id.btnPremiumCheckout)
        val refresh = view.findViewById<View>(R.id.btnPremiumRefresh)
        val status = view.findViewById<TextView>(R.id.tvPremiumStatus)
        view.findViewById<View>(R.id.btnPremiumBack).setOnClickListener { parentFragmentManager.popBackStack() }
        view.findViewById<TextView>(R.id.premiumPriceMonthly).text = planPrice(PremiumPlan.MONTH)
        view.findViewById<TextView>(R.id.premiumPriceYearly).text = planPrice(PremiumPlan.YEAR)
        monthly.setOnClickListener { viewModel.select(PremiumPlan.MONTH) }
        yearly.setOnClickListener { viewModel.select(PremiumPlan.YEAR) }
        refresh.setOnClickListener { viewModel.refresh() }
        checkout.setOnClickListener {
            val current = viewModel.state.value
            if (current.resumeUrl != null) {
                viewModel.checkout()
            } else {
                val plan = current.plan
                val label = getString(if (plan == PremiumPlan.MONTH) R.string.premium_monthly else R.string.premium_yearly)
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.premium_confirm)
                    .setMessage(getString(R.string.premium_confirm_detail, label, planPrice(plan)))
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.premium_checkout) { _, _ -> viewModel.checkout() }
                    .show()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    monthly.isSelected = state.plan == PremiumPlan.MONTH
                    yearly.isSelected = state.plan == PremiumPlan.YEAR
                    view.findViewById<RadioButton>(R.id.premiumRadioMonthly).isChecked = monthly.isSelected
                    view.findViewById<RadioButton>(R.id.premiumRadioYearly).isChecked = yearly.isSelected
                    val canChoose = !state.loading && state.status != "pending"
                    monthly.isEnabled = canChoose
                    yearly.isEnabled = canChoose
                    checkout.isEnabled = !state.loading && state.status != "review"
                    checkout.setText(if (state.resumeUrl != null) R.string.premium_checkout_continue else R.string.premium_checkout)
                    refresh.isEnabled = !state.loading
                    status.text = statusMessage(state)
                    status.visibility = View.VISIBLE
                    state.openUrl?.let { url ->
                        viewModel.consumeOpenUrl()
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW, url))
                        } catch (_: ActivityNotFoundException) {
                            Toast.makeText(requireContext(), R.string.premium_browser_unavailable, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun planPrice(plan: PremiumPlan): String {
        val amount = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN")).format(plan.amount)
        return getString(R.string.premium_price_period, amount, plan.days)
    }

    private fun statusMessage(state: PremiumCheckoutState): String {
        val message = when {
            state.loading -> getString(R.string.premium_loading)
            state.error != null -> state.error
            else -> getString(when (state.status) {
                "paid" -> R.string.premium_status_paid
                "pending" -> R.string.premium_status_pending
                "failed" -> R.string.premium_status_failed
                "expired" -> R.string.premium_status_expired
                "review" -> R.string.premium_status_review
                else -> R.string.premium_status_none
            })
        }
        if (state.premiumUntil <= System.currentTimeMillis()) return message
        val date = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.forLanguageTag("vi-VN"))
            .format(Date(state.premiumUntil))
        return "$message\n${getString(R.string.premium_active_until, date)}"
    }
}
