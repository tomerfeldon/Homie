package com.example.homie.ui.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.homie.MainActivity
import com.example.homie.databinding.ActivitySplashBinding
import com.example.homie.ui.onboarding.ApartmentChoiceActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkUserState()
    }

    private fun checkUserState() {

        val currentUser = auth.currentUser

        if (currentUser == null) {
            Handler(Looper.getMainLooper()).postDelayed(
                {
                    navigateToLogin()
                }, 3000
            )
        } else {
            checkApartment(currentUser.uid)
        }
    }

    private fun checkApartment(userId: String) {
        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val apartmentId = document.getString("apartmentId")

                if (apartmentId.isNullOrEmpty()) {
                    navigateToApartmentChoice()
                } else {
                    navigateToMain()
                }
            }
            .addOnFailureListener {
                navigateToLogin()
            }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun navigateToApartmentChoice() {
        startActivity(Intent(this, ApartmentChoiceActivity::class.java))
        finish()
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

}