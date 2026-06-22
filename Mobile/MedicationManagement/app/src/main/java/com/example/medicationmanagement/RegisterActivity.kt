package com.example.medicationmanagement

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.medicationmanagement.api.RetrofitClient
import com.example.medicationmanagement.api.AuthApi
import com.example.medicationmanagement.api.RegisterRequest
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var registerBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val email = findViewById<EditText>(R.id.emailInput)
        val password = findViewById<EditText>(R.id.passwordInput)
        registerBtn = findViewById(R.id.registerBtn)
        val backToLogin = findViewById<TextView>(R.id.backToLogin)

        registerBtn.setOnClickListener {
            val mail = email.text.toString().trim()
            val pass = password.text.toString().trim()

            if (mail.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, R.string.register_fill_all_fields, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performRegister(mail, pass)
        }

        backToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun performRegister(email: String, password: String) {
        val originalText = registerBtn.text
        registerBtn.isEnabled = false
        registerBtn.text = getString(R.string.register_loading)

        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getAuthApi(this@RegisterActivity)
                val response = api.register(RegisterRequest(email, password))

                if (response.isSuccessful) {
                    Toast.makeText(this@RegisterActivity, R.string.register_success, Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@RegisterActivity, ConfirmEmailActivity::class.java)
                    intent.putExtra("email", email)
                    startActivity(intent)
                    finish()
                } else {
                    val code = response.code()
                    Toast.makeText(this@RegisterActivity, getString(R.string.register_failed, code), Toast.LENGTH_LONG).show()
                    registerBtn.isEnabled = true
                    registerBtn.text = originalText
                }
            } catch (e: Exception) {
                Toast.makeText(this@RegisterActivity, getString(R.string.register_network_error, e.message ?: ""), Toast.LENGTH_LONG).show()
                registerBtn.isEnabled = true
                registerBtn.text = originalText
            }
        }
    }
}
