package com.example.jiguyunyu.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.jiguyunyu.ui.theme.*
import com.example.jiguyunyu.viewmodel.ArchaeologyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchaeologyUploadScreen(navController: NavController, viewModel: ArchaeologyViewModel = viewModel()) {
    var name by remember { mutableStateOf("") }
    var era by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var loc by remember { mutableStateOf("") }
    var tagsString by remember { mutableStateOf("") }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                bitmap = if (Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true // 确保 bitmap 可操作
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("文物信息录入 (专家)", color = IndigoInk, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif) }, 
                navigationIcon = { 
                    IconButton(onClick={navController.popBackStack()}) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = IndigoInk) 
                    } 
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = IvoryWhite)
            ) 
        },
        containerColor = IvoryWhite
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (viewModel.errorMsg.isNotEmpty()) {
                Text(
                    text = viewModel.errorMsg,
                    color = CinnabarRed,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            OutlinedTextField(
                value = name, 
                onValueChange = {name=it}, 
                label = { Text("文物名称") }, 
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = IndigoInk, focusedLabelColor = IndigoInk)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = era, 
                onValueChange = {era=it}, 
                label = { Text("所属年代 (如: 唐代)") }, 
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = IndigoInk, focusedLabelColor = IndigoInk)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = loc, 
                onValueChange = {loc=it}, 
                label = { Text("出土地点") }, 
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = IndigoInk, focusedLabelColor = IndigoInk)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = tagsString, 
                onValueChange = {tagsString=it}, 
                label = { Text("标签 (空格分隔)") }, 
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = IndigoInk, focusedLabelColor = IndigoInk)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = desc, 
                onValueChange = {desc=it}, 
                label = { Text("详细描述") }, 
                modifier = Modifier.fillMaxWidth(), 
                minLines = 4,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = IndigoInk, focusedLabelColor = IndigoInk)
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            Text("文物照片", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Bronze, modifier = Modifier.padding(bottom = 8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .border(1.dp, Parchment, RoundedCornerShape(12.dp))
                    .clickable { launcher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(bitmap!!.asImageBitmap(), null, modifier = Modifier.fillMaxSize())
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddPhotoAlternate, null, tint=Bronze, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("点击上传文物照片", color=Bronze)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { 
                    val tagsList = tagsString.split(" ").filter { it.isNotBlank() }
                    viewModel.submitArtifact(
                        name = name,
                        desc = desc,
                        bitmap = bitmap,
                        location = loc,
                        era = era,
                        tags = tagsList
                    ) { 
                        navController.popBackStack() 
                    } 
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoInk),
                enabled = !viewModel.isSubmitting && bitmap != null && name.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                if(viewModel.isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("上传中...")
                } else {
                    Text("提交考古数据库", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
