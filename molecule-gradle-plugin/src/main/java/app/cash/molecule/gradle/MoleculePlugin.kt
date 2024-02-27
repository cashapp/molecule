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

import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPlugin.API_CONFIGURATION_NAME
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.Companion.COMMON_MAIN_SOURCE_SET_NAME
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

private const val EXTENSION_NAME = "molecule"
private const val MOLECULE_RUNTIME = "app.cash.molecule:molecule-runtime:$moleculeVersion"

private abstract class MoleculeExtensionImpl
@Inject constructor(objectFactory: ObjectFactory) : MoleculeExtension {
  override val kotlinCompilerPlugin: Property<String> =
    objectFactory.property(String::class.java)
      .convention(composeCompilerVersion)
}

class MoleculePlugin : KotlinCompilerPluginSupportPlugin {
  private lateinit var extension: MoleculeExtension

  override fun apply(target: Project) {
    super.apply(target)

    extension = target.extensions.create(
      MoleculeExtension::class.java,
      EXTENSION_NAME,
      MoleculeExtensionImpl::class.java,
    )

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
        |            implementation("$MOLECULE_RUNTIME")
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
        MOLECULE_RUNTIME
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

  override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

  override fun getCompilerPluginId(): String = "app.cash.molecule"

  override fun getPluginArtifact(): SubpluginArtifact {
    val plugin = extension.kotlinCompilerPlugin.get()
    val parts = plugin.split(":")
    return when (parts.size) {
      1 -> SubpluginArtifact("org.jetbrains.compose.compiler", "compiler", parts[0])
      3 -> SubpluginArtifact(parts[0], parts[1], parts[2])
      else -> error(
        """
        |Illegal format of '$EXTENSION_NAME.${MoleculeExtension::kotlinCompilerPlugin.name}' property.
        |Expected format: either '<VERSION>' or '<GROUP_ID>:<ARTIFACT_ID>:<VERSION>'
        |Actual value: '$plugin'
        """.trimMargin(),
      )
    }
  }

  private fun Project.isInternal(): Boolean {
    return properties["app.cash.molecule.internal"].toString() == "true"
  }

  override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
    return kotlinCompilation.target.project.provider { emptyList() }
  }
}
