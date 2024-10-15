/*
 * Copyright (C) 2022 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.molecule.viewmodel

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    WindowCompat.setDecorFitsSystemWindows(window, false)
    val viewModel by viewModels<PupperPicsViewModel>()
    setContent {
      RootContainer {
        val model by viewModel.models.collectAsState()
        PupperPicsScreen(model, onEvent = { event -> viewModel.take(event) })
      }
    }
  }
}

@Composable
private fun RootContainer(content: @Composable () -> Unit) {
  val view = LocalView.current
  val window = (view.context as Activity).window
  val inLightMode = !isSystemInDarkTheme()

  SideEffect {
    window.statusBarColor = Color.TRANSPARENT
    window.navigationBarColor = Color.TRANSPARENT
    val insetsController = WindowCompat.getInsetsController(window, view)
    insetsController.isAppearanceLightStatusBars = inLightMode
    insetsController.isAppearanceLightNavigationBars = inLightMode
  }

  MaterialTheme(colorScheme = if (inLightMode) lightColorScheme() else darkColorScheme()) {
    content()
  }
}
