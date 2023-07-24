package com.example.molecule.viewmodel

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

interface MoleculePresenter<Event, Model> {
  val seed: Model

  @Composable
  fun present(events: Flow<Event>): Model
}
