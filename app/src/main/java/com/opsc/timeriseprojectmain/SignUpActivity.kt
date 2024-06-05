package com.opsc.timeriseprojectmain

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class SignUpActivity : AppCompatActivity() {

    private lateinit var editTextUsername: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var editTextDateOfBirth: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // Initialize views
        editTextUsername = findViewById(R.id.editTextUsername)
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        editTextDateOfBirth = findViewById(R.id.editTextDateOfBirth)
        val buttonSignUp: Button = findViewById(R.id.buttonCreateAccount)
        val buttonBack: Button = findViewById(R.id.buttonBack)

        // Set click listener for sign-up button
        buttonSignUp.setOnClickListener {
            val username = editTextUsername.text.toString()
            val email = editTextEmail.text.toString()
            val password = editTextPassword.text.toString()
            val dateOfBirth = editTextDateOfBirth.text.toString()

            Authentication.createAccount(email, password, username, dateOfBirth) { isAuthenticated ->
                if (isAuthenticated) {
                    Toast.makeText(this, "Account created for $username", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, SignInActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Could not create account for $username", Toast.LENGTH_SHORT).show()
                }
            }
        }

        editTextDateOfBirth.setOnClickListener { showDatePickerDialog() }

        buttonBack.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showDatePickerDialog() {
        // Get Current Date
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, year, monthOfYear, dayOfMonth ->
            editTextDateOfBirth.setText("$dayOfMonth-${monthOfYear + 1}-$year")
        }, year, month, day).show()
    }
}
