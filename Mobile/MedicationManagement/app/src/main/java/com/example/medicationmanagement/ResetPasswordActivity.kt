package com.example.medicationmanagement

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.medicationmanagement.api.ResetPasswordRequest
import com.example.medicationmanagement.api.RetrofitClient
import kotlinx.coroutines.launch

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var email: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        email = intent.getStringExtra("email") ?: ""

        val codeInput = findViewById<EditText>(R.id.resetCodeInput)
        val newPasswordInput = findViewById<EditText>(R.id.resetNewPasswordInput)
        val confirmPasswordInput = findViewById<EditText>(R.id.resetConfirmPasswordInput)
        val btnReset = findViewById<Button>(R.id.btnResetPassword)

        btnReset.setOnClickListener {
            val code = codeInput.text.toString().trim()
            val newPassword = newPasswordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            if (code.isBlank()) {
                Toast.makeText(this, R.string.reset_password_code_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPassword.length < 6) {
                Toast.makeText(this, R.string.reset_password_too_short, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPassword != confirmPassword) {
                Toast.makeText(this, R.string.reset_password_mismatch, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            resetPassword(code, newPassword)
        }
    }

    private fun resetPassword(code: String, newPassword: String) {
        val btnReset = findViewById<Button>(R.id.btnResetPassword)
        btnReset.isEnabled = false

        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getAuthApi(this@ResetPasswordActivity)
                val response = api.resetPassword(ResetPasswordRequest(email, code, newPassword))
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@ResetPasswordActivity,
                        R.string.reset_password_success,
                        Toast.LENGTH_LONG
                    ).show()
                    // Повертаємось на логін
                    val intent = Intent(this@ResetPasswordActivity, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        this@ResetPasswordActivity,
                        "Error ${response.code()}: Check your code or password requirements",
                        Toast.LENGTH_LONG
                    ).show()
                    btnReset.isEnabled = true
                }
            } catch (e: Exception) {
                Toast.makeText(this@ResetPasswordActivity, e.message ?: "Error", Toast.LENGTH_LONG).show()
                btnReset.isEnabled = true
            }
        }
    }
}
