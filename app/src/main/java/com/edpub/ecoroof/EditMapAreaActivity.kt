package com.edpub.ecoroof

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.edpub.ecoroof.databinding.ActivityEditMapAreaBinding

class EditMapAreaActivity : AppCompatActivity() {

    private lateinit var binding : ActivityEditMapAreaBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditMapAreaBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.cvConfirmImage.setOnClickListener {
            val intent = Intent(this, CalculateSavingsActivity::class.java)
            startActivity(intent)
        }

    }
}