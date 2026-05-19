/*
* Copyright 2026 Kewt Labs
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
* */
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")

            configure<KotlinMultiplatformExtension> {
                applyDefaultHierarchyTemplate()
                explicitApi()
                linuxX64()
                linuxArm64()
                macosArm64()

                sourceSets.apply {
                    commonTest.dependencies {
                        implementation(kotlin("test"))
                    }
                }

                compilerOptions {
                    progressiveMode.set(true)

                    allWarningsAsErrors.set(System.getenv("CI")?.toBoolean() ?: false)

                    freeCompilerArgs.addAll(
                        "-opt-in=kotlin.RequiresOptIn",
                        "-opt-in=kotlin.ExperimentalStdlibApi",
                        "-Xexpect-actual-classes",
                    )
                }

                tasks.withType<AbstractTestTask>().configureEach {
                    testLogging {
                        events("passed", "skipped", "failed")
                        showStandardStreams = false
                        showExceptions = true
                        showCauses = true
                        showStackTraces = true
                    }
                }
            }
        }
}
