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
package com.example.molecule

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
import retrofit2.Retrofit.Builder
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Query

interface RandomService {
  suspend fun get(min: Int, max: Int): Int
}

private interface RandomApi {
  @GET("integers/?num=1&col=1&base=10&format=plain")
  suspend fun get(
    @Query("min") min: Int,
    @Query("max") max: Int,
  ): String
}

fun RandomService(): RandomService {
  val retrofit = Builder()
    .baseUrl("https://www.random.org/")
    .client(
      OkHttpClient.Builder()
        .addInterceptor(
          HttpLoggingInterceptor { Log.d("HTTP", it) }
            .also { it.level = BASIC },
        )
        .build(),
    )
    .addConverterFactory(ScalarsConverterFactory.create())
    .build()
  val api = retrofit.create<RandomApi>()
  return object : RandomService {
    override suspend fun get(min: Int, max: Int): Int {
      return api.get(min, max).trim().toInt()
    }
  }
}
