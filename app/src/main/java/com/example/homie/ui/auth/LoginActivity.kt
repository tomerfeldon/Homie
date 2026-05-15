package com.example.homie.ui.auth

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.homie.MainActivity
import com.example.homie.databinding.ActivityLoginBinding
import com.example.homie.ui.onboarding.ApartmentChoiceActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Logging in...")
        progressDialog.setCancelable(false)

        binding.btnLogin.setOnClickListener {
            loginUser()
        }

        binding.llRegister.setOnClickListener {
            startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
        }

        binding.tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    private fun loginUser() {

        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        progressDialog.show()

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->

                val userId = authResult.user?.uid ?: return@addOnSuccessListener

                firestore.collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener { document ->

                        progressDialog.dismiss()

                        val apartmentId = document.getString("apartmentId")

                        if (apartmentId.isNullOrEmpty()) {
                            navigateToApartmentChoice()
                        } else {
                            navigateToMain()
                        }
                    }
                    .addOnFailureListener {
                        progressDialog.dismiss()
                        Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                progressDialog.dismiss()
                Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToApartmentChoice() {
        startActivity(Intent(this, ApartmentChoiceActivity::class.java))
        finish()
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showForgotPasswordDialog() {
        val emailInput = com.google.android.material.textfield.TextInputEditText(this)
        emailInput.hint = "Email"
        emailInput.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        val container = android.widget.FrameLayout(this).apply {
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, 0, padding, 0)
            addView(emailInput)
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Reset Password")
            .setMessage("Enter your email address and we'll send you a reset link.")
            .setView(container)
            .setPositiveButton("Send") { _, _ ->
                val email = emailInput.text.toString().trim()
                if (email.isEmpty()) {
                    Toast.makeText(this, "Enter your email", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Check your email for a reset link", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, it.message ?: "Failed to send email", Toast.LENGTH_LONG).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}