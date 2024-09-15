/*
 * Copyright 2024 The Google AI Edge Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.aiedge.examples.image_segmentation

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.aiedge.examples.image_segmentation.view.CameraScreen
import com.google.aiedge.examples.image_segmentation.view.GalleryScreen
import com.google.aiedge.examples.image_segmentation.view.darkBlue
import com.google.aiedge.examples.image_segmentation.view.teal

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel: MainViewModel by viewModels { MainViewModel.getFactory(this) }
        setContent {
            var tabState by remember { mutableStateOf(Tab.Camera) }

            // Register ActivityResult handler
            val galleryLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                    if (uri != null) {
                        viewModel.updateMediaUri(uri)
                    }
                }

            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(uiState.errorMessage) {
                if (uiState.errorMessage != null) {
                    Toast.makeText(
                        this@MainActivity, "${uiState.errorMessage}", Toast.LENGTH_SHORT
                    ).show()
                    viewModel.errorMessageShown()
                }
            }

            BottomSheetScaffold(
                sheetShape = RoundedCornerShape(topStart = 15.dp, topEnd = 15.dp),
                sheetPeekHeight = 70.dp,
                sheetContent = {
                    BottomSheet(inferenceTime = uiState.inferenceTime, onDelegateSelected = {
                        viewModel.setDelegate(it)
                    })
                },
                floatingActionButton = {
                    if (tabState == Tab.Gallery) {
                        FloatingActionButton(
                            backgroundColor = teal,
                            shape = CircleShape, onClick = {
                                val request = PickVisualMediaRequest()
                                galleryLauncher.launch(request)
                            }) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                        }
                    }
                }) {
                Column {
                    Header()
                    Content(
                        uiState = uiState,
                        tab = tabState,
                        onTabChanged = {
                            tabState = it
                            viewModel.stopSegment()
                        },
                        onImageProxyAnalyzed = { imageProxy ->
                            viewModel.segment(imageProxy)
                        },
                        onImageBitMapAnalyzed = { bitmap, degrees ->
                            viewModel.segment(bitmap, degrees)
                        })
                }
            }
        }
    }

    @Composable
    fun Content(
        uiState: UiState,
        tab: Tab,
        modifier: Modifier = Modifier,
        onTabChanged: (Tab) -> Unit,
        onImageProxyAnalyzed: (ImageProxy) -> Unit,
        onImageBitMapAnalyzed: (Bitmap, Int) -> Unit,
    ) {
        val tabs = Tab.entries
        Column(modifier) {
            TabRow(backgroundColor = darkBlue, selectedTabIndex = tab.ordinal) {
                tabs.forEach { t ->
                    Tab(
                        text = { Text(t.name, color = Color.White) },
                        selected = tab == t,
                        onClick = { onTabChanged(t) },
                    )
                }
            }

            when (tab) {
                Tab.Camera -> CameraScreen(
                    uiState = uiState,
                    onImageAnalyzed = {
                        onImageProxyAnalyzed(it)
                    },
                )

                Tab.Gallery -> GalleryScreen(
                    modifier = Modifier.fillMaxSize(),
                    uiState = uiState,
                    onImageAnalyzed = {
                        onImageBitMapAnalyzed(it, 0)
                    },
                )
            }
        }
    }

    @Composable
    fun Header() {
        TopAppBar(
            backgroundColor = teal,
            title = {
                Image(
                    modifier = Modifier.size(120.dp),
                    alignment = Alignment.CenterStart,
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = null,
                )
            },
        )
    }

    @Composable
    fun BottomSheet(
        inferenceTime: Long,
        modifier: Modifier = Modifier,
        onDelegateSelected: (ImageSegmentationHelper.Delegate) -> Unit
    ) {
        Column(modifier = modifier.padding(horizontal = 20.dp, vertical = 5.dp)) {
            Image(
                modifier = Modifier
                    .size(40.dp)
                    .padding(top = 2.dp, bottom = 5.dp)
                    .align(Alignment.CenterHorizontally),
                painter = painterResource(id = R.drawable.ic_chevron_up),
                contentDescription = ""
            )
            Row {
                Text(modifier = Modifier.weight(0.5f), text = "Inference Time")
                Text(text = inferenceTime.toString())
            }
            Spacer(modifier = Modifier.height(20.dp))
            OptionMenu(label = "Delegate",
                options = ImageSegmentationHelper.Delegate.entries.map { it.name }) {
                onDelegateSelected(ImageSegmentationHelper.Delegate.valueOf(it))
            }
        }
    }

    @Composable
    fun OptionMenu(
        label: String,
        modifier: Modifier = Modifier,
        options: List<String>,
        onOptionSelected: (option: String) -> Unit,
    ) {
        var expanded by remember { mutableStateOf(false) }
        var option by remember { mutableStateOf(options.first()) }
        Row(
            modifier = modifier, verticalAlignment = Alignment.CenterVertically
        ) {
            Text(modifier = Modifier.weight(0.5f), text = label, fontSize = 15.sp)
            Box {
                Row(
                    modifier = Modifier.clickable {
                        expanded = true
                    }, verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = option, fontSize = 15.sp)
                    Spacer(modifier = Modifier.width(5.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Localized description"
                    )
                }

                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach {
                        DropdownMenuItem(
                            content = {
                                Text(it, fontSize = 15.sp)
                            },
                            onClick = {
                                option = it
                                onOptionSelected(option)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
    }

    enum class Tab {
        Camera, Gallery,
    }
}
