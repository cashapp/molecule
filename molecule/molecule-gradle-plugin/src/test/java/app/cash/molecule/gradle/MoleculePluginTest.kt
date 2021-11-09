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

import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import java.io.File

class MoleculePluginTest {
  @Test fun testingModuleWithoutVersion() {
    createRunner("testing-module-without-version").build()
  }
  @Test fun testingModuleExplicitVersion() {
    val result = createRunner("testing-module-explicit-version").buildAndFail()
    assertThat(result.output).contains("Could not find app.cash.molecule:molecule-testing:0.0.0")
  }

  private fun createRunner(fixture: String): GradleRunner {
    val fixtureDir = File("src/test/fixtures", fixture)
    val gradleRoot = File(fixtureDir, "gradle").also { it.mkdir() }
    File("../../gradle/wrapper").copyRecursively(File(gradleRoot, "wrapper"), true)
    return GradleRunner.create()
      .withProjectDir(fixtureDir)
      .withArguments("clean", "build", "--stacktrace", "-PmoleculeVersion=$moleculeVersion")
      .forwardOutput()
  }
}
