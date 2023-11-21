package com.edpub.ecoroof

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.edpub.ecoroof.databinding.ActivityEditMapAreaBinding
import java.io.OutputStream


class EditMapAreaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditMapAreaBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditMapAreaBinding.inflate(layoutInflater)

        setContentView(binding.root)


        val uri = Uri.parse(intent.getStringExtra("uri"))
        val latitude = intent.getDoubleExtra("latitude", 0.0)
        val longitude = intent.getDoubleExtra("longitude", 0.0)

        binding.cvConfirmImage.setOnClickListener {
            val croppedImage = binding.cropImageView.getCroppedImage()
            val croppedImageUri = Utils.getUriFromBitmap(croppedImage!!, contentResolver)

            val intent = Intent(this, CalculateSavingsActivity::class.java)
            intent.putExtra("latitude", latitude)
            intent.putExtra("longitude", longitude)
            intent.putExtra("uri", croppedImageUri)
            startActivity(intent)

        }
        binding.cropImageView.setImageUriAsync(uri)
    }
}