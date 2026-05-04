package com.example.medicationmanagement

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.medicationmanagement.api.ApiClient
import com.example.medicationmanagement.api.AuthApi
import com.example.medicationmanagement.api.LoginRequest
import com.example.medicationmanagement.utils.TokenManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Auto-login check
        if (TokenManager.getInstance(this).isLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        
        setContentView(R.layout.activity_login)

        emailField = findViewById(R.id.emailInput)
        passwordField = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginBtn)
        registerButton = findViewById(R.id.registerLink)

        loginButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performLogin(email, password)
        }

        registerButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun performLogin(email: String, password: String) {
        val originalText = loginButton.text
        loginButton.isEnabled = false
        loginButton.text = "Loading..."
        
        lifecycleScope.launch {
            try {
                val api = ApiClient.createService<AuthApi>(this@LoginActivity)
                val response = api.login(LoginRequest(email, password))

                if (response.isSuccessful) {
                    val token = response.body()?.token
                    if (token != null) {
                        TokenManager.getInstance(this@LoginActivity).saveToken(token)
                        Toast.makeText(this@LoginActivity, "Login successful", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Invalid response from server", Toast.LENGTH_SHORT).show()
                        loginButton.isEnabled = true
                        loginButton.text = originalText
                    }
                } else {
                    val code = response.code()
                    if (code == 401) {
                        Toast.makeText(this@LoginActivity, "Invalid email or password", Toast.LENGTH_SHORT).show()
                    } else if (code == 403) {
                        Toast.makeText(this@LoginActivity, "Email is not confirmed. Please check your inbox.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@LoginActivity, "Login failed: $code", Toast.LENGTH_SHORT).show()
                    }
                    loginButton.isEnabled = true
                    loginButton.text = originalText
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                loginButton.isEnabled = true
                loginButton.text = originalText
            }
        }
    }
}