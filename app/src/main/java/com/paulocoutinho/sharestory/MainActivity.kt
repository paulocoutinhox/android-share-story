package com.paulocoutinho.sharestory

import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.paulocoutinho.sharestory.ui.theme.ShareStoryTheme
import com.pranavpandey.android.dynamic.toasts.DynamicToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : ComponentActivity(), CoroutineScope {
    override val coroutineContext = Dispatchers.Main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScreenContent()
        }
    }

    fun doShareVideo() {
        // show loading
        showWarning("Loading, wait...")

        // save asset image
        launch(Dispatchers.IO) {
            if (saveImageAsset()) {
                if (saveBackgroundVideoAsset()) {
                    withContext(Dispatchers.Main) {
                        // show posting
                        showWarning("Done, now i'm posting...")

                        // create intent
                        val providerAssetUri = getProviderFileUri(getImageAssetFile())
                        val providerBackgroundAssetUri = getProviderFileUri(
                            getBackgroundVideoAssetFile()
                        )

                        val shareIntent = Intent("com.facebook.stories.ADD_TO_STORY").apply {
                            setDataAndType(providerBackgroundAssetUri, "video/*")
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            putExtra("source_application", packageName)
                            putExtra(
                                "com.facebook.platform.extra.APPLICATION_ID",
                                "293441040780773"
                            )
                            putExtra("interactive_asset_uri", providerAssetUri)
                        }

                        // grant permission to downloaded files
                        grantPermission(providerAssetUri)
                        grantPermission(providerBackgroundAssetUri)

                        // open intent
                        openIntent(shareIntent)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showError("Error: Background video asset cannot be download")
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    showError("Error: Image asset cannot be download")
                }
            }
        }
    }

    fun doShareImage() {
        // show loading
        showWarning("Loading, wait...")

        // save asset image
        launch(Dispatchers.IO) {
            if (saveImageAsset()) {
                if (saveBackgroundImageAsset()) {
                    withContext(Dispatchers.Main) {
                        // show posting
                        showWarning("Done, now i'm posting...")

                        // create intent
                        val providerAssetUri = getProviderFileUri(getImageAssetFile())
                        val providerBackgroundAssetUri = getProviderFileUri(
                            getBackgroundImageAssetFile()
                        )

                        val shareIntent = Intent("com.facebook.stories.ADD_TO_STORY").apply {
                            setDataAndType(providerBackgroundAssetUri, "image/*")
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            putExtra("source_application", packageName)
                            putExtra(
                                "com.facebook.platform.extra.APPLICATION_ID",
                                "293441040780773"
                            )
                            putExtra("interactive_asset_uri", providerAssetUri)
                        }

                        // grant permission to downloaded files
                        grantPermission(providerAssetUri)
                        grantPermission(providerBackgroundAssetUri)

                        // open intent
                        openIntent(shareIntent)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showError("Error: Background image asset cannot be download")
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    showError("Error: Image asset cannot be download")
                }
            }
        }
    }

    fun doShareColor() {
        // show loading
        showWarning("Loading, wait...")

        // save asset image
        launch(Dispatchers.IO) {
            if (saveImageAsset()) {
                withContext(Dispatchers.Main) {
                    // show posting
                    showWarning("Done, now i'm posting...")

                    // create intent
                    val providerAssetUri = getProviderFileUri(getImageAssetFile())

                    val shareIntent = Intent("com.facebook.stories.ADD_TO_STORY").apply {
                        type = "image/*"
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        putExtra("source_application", packageName)
                        putExtra(
                            "com.facebook.platform.extra.APPLICATION_ID",
                            "293441040780773"
                        )
                        putExtra("interactive_asset_uri", providerAssetUri)
                        putExtra("top_background_color", "#FF0000")
                        putExtra("bottom_background_color", "#0000FF")
                    }

                    // grant permission to downloaded file
                    grantPermission(providerAssetUri)

                    // open intent
                    openIntent(shareIntent)
                }
            } else {
                withContext(Dispatchers.Main) {
                    showError("Error: Image asset cannot be download")
                }
            }
        }
    }

    private fun getProviderFileUri(file: File): Uri? {
        return FileProvider.getUriForFile(
            this,
            "com.paulocoutinho.fileprovider",
            file
        )
    }

    private fun grantPermission(uri: Uri?) {
        uri?.let {
            grantUriPermission(
                "com.facebook.katana", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    private fun openIntent(shareIntent: Intent) {
        if (packageManager?.resolveActivity(shareIntent, 0) != null) {
            startActivityForResult(shareIntent, 0)
            showSuccess("Done!")
        } else {
            showError("Cannot start activity with the required intent!")
        }
    }

    private fun saveImageAsset(): Boolean {
        val request = Request.Builder().url("https://picsum.photos/100/150").build()
        val client = OkHttpClient.Builder().build()
        val response = client.newCall(request).execute()

        if (response.isSuccessful) {
            val folder = filesDir

            if (!folder.exists()) {
                folder.mkdir()
            }

            val file = getImageAssetFile()

            if (file.exists()) {
                file.delete()
            }

            val body = response.body

            body?.let {
                val inputStream = body.byteStream()
                val fos = FileOutputStream(file)
                val buffer = ByteArray(4096)
                var len: Int

                while (inputStream.read(buffer).also { len = it } != -1) {
                    fos.write(buffer, 0, len)
                }

                fos.flush()
                fos.close()

                return true
            }

            return false
        }

        return false
    }

    private fun saveBackgroundImageAsset(): Boolean {
        val request = Request.Builder().url("https://picsum.photos/720/1280").build()
        val client = OkHttpClient.Builder().build()
        val response = client.newCall(request).execute()

        if (response.isSuccessful) {
            val folder = filesDir

            if (!folder.exists()) {
                folder.mkdir()
            }

            val file = getBackgroundImageAssetFile()

            if (file.exists()) {
                file.delete()
            }

            val body = response.body

            body?.let {
                val inputStream = body.byteStream()
                val fos = FileOutputStream(file)
                val buffer = ByteArray(4096)
                var len: Int

                while (inputStream.read(buffer).also { len = it } != -1) {
                    fos.write(buffer, 0, len)
                }

                fos.flush()
                fos.close()

                return true
            }

            return false
        }

        return false
    }

    private fun saveBackgroundVideoAsset(): Boolean {
        try {
            copyAssetFile(this, "video.mp4", getBackgroundVideoAssetFile().absolutePath)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }

    private fun copyAssetFile(context: Context, sourceFilename: String, targetFilename: String) {
        val assetManager: AssetManager = context.assets

        try {
            val inStream = assetManager.open(sourceFilename)
            val outStream = FileOutputStream(targetFilename)

            try {
                val buffer = ByteArray(1024)
                var read: Int

                while (inStream.read(buffer).also { read = it } != -1) {
                    outStream.write(buffer, 0, read)
                }
            } catch (e: IOException) {
                e.printStackTrace()
                // failed to copy asset file: $sourceFilename to: $targetFilename
            } finally {
                try {
                    inStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                try {
                    outStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // failed to copy asset file: {$e.message}
        }
    }

    private fun getImageAssetFile(): File {
        val folder = filesDir
        return File(folder.path.toString() + "/asset-image.jpg")
    }

    private fun getBackgroundImageAssetFile(): File {
        val folder = filesDir
        return File(folder.path.toString() + "/bg-asset-image.jpg")
    }

    private fun getBackgroundVideoAssetFile(): File {
        val folder = filesDir
        return File(folder.path.toString() + "/bg-asset-video.mp4")
    }

    private fun showSuccess(msg: String) {
        DynamicToast.makeSuccess(this, msg).show()
    }

    private fun showWarning(msg: String) {
        DynamicToast.makeWarning(this, msg).show()
    }

    private fun showError(msg: String) {
        DynamicToast.makeError(this, msg).show()
    }

}

@Composable
private fun ScreenContent() {
    val context = LocalContext.current

    ShareStoryTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Share Story", color = Color.White, fontSize = 30.sp)
                }
                Spacer(Modifier.height(30.dp))
                Row {
                    Image(
                        modifier = Modifier.size(64.dp),
                        imageVector = ImageVector.vectorResource(R.drawable.ic_facebook),
                        contentDescription = "Facebook",
                    )
                    Spacer(Modifier.width(20.dp))
                    Image(
                        modifier = Modifier.size(64.dp),
                        imageVector = ImageVector.vectorResource(R.drawable.ic_instagram),
                        contentDescription = "Instagram"
                    )
                }
                Spacer(Modifier.height(30.dp))
                ProductCoverImage()
                Spacer(Modifier.height(20.dp))
                Button(onClick = {
                    (context as MainActivity).doShareColor()
                }) {
                    Text(text = "Share Color")
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    (context as MainActivity).doShareImage()
                }) {
                    Text(text = "Share Image")
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    (context as MainActivity).doShareVideo()
                }) {
                    Text(text = "Share Video")
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun ProductCoverImage() {
    val size = 100
    val url = "https://picsum.photos/100/150"

    AsyncImage(
        url,
        contentDescription = "Cover Image",
        contentScale = ContentScale.FillBounds,
        modifier = Modifier
            .width(size.dp)
            .height((size * 1.5).dp)
            .clip(RoundedCornerShape(2.dp))
    )
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ShareStoryTheme {
        ScreenContent()
    }
}