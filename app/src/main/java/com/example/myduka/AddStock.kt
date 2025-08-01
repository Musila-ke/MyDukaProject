package com.example.myduka

import android.Manifest
import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.myduka.databinding.ActivityAddStockBinding
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.ArrayAdapter

class AddStock : AppCompatActivity() {

    private lateinit var binding: ActivityAddStockBinding
    private var tempCaptureUri: Uri? = null
    private var pfpUri: Uri? = null
    private val slotUris = arrayOfNulls<Uri>(3)

    private enum class Slot { PFP, ADD1, ADD2, ADD3 }
    private var currentSlot = Slot.PFP

    private lateinit var permLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var cropLauncher: ActivityResultLauncher<Uri>

    private lateinit var textRecognizer: TextRecognizer
    private val recognitionQueue = Channel<Pair<Uri, Slot>>(Channel.UNLIMITED)
    private val recogScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var branchListenerReg: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddStockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup for the Type spinner
        val typeOptions = listOf("Product", "Service")
        val typeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            typeOptions
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.typeSpinner.adapter = typeAdapter

        // Setup for the Lifespan spinner
        val lifespanOptions = listOf(
            "None",
            "Very Perishable (Days to 2 Weeks)",
            "Short Lifespan (2 Weeks to 3 Months)",
            "Medium Lifespan (3 Months to 1 Year)",
            "Long Lifespan (1 to 3 Years)",
            "Very Long Lifespan (3+ Years)"
        )
        val lifespanAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            lifespanOptions
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.lifespanSpinner.adapter = lifespanAdapter

        setupBranchChips()

