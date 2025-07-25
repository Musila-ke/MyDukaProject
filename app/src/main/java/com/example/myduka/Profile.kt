package com.example.myduka

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myduka.databinding.ActivityProfileBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream

class Profile : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private lateinit var workerAdapter: WorkerAdapter

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private var userListener: ListenerRegistration? = null
    private var workersListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.profileToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        getUserData()
        setupRecyclerView()
        listenToWorkers()
        initImagePicker()

        binding.buttonEditProfile.setOnClickListener {
            ProfileDF().show(supportFragmentManager, "ProfileDF")
        }

        binding.AddWorker.setOnClickListener {
            worker_dialog_fragment().show(supportFragmentManager, "worker_dialog_fragment")
        }

        binding.imageViewProfilePicture.setOnClickListener {
            showImagePickerOptions()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    private fun initImagePicker() {
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                setProfileImage(it)
                uploadImageToFirebaseStorage(it)
            }
        }
    }

    private fun showImagePickerOptions() {
        val options = arrayOf("Camera", "Gallery")
        AlertDialog.Builder(this)
            .setTitle("Choose Image Source")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermission()
                    1 -> checkStoragePermission()
                }
            }
            .show()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101)
        } else {
            openCamera()
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, 102)
    }

    private fun getImageUriFromBitmap(bitmap: Bitmap): Uri {
        val file = File.createTempFile("camera_image", ".jpg", cacheDir)
        val out = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        out.flush()
        out.close()
        return Uri.fromFile(file)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 102 && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as? Bitmap
            imageBitmap?.let {
                val uri = getImageUriFromBitmap(it)
                setProfileImage(uri)
                uploadImageToFirebaseStorage(uri)
            }
        }
    }

    private fun setProfileImage(uri: Uri) {
        Glide.with(this)
            .load(uri)
            .circleCrop()
            .into(binding.imageViewProfilePicture)
    }

    private fun uploadImageToFirebaseStorage(imageUri: Uri) {
        val user = auth.currentUser
        if (user == null) return handleAuthExpired()

        val ref = storage.reference.child("users/${user.uid}/profile.jpg")
        ref.putFile(imageUri)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful || task.exception != null) {
                    Toast.makeText(this,
                        "Upload failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addOnCompleteListener
                }
                ref.downloadUrl
                    .addOnSuccessListener { uri ->
                        saveImageUrlToFirestore(uri.toString())
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this,
                            "Could not fetch image URL: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this,
                    "Storage error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun saveImageUrlToFirestore(imageUrl: String) {
        val user = auth.currentUser
        if (user == null) return handleAuthExpired()

        db.collection("users")
            .document(user.uid)
            .set(mapOf("profilePicture" to imageUrl), SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this,
                    "Profile picture updated",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this,
                    "Failed to save profile picture: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun handleAuthExpired() {
        auth.signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
        Toast.makeText(this,
            "Session expired—please sign in again.",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun setupRecyclerView() {
        workerAdapter = WorkerAdapter(emptyList())
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@Profile)
            adapter = workerAdapter
        }
        attachSwipeToDelete()
    }

    private fun attachSwipeToDelete() {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.adapterPosition
                val snapshot = workerAdapter.getSnapshot(pos)
                snapshot.reference.delete()
                    .addOnSuccessListener {
                        Toast.makeText(this@Profile, "Worker deleted", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this@Profile,
                            "Delete failed: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                        workerAdapter.notifyItemChanged(pos)
                    }
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerView)
    }

    private fun getUserData() {
        val user = auth.currentUser ?: return handleAuthExpired()
        userListener = db.collection("users").document(user.uid)
            .addSnapshotListener { doc, err ->
                if (err != null) {
                    Toast.makeText(this, err.localizedMessage, Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                if (doc != null && doc.exists()) {
                    binding.UserNameProfile.setText(doc.getString("userName"))
                    binding.companyNameProfile.setText(doc.getString("companyName"))
                    doc.getString("profilePicture")?.let { url ->
                        Glide.with(this).load(url).circleCrop()
                            .into(binding.imageViewProfilePicture)
                    }
                }
            }
    }

    private fun listenToWorkers() {
        val user = auth.currentUser ?: return handleAuthExpired()
        workersListener = db.collection("users/${user.uid}/workers")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Toast.makeText(this, err.localizedMessage, Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                workerAdapter.updateSnapshots(snap?.documents ?: emptyList())
            }
    }

    private fun checkStoragePermission() {
        val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(this, permission)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 1)
        } else {
            pickImageLauncher.launch("image/*")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickImageLauncher.launch("image/*")
            } else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show()
            }
            101 -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.profilemenu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.deleteAccount) promptDeleteAccount()
        return super.onOptionsItemSelected(item)
    }

    private fun promptDeleteAccount() {
        val input = EditText(this).apply { hint = "Enter your password" }
        AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure? This cannot be undone.")
            .setView(input)
            .setPositiveButton("Delete") { _, _ ->
                confirmDelete(input.text.toString())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(password: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val user = auth.currentUser ?: return@launch handleAuthExpired()
            try {
                userListener?.remove()
                workersListener?.remove()
                val cred = EmailAuthProvider.getCredential(user.email ?: "", password)
                user.reauthenticate(cred).await()
                val uid = user.uid
                deleteFirestoreCollection(db, "users/$uid")
                db.collection("users").document(uid).delete().await()
                deleteStorageFolder(storage, "users/$uid")
                user.delete().await()
                startActivity(Intent(this@Profile, LoginActivity::class.java))
                finish()
            } catch (e: FirebaseAuthRecentLoginRequiredException) {
                Toast.makeText(this@Profile,
                    "Re-login required.", Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(this@Profile,
                    "Deletion failed: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private suspend fun deleteFirestoreCollection(
        db: FirebaseFirestore, path: String, batchSize: Int = 100
    ) {
        while (true) {
            val snap = db.collection(path).limit(batchSize.toLong()).get().await()
            if (snap.isEmpty) break
            val batch = db.batch().apply { snap.documents.forEach { delete(it.reference) } }
            batch.commit().await()
        }
    }

    private suspend fun deleteStorageFolder(
        storage: FirebaseStorage, folderPath: String
    ) {
        val ref = storage.reference.child(folderPath)
        val list = ref.listAll().await()
        list.items.forEach { it.delete().await() }
        list.prefixes.forEach { deleteStorageFolder(storage, it.path) }
    }
}
