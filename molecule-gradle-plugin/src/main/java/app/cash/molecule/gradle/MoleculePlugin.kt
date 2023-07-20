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
package app.cash.molecule.gradle

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin.API_CONFIGURATION_NAME
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.androidJvm
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.common
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.js
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.jvm
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.native
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.wasm
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.Companion.COMMON_MAIN_SOURCE_SET_NAME
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

private const val moleculeRuntime = "app.cash.molecule:molecule-runtime:$moleculeVersion"

class MoleculePlugin : KotlinCompilerPluginSupportPlugin {
  override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

  override fun getCompilerPluginId(): String = "app.cash.molecule"

  override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
    composeCompilerGroupId,
    composeCompilerArtifactId,
    composeCompilerVersion,
  )

  override fun apply(target: Project) {
    super.apply(target)

    if (target.isInternal() && target.path == ":molecule-runtime") {
      // Being lazy and using our own plugin to configure the Compose compiler on our runtime.
      // Bail out because otherwise we create a circular dependency reference on ourselves!
      return
    }

    target.pluginManager.withPlugin("org.jetbrains.compose") {
      throw IllegalStateException(
        """
        |The Molecule Gradle plugin cannot be applied to the same project as the JetBrains Compose Gradle plugin.
        |
        |Both plugins attempt to configure the Compose compiler plugin which is incompatible. To use Molecule
        |within a JetBrains Compose project you only need to add the runtime dependency:
        |
        |    kotlin {
        |      sourceSets {
        |        commonMain {
        |          dependencies {
        |            implementation("$moleculeRuntime")
        |          }
        |        }
        |      }
        |    }
        """.trimMargin(),
      )
    }

    target.afterEvaluate {
      val android = target.extensions.findByType(KotlinAndroidProjectExtension::class.java)
      val jvm = target.extensions.findByType(KotlinJvmProjectExtension::class.java)
      val multiplatform = target.extensions.findByType(KotlinMultiplatformExtension::class.java)

      val dependency: Any = if (target.isInternal()) {
        target.dependencies.project(mapOf("path" to ":molecule-runtime"))
      } else {
        moleculeRuntime
      }

      if (jvm != null || android != null) {
        target.dependencies.add(API_CONFIGURATION_NAME, dependency)
      } else if (multiplatform != null) {
        multiplatform.sourceSets.getByName(COMMON_MAIN_SOURCE_SET_NAME) { sourceSet ->
          sourceSet.dependencies {
            api(dependency)
          }
        }
      } else {
        throw IllegalStateException("No supported Kotlin plugin detected!")
      }
    }
  }

  private fun Project.isInternal(): Boolean {
    return properties["app.cash.molecule.internal"].toString() == "true"
  }

  override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
    when (kotlinCompilation.platformType) {
      js -> {
        // This enables a workaround for Compose lambda generation to function correctly in JS.
        // Note: We cannot use SubpluginOption to do this because it targets the Compose plugin.
        kotlinCompilation.kotlinOptions.freeCompilerArgs +=
          listOf("-P", "plugin:androidx.compose.compiler.plugins.kotlin:generateDecoys=true")
      }
      common, androidJvm, jvm, native, wasm -> {
        // Nothing to do!
      }
    }

    return kotlinCompilation.target.project.provider { emptyList() }
  }
}
