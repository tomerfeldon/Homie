package com.example.homie.ui.onboarding

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.homie.MainActivity
import com.example.homie.databinding.ActivityJoinApartmentBinding

class JoinApartmentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJoinApartmentBinding
    private val viewModel: ApartmentViewModel by viewModels()
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJoinApartmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Joining Apartment...")
        progressDialog.setCancelable(false)

        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.btnJoinApartment.setOnClickListener {

            val code = binding.etInviteCode.text.toString().trim()

            if (code.isNotEmpty()) {
                progressDialog.show()
                viewModel.joinApartment(code)
            } else {
                Toast.makeText(
                    this,
                    "Invite code should not be empty",
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
                Toast.makeText(this, "Joined Apartment", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity()
            }

            result.onFailure {
                Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}