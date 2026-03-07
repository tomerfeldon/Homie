package com.example.homie.ui.onboarding

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.homie.MainActivity
import com.example.homie.databinding.ActivityCreateApartmentBinding

class CreateApartmentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateApartmentBinding
    private val viewModel: ApartmentViewModel by viewModels()
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateApartmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Creating Apartment...")
        progressDialog.setCancelable(false)

        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.btnCreate.setOnClickListener {

            val name = binding.etName.text.toString().trim()

            if (name.isNotEmpty()) {
                progressDialog.show()
                viewModel.createApartment(name)
            } else {
                Toast.makeText(
                    this,
                    "Apartment name should not be empty",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        observeViewModel()
    }

    private fun observeViewModel() {

        viewModel.apartmentState.observe(this) { result ->

            progressDialog.dismiss()

            result.onSuccess {
                Toast.makeText(this, "Apartment Created", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity()
            }

            result.onFailure {
                Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}