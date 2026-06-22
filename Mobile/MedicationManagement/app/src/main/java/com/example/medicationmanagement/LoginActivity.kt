package com.example.medicationmanagement

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.medicationmanagement.utils.TokenManager
import androidx.lifecycle.lifecycleScope
import com.example.medicationmanagement.api.RetrofitClient
import com.example.medicationmanagement.api.LoginRequest
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (TokenManager.getInstance(this).isLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginButton = findViewById<Button>(R.id.loginBtn)
        val registerLink = findViewById<TextView>(R.id.registerLink)

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, R.string.login_fields_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performLogin(email, password)
        }

        registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun performLogin(email: String, password: String) {
        val loginButton = findViewById<Button>(R.id.loginBtn)
        loginButton.isEnabled = false
        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getAuthApi(this@LoginActivity)
                val response = api.login(LoginRequest(email, password))
                if (response.isSuccessful) {
                    val token = response.body()?.token
                    if (token.isNullOrBlank()) {
                        Toast.makeText(this@LoginActivity, R.string.login_failed, Toast.LENGTH_SHORT).show()
                        loginButton.isEnabled = true
                        return@launch
                    }
                    TokenManager.getInstance(this@LoginActivity).saveToken(token)
                    TokenManager.getInstance(this@LoginActivity).saveUserEmail(email)
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, R.string.login_failed, Toast.LENGTH_SHORT).show()
                    loginButton.isEnabled = true
                }
            } catch (exception: Exception) {
                Toast.makeText(this@LoginActivity, exception.message ?: getString(R.string.login_failed), Toast.LENGTH_LONG).show()
                loginButton.isEnabled = true
            }
        }
    }
}