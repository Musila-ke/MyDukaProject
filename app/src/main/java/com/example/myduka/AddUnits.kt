package com.example.myduka

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.myduka.databinding.ActivityAddUnitsBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AddUnits : AppCompatActivity() {
    private lateinit var binding: ActivityAddUnitsBinding
    private lateinit var cameraExecutor: ExecutorService

    private val barcodeScanner by lazy { BarcodeScanning.getClient() }

    private var firstScan: String? = null
    private var isScanningEnabled = true

    private var branchId: String? = null
    private var productId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddUnitsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        branchId = intent.getStringExtra("branchId")
        productId = intent.getStringExtra("productId")

        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.chipGroupMode.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = checkedIds.firstOrNull()

            when (checkedId) {
                R.id.chipBarcode -> {
                    binding.groupWithBarcode.visibility = View.VISIBLE
                    binding.tilExpiryWithBarcode.visibility = View.VISIBLE
                    binding.previewView.visibility = View.VISIBLE

                    binding.groupNoBarcode.visibility = View.GONE
                    binding.groupService.visibility = View.GONE

                    startCamera()
                }

                R.id.chipNoBarcode -> {
                    binding.groupWithBarcode.visibility = View.GONE
                    binding.tilExpiryWithBarcode.visibility = View.GONE
                    binding.previewView.visibility = View.GONE

                    binding.groupNoBarcode.visibility = View.VISIBLE
                    binding.groupService.visibility = View.GONE

                    stopCamera()
                }

                R.id.chipService -> {
                    binding.groupWithBarcode.visibility = View.GONE
                    binding.tilExpiryWithBarcode.visibility = View.GONE
                    binding.previewView.visibility = View.GONE

                    binding.groupNoBarcode.visibility = View.GONE
                    binding.groupService.visibility = View.VISIBLE

                    stopCamera()
                }

                else -> {
                    binding.groupWithBarcode.visibility = View.GONE
                    binding.groupNoBarcode.visibility = View.GONE
                    binding.groupService.visibility = View.GONE
                    stopCamera()
                }
            }
        }

        binding.chipBarcode.isChecked = true

        binding.btnScanBarcode.setOnClickListener {
            firstScan = null
            isScanningEnabled = true
            Toast.makeText(this, "Ready to scan again", Toast.LENGTH_SHORT).show()
        }

        binding.btnSaveUnit.setOnClickListener {
            binding.btnSaveUnit.isEnabled = false

            if (!isOnline()) {
                Toast.makeText(this, "No internet access", Toast.LENGTH_SHORT).show()
                binding.btnSaveUnit.isEnabled = true
                return@setOnClickListener
            }

            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid.isNullOrEmpty() || branchId.isNullOrEmpty() || productId.isNullOrEmpty()) {
                Toast.makeText(this, "Missing context", Toast.LENGTH_SHORT).show()
                binding.btnSaveUnit.isEnabled = true
                return@setOnClickListener
            }

            val db = FirebaseFirestore.getInstance()
            val unitsRef = db.collection("users").document(uid)
                .collection("branches").document(branchId!!)
                .collection("branchproducts").document(productId!!)
                .collection("units")

            when {
                binding.chipBarcode.isChecked -> handleBarcodeMode(unitsRef)
                binding.chipNoBarcode.isChecked -> handleNoBarcodeMode(db, unitsRef)
                binding.chipService.isChecked -> {
                    Toast.makeText(this, "Service recorded", Toast.LENGTH_SHORT).show()
                    finish()
                }
                else -> {
                    Toast.makeText(this, "Select a mode", Toast.LENGTH_SHORT).show()
                    binding.btnSaveUnit.isEnabled = true
                }
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101)
        }
    }

    override fun onResume() {
        super.onResume()
        if (binding.chipBarcode.isChecked) startCamera()
    }

    override fun onPause() {
        super.onPause()
        stopCamera()
    }

    private fun handleBarcodeMode(unitsRef: com.google.firebase.firestore.CollectionReference) {
        val code = binding.etBarcode.text.toString().trim()
        val expiryText = binding.etExpiryWithBarcode.text.toString().trim()
        if (code.isEmpty() || expiryText.isEmpty()) {
            Toast.makeText(this, "Enter barcode and expiry", Toast.LENGTH_SHORT).show()
            binding.btnSaveUnit.isEnabled = true
            return
        }
        val expiryTs = parseExpiry(expiryText) ?: run {
            Toast.makeText(this, "Expiry must be YYYY-MM-DD", Toast.LENGTH_SHORT).show()
            binding.btnSaveUnit.isEnabled = true
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                unitsRef.add(mapOf("barcode" to code, "expiryDate" to expiryTs)).await()

                incrementQuantity(
                    db = FirebaseFirestore.getInstance(),
                    uid = FirebaseAuth.getInstance().currentUser!!.uid,
                    branchId = branchId!!,
                    productId = productId!!,
                    amount = 1
                )

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AddUnits, "Unit saved!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AddUnits, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.btnSaveUnit.isEnabled = true
                }
            }
        }
    }

    private fun handleNoBarcodeMode(
        db: FirebaseFirestore,
        unitsRef: com.google.firebase.firestore.CollectionReference
    ) {
        val expiryText = binding.etExpiryNoBarcode.text.toString().trim()
        val quantity = binding.etQuantityNoBarcode.text.toString().trim().toIntOrNull()
        if (expiryText.isEmpty() || quantity == null || quantity <= 0) {
            Toast.makeText(this, "Enter expiry and valid quantity", Toast.LENGTH_SHORT).show()
            binding.btnSaveUnit.isEnabled = true
            return
        }
        val expiryTs = parseExpiry(expiryText) ?: run {
            Toast.makeText(this, "Expiry must be YYYY-MM-DD", Toast.LENGTH_SHORT).show()
            binding.btnSaveUnit.isEnabled = true
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val chunks = (1..quantity).chunked(450)
                for (chunk in chunks) {
                    val batch = db.batch()
                    chunk.forEach {
                        batch.set(unitsRef.document(), mapOf("expiryDate" to expiryTs))
                    }
                    batch.commit().await()
                }

                incrementQuantity(
                    db = db,
                    uid = FirebaseAuth.getInstance().currentUser!!.uid,
                    branchId = branchId!!,
                    productId = productId!!,
                    amount = quantity.toLong()
                )

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AddUnits, "Units saved!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AddUnits, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.btnSaveUnit.isEnabled = true
                }
            }
        }
    }

    private suspend fun incrementQuantity(
        db: FirebaseFirestore,
        uid: String,
        branchId: String,
        productId: String,
        amount: Long
    ) {
        val productRef = db.collection("users").document(uid)
            .collection("branches").document(branchId)
            .collection("branchproducts").document(productId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(productRef)
            val currentQty = snapshot.getLong("quantity") ?: 0
            transaction.update(productRef, "quantity", currentQty + amount)
        }.await()
    }

    private fun parseExpiry(text: String): Timestamp? {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            Timestamp(sdf.parse(text)!!)
        } catch (e: Exception) {
            null
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            val imageAnalyzer = ImageAnalysis.Builder().build().also { analyzer ->
                analyzer.setAnalyzer(cameraExecutor) { imageProxy -> processImageProxy(imageProxy) }
            }
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer)
        }, ContextCompat.getMainExecutor(this))
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        try {
            if (!isScanningEnabled) return
            val mediaImage = imageProxy.image ?: return
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    barcodes.firstOrNull { it.rawValue != null }
                        ?.rawValue
                        ?.let { handleScannedCode(it) }
                }
                .addOnFailureListener { Log.e("BarcodeScanner", "Error: ${it.message}") }
        } finally {
            imageProxy.close()
        }
    }

    private fun handleScannedCode(scanned: String) {
        runOnUiThread {
            if (firstScan == null) {
                firstScan = scanned
                Toast.makeText(this, "Scanned! Now scan again to verify.", Toast.LENGTH_SHORT).show()
            } else if (firstScan == scanned) {
                binding.etBarcode.setText(scanned)
                Toast.makeText(this, "Barcode verified!", Toast.LENGTH_SHORT).show()
                isScanningEnabled = false
            } else {
                firstScan = null
                Toast.makeText(this, "Mismatch! Try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopCamera() {
        ProcessCameraProvider.getInstance(this).addListener({
            ProcessCameraProvider.getInstance(this).get().unbindAll()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun isOnline(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

