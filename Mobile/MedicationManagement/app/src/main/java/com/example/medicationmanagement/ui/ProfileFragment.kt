package com.example.medicationmanagement.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.medicationmanagement.R
import com.example.medicationmanagement.api.RetrofitClient
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val progressBar = view.findViewById<ProgressBar>(R.id.profileProgressBar)
        val emailView = view.findViewById<MaterialTextView>(R.id.profileEmail)
        val roleView = view.findViewById<MaterialTextView>(R.id.profileRole)
        val orgView = view.findViewById<MaterialTextView>(R.id.profileOrg)

        progressBar.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val api = RetrofitClient.getAuthApi(requireContext())
                val response = api.getMe()
                if (response.isSuccessful) {
                    val me = response.body()
                    emailView.text = me?.email ?: "—"
                    val displayRoles = me?.roles?.map { role ->
                        when (role.lowercase()) {
                            "administrator" -> getString(R.string.role_administrator)
                            "manager" -> getString(R.string.role_manager)
                            "user" -> getString(R.string.role_user)
                            else -> role
                        }
                    }?.joinToString(", ") ?: "—"
                    roleView.text = displayRoles
                    orgView.text = me?.organizationId ?: "—"
                } else {
                    Toast.makeText(requireContext(), R.string.profile_error, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.message ?: getString(R.string.profile_error), Toast.LENGTH_LONG).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
}
