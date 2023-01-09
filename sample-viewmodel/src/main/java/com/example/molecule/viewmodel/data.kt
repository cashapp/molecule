/*
 * Copyright (C) 2023 Square, Inc.
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

import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Path

interface PupperPicsService {
  suspend fun listBreeds(): List<String>
  suspend fun randomImageUrlFor(breed: String): String
}

fun PupperPicsService(): PupperPicsService {
  val api = Retrofit.Builder()
    .baseUrl("https://dog.ceo/api/")
    .addConverterFactory(MoshiConverterFactory.create())
    .build()
    .create<PupperPicsApi>()

  return object : PupperPicsService {
    override suspend fun listBreeds(): List<String> {
      return api.listBreeds().message.flatMap { (breed, subBreeds) ->
        if (subBreeds.isEmpty()) {
          listOf(breed)
        } else {
          subBreeds.map { subBreed -> "$breed/$subBreed" }
        }
      }
    }

    override suspend fun randomImageUrlFor(breed: String): String {
      return api.randomImageFor(breed).message
    }
  }
}

private interface PupperPicsApi {
  @GET("breeds/list/all")
  suspend fun listBreeds(): ListResponse

  @GET("breed/{breed}/images/random")
  suspend fun randomImageFor(@Path("breed", encoded = true) breed: String): ImageResponse

  data class ListResponse(val message: Map<String, List<String>>)
  data class ImageResponse(val message: String)
}
