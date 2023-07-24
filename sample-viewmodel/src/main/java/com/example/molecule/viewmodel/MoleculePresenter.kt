package com.example.molecule.viewmodel

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

interface MoleculePresenter<Event, Model> {
  @Composable
  fun present(seed: Model, events: Flow<Event>): Model
}
