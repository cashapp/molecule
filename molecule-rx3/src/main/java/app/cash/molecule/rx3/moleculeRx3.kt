/*
 * Copyright (C) 2021 Square, Inc.
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
package app.cash.molecule.rx3

import androidx.compose.runtime.Composable
import app.cash.molecule.Action
import app.cash.molecule.moleculeFlow
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.rx3.asFlowable
import kotlinx.coroutines.rx3.asObservable
import kotlin.coroutines.CoroutineContext

fun <T : Any> moleculeObservable(
  coroutineContext: CoroutineContext,
  body: @Composable () -> Action<T>,
): Observable<T> {
  return moleculeFlow(body).asObservable(coroutineContext + Unconfined)
}

fun <T : Any> moleculeFlowable(
  coroutineContext: CoroutineContext,
  body: @Composable () -> Action<T>,
): Flowable<T> {
  return moleculeFlow(body).asFlowable(coroutineContext + Unconfined)
}
