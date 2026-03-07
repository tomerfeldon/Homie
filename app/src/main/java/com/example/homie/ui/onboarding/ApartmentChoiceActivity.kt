package com.example.homie.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.homie.R
import com.example.homie.databinding.ActivityApartmentChoiceBinding

class ApartmentChoiceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityApartmentChoiceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApartmentChoiceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCreateApartment.setOnClickListener {
            startActivity(Intent(this, CreateApartmentActivity::class.java))
        }

        binding.btnJoinApartment.setOnClickListener {
            startActivity(Intent(this, JoinApartmentActivity::class.java))
        }
    }
}