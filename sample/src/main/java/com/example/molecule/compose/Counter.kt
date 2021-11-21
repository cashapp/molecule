package com.example.molecule.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.molecule.CounterModel

@Composable
fun Counter(
    counter: CounterModel,
    onDecreaseTenClick: () -> Unit,
    onDecreaseOneClick: () -> Unit,
    onIncreaseTenClick: () -> Unit,
    onIncreaseOneClick: () -> Unit,
    onRandomizeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onDecreaseTenClick,
                enabled = !counter.loading
            ) {
                Text(text = "-10")
            }
            Button(
                onClick = onDecreaseOneClick,
                enabled = !counter.loading
            ) {
                Text(text = "-1")
            }
            Text(text = counter.value.toString(), fontSize = 20.sp)
            Button(
                onClick = onIncreaseOneClick,
                enabled = !counter.loading
            ) {
                Text(text = "+1")
            }
            Button(
                onClick = onIncreaseTenClick,
                enabled = !counter.loading
            ) {
                Text(text = "+10")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onRandomizeClick,
            enabled = !counter.loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Randomize")
        }
    }
}