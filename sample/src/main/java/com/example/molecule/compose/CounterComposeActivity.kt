package com.example.molecule.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.cash.molecule.AndroidUiDispatcher.Companion.Main
import app.cash.molecule.launchMolecule
import com.example.molecule.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow

class CounterComposeActivity : ComponentActivity() {
    private val scope = CoroutineScope(Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val randomService = RandomService()
        val events = MutableSharedFlow<CounterEvent>(extraBufferCapacity = 1)
        val models = scope.launchMolecule { CounterPresenter(events, randomService) }
        setContent {
            val counterModel by models.collectAsState()
            Counter(
                counter = counterModel,
                onDecreaseTenClick = {
                    events.tryEmit(Change(-10))
                },
                onDecreaseOneClick = {
                    events.tryEmit(Change(-1))
                },
                onIncreaseTenClick = {
                    events.tryEmit(Change(10))
                },
                onIncreaseOneClick = {
                    events.tryEmit(Change(1))
                },
                onRandomizeClick = {
                    events.tryEmit(Randomize)
                }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}