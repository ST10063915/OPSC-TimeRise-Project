package com.opsc.timeriseprojectmain

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var editTextEmail: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        editTextEmail = findViewById(R.id.editTextEmail)
        val buttonBack: Button = findViewById(R.id.buttonBack)
        val buttonResetPassword: Button = findViewById(R.id.buttonReset)

        buttonResetPassword.setOnClickListener {
            val email = editTextEmail.text.toString()

            Authentication.resetPassword(email) { isAuthenticated ->
                if (isAuthenticated) {
                    Toast.makeText(this, "Password reset link sent to $email", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, SignInActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Could not find an account with this email $email", Toast.LENGTH_SHORT).show()
                }
            }
        }
        buttonBack.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }
    }
}
