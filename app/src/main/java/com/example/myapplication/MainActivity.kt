package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.myapplication.model.Photo
import com.example.myapplication.scanner.GalleryScanner

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                DuplicateCleanerScreen()
            }
        }
    }
}

@Composable
fun DuplicateCleanerScreen() {

    val context = LocalContext.current

    var status by remember {
        mutableStateOf("Ready to scan gallery")
    }

    var duplicateGroups by remember {
        mutableStateOf<Map<String, List<Photo>>>(emptyMap())
    }

    var savedStorage by remember {
        mutableLongStateOf(0L)
    }

    var isScanning by remember {
        mutableStateOf(false)
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {

                isScanning = true
                status = "Scanning gallery..."

                val photos = GalleryScanner.getAllPhotos(context)

                duplicateGroups =
                    photos.groupBy { it.hash }
                        .filter { it.value.size > 1 }

                status =
                    "Found ${duplicateGroups.size} duplicate groups"

                isScanning = false

            } else {

                Toast.makeText(
                    context,
                    "Permission denied",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF081B29),
                        Color(0xFF000814),
                        Color(0xFF12002F)
                    )
                )
            )
    ) {

        Box(
            modifier = Modifier
                .size(220.dp)
                .offset(x = (-60).dp, y = (-40).dp)
                .blur(80.dp)
                .background(
                    Color.Cyan.copy(alpha = 0.25f),
                    CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 80.dp)
                .blur(80.dp)
                .background(
                    Color.Magenta.copy(alpha = 0.25f),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 70.dp),

            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Smart Duplicate Cleaner",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(25.dp))

            Button(
                onClick = {

                    val permission = if (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    ) {
                        Manifest.permission.READ_MEDIA_IMAGES
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }

                    if (
                        ContextCompat.checkSelfPermission(
                            context,
                            permission
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {

                        isScanning = true
                        status = "Scanning gallery..."

                        val photos =
                            GalleryScanner.getAllPhotos(context)

                        duplicateGroups =
                            photos.groupBy { it.hash }
                                .filter { it.value.size > 1 }

                        status =
                            "Found ${duplicateGroups.size} duplicate groups"

                        isScanning = false

                    } else {

                        permissionLauncher.launch(permission)
                    }
                },

                colors = ButtonDefaults.buttonColors(
                    containerColor =
                        Color.White.copy(alpha = 0.12f)
                ),

                shape = RoundedCornerShape(20.dp),

                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Color.White.copy(alpha = 0.08f)
                    )
            ) {

                Text(
                    text = "Scan Gallery",
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = status,
                color = Color.White.copy(alpha = 0.8f)
            )

            if (savedStorage > 0) {

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text =
                        "Saved ${(savedStorage / 1024 / 1024)} MB",
                    color = Color.Green,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isScanning) {

                Spacer(modifier = Modifier.height(20.dp))

                CircularProgressIndicator(
                    color = Color(0xFFB388FF)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            LazyColumn {

                duplicateGroups.forEach { group ->

                    val groupPhotos = group.value

                    val bestPhoto =
                        groupPhotos.maxByOrNull {
                            it.width * it.height
                        }

                    item {

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor =
                                    Color.White.copy(alpha = 0.08f)
                            ),

                            shape = RoundedCornerShape(25.dp),

                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                        ) {

                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {

                                Text(
                                    text =
                                        "${groupPhotos.size} duplicate photos",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )

                                Spacer(
                                    modifier = Modifier.height(15.dp)
                                )

                                groupPhotos.forEach { photo ->

                                    Row(
                                        verticalAlignment =
                                            Alignment.CenterVertically,

                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                    ) {

                                        AsyncImage(
                                            model = photo.uri,
                                            contentDescription = null,

                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(
                                                    RoundedCornerShape(16.dp)
                                                ),

                                            contentScale =
                                                ContentScale.Crop
                                        )

                                        Spacer(
                                            modifier =
                                                Modifier.width(12.dp)
                                        )

                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {

                                            Text(
                                                text = photo.name,
                                                color = Color.White,
                                                maxLines = 1
                                            )

                                            Text(
                                                text =
                                                    "${photo.width} x ${photo.height}",
                                                color =
                                                    Color.LightGray,
                                                fontSize = 12.sp
                                            )

                                            Text(
                                                text =
                                                    "${photo.size / 1024} KB",
                                                color =
                                                    Color.LightGray,
                                                fontSize = 12.sp
                                            )

                                            if (photo == bestPhoto) {

                                                Text(
                                                    text = "Best Quality",
                                                    color = Color.Green,
                                                    fontWeight =
                                                        FontWeight.Bold
                                                )
                                            }
                                        }

                                        if (photo != bestPhoto) {

                                            Button(

                                                onClick = {

                                                    context.contentResolver.delete(
                                                        photo.uri,
                                                        null,
                                                        null
                                                    )

                                                    savedStorage += photo.size

                                                    status =
                                                        "Deleted ${photo.name}"

                                                },

                                                colors =
                                                    ButtonDefaults.buttonColors(
                                                        containerColor =
                                                            Color.Red.copy(
                                                                alpha = 0.85f
                                                            )
                                                    ),

                                                shape =
                                                    RoundedCornerShape(16.dp)
                                            ) {

                                                Text("Delete")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}