package com.example.myduka

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import com.yalantis.ucrop.UCrop
import java.io.File

class CropActivityContract : ActivityResultContract<Uri, Uri?>() {

    override fun createIntent(context: Context, input: Uri): Intent {
        val outputFile = File(context.cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
        val outputUri = Uri.fromFile(outputFile)

        return UCrop.of(input, outputUri)
            .withAspectRatio(1f, 1f)
            .getIntent(context)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (resultCode == Activity.RESULT_OK && intent != null) {
            UCrop.getOutput(intent)
        } else null
    }
}
