package com.opsc.timeriseprojectmain

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SignInActivity : AppCompatActivity() {

    private lateinit var editTextUsername: EditText
    private lateinit var editTextPassword: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        // Initialize views
        editTextUsername = findViewById(R.id.editTextUsername)
        editTextPassword = findViewById(R.id.editTextPassword)
        val buttonSignIn: Button = findViewById(R.id.buttonSignIn)
        val buttonCreateAccount: Button = findViewById(R.id.buttonCreateAccount)
        val buttonForgotPassword: Button = findViewById(R.id.buttonForgotPassword)

        // Set click listener for sign-in button
        buttonSignIn.setOnClickListener {
            val email = editTextUsername.text.toString().toLowerCase()
            val password = editTextPassword.text.toString()

            Authentication.authenticate(email, password) { isAuthenticated ->
                if (isAuthenticated) {
                    Toast.makeText(this, "Welcome, $email!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Invalid credentials. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Set click listener for "Forgot Password" button
        buttonForgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

        // Set click listener for "Create Account" button
        buttonCreateAccount.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }
}
