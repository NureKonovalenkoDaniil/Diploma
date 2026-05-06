package com.example.medicationmanagement

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.medicationmanagement.api.ApiClient
import com.example.medicationmanagement.api.AuthApi
import com.example.medicationmanagement.api.ConfirmEmailRequest
import com.example.medicationmanagement.api.ResendConfirmationRequest
import kotlinx.coroutines.launch

class ConfirmEmailActivity : AppCompatActivity() {

    private lateinit var email: String
    private lateinit var codeInput: EditText
    private lateinit var confirmBtn: Button
    private lateinit var resendBtn: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm_email)

        email = intent.getStringExtra("email") ?: ""

        codeInput = findViewById(R.id.codeInput)
        confirmBtn = findViewById(R.id.confirmBtn)
        resendBtn = findViewById(R.id.resendBtn)
        progressBar = findViewById(R.id.confirmProgressBar)

        val emailLabel = findViewById<TextView>(R.id.emailLabel)
        emailLabel.text = "Код підтвердження надіслано на: $email"

        confirmBtn.setOnClickListener {
            val code = codeInput.text.toString().trim()
            if (code.length != 6) {
                Toast.makeText(this, "Введіть 6-значний код", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            confirmEmail(code)
        }

        resendBtn.setOnClickListener {
            resendCode()
        }
    }

    private fun confirmEmail(code: String) {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val api = ApiClient.createService<AuthApi>(this@ConfirmEmailActivity)
                val response = api.confirmEmail(ConfirmEmailRequest(email, code))

                if (response.isSuccessful) {
                    Toast.makeText(this@ConfirmEmailActivity, "Email підтверджено! Тепер можна увійти.", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@ConfirmEmailActivity, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                } else {
                    Toast.makeText(this@ConfirmEmailActivity, "Невірний або прострочений код", Toast.LENGTH_LONG).show()
                    setLoading(false)
                }
            } catch (e: Exception) {
                Toast.makeText(this@ConfirmEmailActivity, "Помилка мережі: ${e.message}", Toast.LENGTH_LONG).show()
                setLoading(false)
            }
        }
    }

    private fun resendCode() {
        if (email.isBlank()) return
        setLoading(true)
        lifecycleScope.launch {
            try {
                val api = ApiClient.createService<AuthApi>(this@ConfirmEmailActivity)
                val response = api.resendConfirmation(ResendConfirmationRequest(email))
                Toast.makeText(
                    this@ConfirmEmailActivity,
                    if (response.isSuccessful) "Новий код надіслано на $email" else "Не вдалося надіслати код",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(this@ConfirmEmailActivity, "Помилка мережі", Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        confirmBtn.isEnabled = !loading
        resendBtn.isEnabled = !loading
    }
}
