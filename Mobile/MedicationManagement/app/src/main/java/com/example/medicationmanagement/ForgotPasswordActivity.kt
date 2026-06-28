package com.example.medicationmanagement

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.medicationmanagement.api.ForgotPasswordRequest
import com.example.medicationmanagement.api.RetrofitClient
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val emailInput = findViewById<EditText>(R.id.forgotEmailInput)
        val btnSendCode = findViewById<Button>(R.id.btnSendCode)
        val btnBackToLogin = findViewById<TextView>(R.id.btnBackToLogin)

        btnBackToLogin.setOnClickListener {
            finish()
        }

        btnSendCode.setOnClickListener {
            val email = emailInput.text.toString().trim()
            if (email.isBlank()) {
                Toast.makeText(this, R.string.forgot_email_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendResetCode(email)
        }
    }

    private fun sendResetCode(email: String) {
        val btnSendCode = findViewById<Button>(R.id.btnSendCode)
        btnSendCode.isEnabled = false

        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getAuthApi(this@ForgotPasswordActivity)
                val response = api.forgotPassword(ForgotPasswordRequest(email))
                // Бекенд завжди повертає 200 (security best practice)
                Toast.makeText(
                    this@ForgotPasswordActivity,
                    getString(R.string.forgot_password_code_sent, email),
                    Toast.LENGTH_LONG
                ).show()
                // Переходимо на екран введення нового пароля
                val intent = Intent(this@ForgotPasswordActivity, ResetPasswordActivity::class.java).apply {
                    putExtra("email", email)
                }
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@ForgotPasswordActivity, e.message ?: "Error", Toast.LENGTH_LONG).show()
            } finally {
                btnSendCode.isEnabled = true
            }
        }
    }
}
