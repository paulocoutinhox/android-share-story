package com.paulocoutinho.sharestory

import android.content.Intent
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
import coil.compose.AsyncImage
import com.paulocoutinho.sharestory.ui.theme.ShareStoryTheme
import com.pranavpandey.android.dynamic.toasts.DynamicToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okio.BufferedSink
import okio.buffer
import okio.sink
import java.io.File

class MainActivity : ComponentActivity(), CoroutineScope {
    override val coroutineContext = Dispatchers.Main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScreenContent()
        }
    }

    fun doShareVideo() {
        TODO("Not yet implemented")
    }

    fun doShareImage() {
        TODO("Not yet implemented")
    }

    fun doShareColor() {
        // show loading
        DynamicToast.makeWarning(this, "Loading, wait...").show();

        // do request
        val request: Request = Request.Builder().url("https://picsum.photos/200/300").build()
        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: okio.IOException) {
                // show error on download
                DynamicToast.makeError(this@MainActivity, e.toString()).show()
            }

            override fun onResponse(call: Call, response: Response) {
                launch(Dispatchers.IO) {
                    // save image locally to send by intent
                    val folder = filesDir

                    if (!folder.exists()) {
                        folder.mkdir()
                    }

                    val file = File(folder.path.toString() + "/image.jpg")

                    if (file.exists()) {
                        file.delete()
                    }

                    file.createNewFile()

                    val sink: BufferedSink = file.sink().buffer()

                    response.body?.let {
                        sink.writeAll(it.source())
                    }

                    sink.close()

                    withContext(Dispatchers.Main) {
                        // show posting
                        DynamicToast
                            .makeWarning(this@MainActivity, "Done, now i'm posting...")
                            .show()

                        // create intent
                        val shareIntent = Intent("com.facebook.stories.ADD_TO_STORY").apply {
                            type = "image/jpeg"
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            putExtra("source_application", packageName)
                            putExtra(
                                "com.facebook.platform.extra.APPLICATION_ID",
                                "293441040780773"
                            )
                            putExtra("interactive_asset_uri", Uri.parse(file.absolutePath))
                            putExtra("top_background_color", "#FF0000")
                            putExtra("bottom_background_color", "#0000FF")
                        }

                        // grant permission to downloaded file
                        grantUriPermission(
                            "com.facebook.katana",
                            Uri.parse(file.absolutePath),
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )

                        // open intent
                        if (packageManager?.resolveActivity(shareIntent, 0) != null) {
                            startActivityForResult(shareIntent, 0)

                            // show success
                            DynamicToast.makeSuccess(this@MainActivity, "Done!").show()
                        } else {
                            DynamicToast.makeError(
                                this@MainActivity,
                                "Cannot start activity with the required intent!"
                            ).show()
                        }
                    }
                }
            }
        })
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
    val url = "https://picsum.photos/200/300"

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