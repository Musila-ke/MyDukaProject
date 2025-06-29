package com.example.myduka

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myduka.databinding.ActivityHelpBinding

class Help : AppCompatActivity() {
    lateinit var helpBinding: ActivityHelpBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        helpBinding = ActivityHelpBinding.inflate(layoutInflater)
        val view = helpBinding.root
        setContentView(view)

        helpBinding.buttonSendMessage.setOnClickListener {
            sendEmail() // Call the sendEmail function when the button is clicked
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    fun sendEmail(){
        val subject = helpBinding.editTextSubject.text.toString()
        val message = helpBinding.editTextMessage.text.toString()

        val recipientEmail = arrayOf("kelvinmusilamaingi@gmail.com") // Your email address
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, recipientEmail)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, message)
        }

        if (emailIntent.resolveActivity(packageManager) != null) {
            startActivity(Intent.createChooser(emailIntent, "Choose an email client"))
        }
    }
}