        // OCR setup
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recogScope.launch {
            for ((uri, slot) in recognitionQueue) {
                try {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@AddStock, "Running OCR for $slot…", Toast.LENGTH_SHORT).show()
                    }
                    val img = InputImage.fromFilePath(this@AddStock, uri)
                    val result = textRecognizer.process(img).await()
                    val text = result.text.trim()
                    withContext(Dispatchers.Main) {
                        when (slot) {
                            Slot.ADD1 -> binding.productEditText.setText(text)
                            Slot.ADD2 -> binding.descriptionEditText.setText(text)
                            Slot.ADD3 -> binding.descriptionEditText.append("\n$text")
                            else -> {}
                        }
                        Toast.makeText(this@AddStock, "OCR complete for $slot", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("AddStockOCR", "OCR failed for $slot", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@AddStock, "OCR error for $slot", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Launchers
        permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            if (granted.values.all { it }) launchCamera()
            else Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
        }
        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
            if (ok && tempCaptureUri != null) cropLauncher.launch(tempCaptureUri!!)
            else Toast.makeText(this, "Capture failed", Toast.LENGTH_SHORT).show()
        }
        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { cropLauncher.launch(it) }
        }
        cropLauncher = registerForActivityResult(CropActivityContract()) { uri ->
            if (uri != null) {
                // Downsample & compress the cropped image
                uri.path?.let { path ->
                    val bmp = decodeSampledBitmap(path, 800, 800)
                    FileOutputStream(File(path)).use { out ->
                        bmp.compress(Bitmap.CompressFormat.JPEG, 80, out)
                    }
                }
                // Display and store URI
                when (currentSlot) {
                    Slot.PFP -> binding.imageViewItempfp.setImageURI(uri).also { pfpUri = uri }
                    else -> {
                        val idx = currentSlot.ordinal - 1
                        slotUris[idx] = uri
                        listOf(binding.additionalImage1, binding.additionalImage2, binding.additionalImage3)[idx]
                            .setImageURI(uri)
                    }
                }
                if (currentSlot != Slot.PFP) {
                    recognitionQueue.trySend(uri to currentSlot)
                    Toast.makeText(this, "Queued $currentSlot for OCR", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Crop canceled", Toast.LENGTH_SHORT).show()
            }
        }

        // Image pickers
        binding.imageViewItempfp.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Select Image")
                .setItems(arrayOf("Camera", "Gallery")) { _, which ->
                    when (which) {
                        0 -> { currentSlot = Slot.PFP; permLauncher.launch(arrayOf(Manifest.permission.CAMERA)) }
                        1 -> { currentSlot = Slot.PFP; galleryLauncher.launch("image/*") }
                    }
                }.show()
        }
        binding.additionalImage1.setOnClickListener { showSlotToast(Slot.ADD1) }
        binding.additionalImage2.setOnClickListener { showSlotToast(Slot.ADD2) }
        binding.additionalImage3.setOnClickListener { showSlotToast(Slot.ADD3) }
        binding.aiCamera.setOnClickListener {
            val next = slotUris.indexOfFirst { it == null }
            if (next >= 0) showSlotToast(Slot.values()[next + 1])
            else Toast.makeText(this, "Max 3 images", Toast.LENGTH_SHORT).show()
        }

        // Deletes
        binding.delete1.setOnClickListener { slotUris[0] = null; binding.additionalImage1.setImageResource(R.drawable.image) }
        binding.delete2.setOnClickListener { slotUris[1] = null; binding.additionalImage2.setImageResource(R.drawable.image) }
        binding.delete3.setOnClickListener { slotUris[2] = null; binding.additionalImage3.setImageResource(R.drawable.image) }

        // Save
        binding.buttonSave.setOnClickListener {
            // Check for internet connectivity
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            val isConnected = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

            if (!isConnected) {
                Toast.makeText(this, "No internet access", Toast.LENGTH_SHORT).show()
                binding.progressBarAddProduct.visibility = View.GONE
                return@setOnClickListener
            }

            // Proceed with existing logic
            binding.progressBarAddProduct.visibility = View.VISIBLE
            lifecycleScope.launch {
                binding.buttonSave.isEnabled = false

                val user = FirebaseAuth.getInstance().currentUser
                val uid = user?.uid
                if (uid.isNullOrEmpty()) {
                    Toast.makeText(this@AddStock, "You must be signed in", Toast.LENGTH_SHORT).show()
                    resetSaveUI()
                    return@launch
                }

                val profile = pfpUri
                val name = binding.productEditText.text.toString().trim()
                val desc = binding.descriptionEditText.text.toString().trim()
                val priceText = binding.priceEditText.text.toString().trim()
                val vatText = binding.vatEditText.text.toString().trim()

                val price = priceText.toDoubleOrNull()
                val vatPercent = vatText.toDoubleOrNull()

                val checkedId = binding.branchChipGroup.checkedChipId
                val branchId = binding.branchChipGroup.findViewById<Chip>(checkedId)?.tag as? String

                if (branchId.isNullOrEmpty() ||
                    profile == null ||
                    name.isEmpty() ||
                    desc.isEmpty() ||
                    price == null ||
                    vatPercent == null
                ) {
                    Toast.makeText(this@AddStock, "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
                    resetSaveUI()
                    return@launch
                }

                try {
                    val db = FirebaseFirestore.getInstance()
                    val stor = FirebaseStorage.getInstance()
                    val branchProductsRef = db.collection("users").document(uid)
                        .collection("branches").document(branchId)
                        .collection("branchproducts")
                    val productDoc = branchProductsRef.document()
                    val productId = productDoc.id

                    val imgRef = stor.reference
                        .child("users/$uid/branches/$branchId/branchproducts/$productId/profile.jpg")
                    imgRef.putFile(profile).await()
                    val url = imgRef.downloadUrl.await().toString()

                    val type = binding.typeSpinner.selectedItem as? String
                    val lifespanCategory = binding.lifespanSpinner.selectedItem as? String

                    if (lifespanCategory == null || type == null) {
                        Toast.makeText(this@AddStock, "Error reading type or lifespan category", Toast.LENGTH_SHORT).show()
                        resetSaveUI()
                        return@launch
                    }

                    val data = mapOf(
                        "id" to productId,
                        "name" to name,
                        "description" to desc,
                        "price" to price,
                        "VAT" to vatPercent,
                        "imageUrl" to url,
                        "type" to type,
                        "lifespanCategory" to lifespanCategory,
                        "quantity" to 0
                    )
                    productDoc.set(data).await()

                    cleanupTempFiles()
                    Toast.makeText(this@AddStock, "Saved!", Toast.LENGTH_SHORT).show()
                    finish()
                } catch (e: Exception) {
                    Log.e("AddStockSave", "failed", e)
                    Toast.makeText(this@AddStock, "Save error: ${e.message}", Toast.LENGTH_LONG).show()
                    resetSaveUI()
                }
            }
        }
    }

    private fun resetSaveUI() {
        binding.progressBarAddProduct.visibility = View.GONE
        binding.buttonSave.isEnabled = true
    }

    private fun cleanupTempFiles() {
        tempCaptureUri?.path?.let { path ->
            File(path).takeIf { it.exists() }?.delete()
        }
        slotUris.forEach { uri ->
            uri?.path?.let { path ->
                File(path).takeIf { it.exists() }?.delete()
            }
        }
    }

    private fun setupBranchChips() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        branchListenerReg = FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .collection("branches")
            .addSnapshotListener { snap, error ->
                if (error != null) return@addSnapshotListener
                binding.branchChipGroup.removeAllViews()
                snap?.forEach { doc ->
                    val name = doc.getString("name") ?: return@forEach
                    val chip = Chip(this).apply {
                        text = name
                        isCheckable = true
                        tag = doc.id
                    }
                    binding.branchChipGroup.addView(chip)
                }
            }
    }

    private fun showSlotToast(slot: Slot) {
        currentSlot = slot
        val msg = when (slot) {
            Slot.PFP -> "Take product photo"
            Slot.ADD1 -> "Take picture of the title"
            Slot.ADD2 -> "Take picture of the description"
            Slot.ADD3 -> "Take picture of the description"
        }
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        permLauncher.launch(arrayOf(Manifest.permission.CAMERA))
    }

    private fun createTempUri(prefix: String): Uri {
        val dir = externalCacheDir ?: cacheDir
        if (!dir.exists()) dir.mkdirs()
        return FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            File.createTempFile("$prefix${System.currentTimeMillis()}", ".jpg", dir)
        )
    }

    private fun launchCamera() {
        tempCaptureUri = createTempUri("captured_")
        cameraLauncher.launch(tempCaptureUri!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        branchListenerReg?.remove()
        recogScope.cancel()
        textRecognizer.close()
    }
}

// Helpers

private fun decodeSampledBitmap(path: String, reqW: Int, reqH: Int) =
    BitmapFactory.Options().run {
        inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, this)
        inSampleSize = calculateInSampleSize(this, reqW, reqH)
        inJustDecodeBounds = false
        BitmapFactory.decodeFile(path, this)
    }

private fun calculateInSampleSize(options: BitmapFactory.Options, reqW: Int, reqH: Int): Int {
    val (height, width) = options.outHeight to options.outWidth
    var inSampleSize = 1
    if (height > reqH || width > reqW) {
        val halfH = height / 2
        val halfW = width / 2
        while (halfH / inSampleSize >= reqH && halfW / inSampleSize >= reqW) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}


private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
    }

private suspend fun com.google.firebase.storage.UploadTask.await(): com.google.firebase.storage.UploadTask.TaskSnapshot =
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
    }