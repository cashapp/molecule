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

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter

@Composable
fun PupperPicsScreen(model: Model, onEvent: (Event) -> Unit) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Add a space above the content that matches TopBarMinHeight.
      // This allows the top bar to expand _over_ the content without moving it.
      Spacer(modifier = Modifier.height(TopBarMinHeight).statusBarsPadding())
      Content(model, modifier = Modifier.weight(1f))
      AnimatedVisibility(visible = !model.loading) { BottomBar(onEvent) }
    }

    TopBar(model, onEvent)
  }
}

private val TopBarMinHeight = 152.dp

@Composable
private fun TopBar(model: Model, onEvent: (Event) -> Unit) {
  var dropdownExpanded by remember { mutableStateOf(false) }
  // Close dropdown on system back button.
  BackHandler(enabled = dropdownExpanded) { dropdownExpanded = false }

  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(TopBarMinHeight)
        .animateContentSize()
        .statusBarsPadding()
        .padding(top = 16.dp),
    ) {
      Text(
        text = "Pupper Pics",
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.padding(horizontal = 16.dp),
      )
      Spacer(modifier = Modifier.size(8.dp))
      CurrentBreedSelection(
        text = model.dropdownText,
        enabled = !model.loading,
        expanded = dropdownExpanded,
        onClick = { dropdownExpanded = !dropdownExpanded },
      )
      if (dropdownExpanded) {
        BreedSelectionList(model) { breed ->
          dropdownExpanded = false
          onEvent(Event.SelectBreed(breed))
        }
      }
    }
  }
}

@Composable
private fun CurrentBreedSelection(
  text: String,
  enabled: Boolean,
  expanded: Boolean,
  onClick: () -> Unit,
) {
  val arrowRotation by animateFloatAsState(if (expanded) 180f else 0f)

  OutlinedCard(
    colors = CardDefaults.outlinedCardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant,
    ),
    modifier = Modifier.padding(horizontal = 16.dp),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth()
        .heightIn(48.dp)
        .clickable(enabled = enabled, onClick = onClick)
        .padding(horizontal = 16.dp),
    ) {
      Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
      )
      Icon(
        imageVector = Icons.Rounded.ArrowDropDown,
        contentDescription = null,
        modifier = Modifier.rotate(arrowRotation),
      )
    }
  }
}

@Composable
private fun BreedSelectionList(model: Model, onBreedClick: (String) -> Unit) {
  LazyColumn(contentPadding = WindowInsets.navigationBars.asPaddingValues()) {
    items(model.breeds) { breed ->
      Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(56.dp)
          .clickable { onBreedClick(breed) }
          .padding(horizontal = 32.dp),
      ) {
        Text(
          text = breed,
          style = MaterialTheme.typography.titleMedium,
        )
      }
    }
  }
}

@Composable
private fun Content(model: Model, modifier: Modifier) {
  var imageLoading by remember { mutableStateOf(true) }

  Box(modifier) {
    AsyncImage(
      model.currentUrl,
      contentDescription = "A good dog",
      // Ignore errors in this sample.
      onState = { imageLoading = it !is AsyncImagePainter.State.Success },
      modifier = Modifier.fillMaxSize(),
    )
    if (model.loading || imageLoading) {
      Loading(Modifier.fillMaxSize().background(color = MaterialTheme.colorScheme.background))
    }
  }
}

@Composable
private fun Loading(modifier: Modifier) {
  val rotation by rememberInfiniteTransition().animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(tween(durationMillis = 1_000)),
  )

  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
    modifier = modifier.fillMaxSize(),
  ) {
    Text(
      text = "🐶",
      style = MaterialTheme.typography.headlineLarge,
      modifier = Modifier.rotate(rotation),
    )
    Spacer(modifier = Modifier.size(8.dp))
    Text(
      text = "Fetching…",
      style = MaterialTheme.typography.headlineSmall,
    )
  }
}

@Composable
private fun BottomBar(onEvent: (Event) -> Unit) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Button(
      content = {
        Text("Fetch again!")
      },
      onClick = { onEvent(Event.FetchAgain) },
      modifier = Modifier
        .navigationBarsPadding()
        .padding(16.dp)
        .fillMaxWidth()
        .heightIn(48.dp),
    )
  }
}
