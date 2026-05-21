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
import dev.kewt.convention.configureTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

/**
 * A convention plugin that configures an example application.
 *
 * This plugin sets up standard native targets and automatically configures
 * a binary executable with the default 'main' entry point.
 */
class KewtExampleApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")

            extensions.configure<KotlinMultiplatformExtension> {
                configureTarget()

                targets.withType<KotlinNativeTarget> {
                    binaries {
                        executable {
                            entryPoint = "main"
                        }
                    }
                }
            }

            // Configure run tasks to use standard streams for TTY interaction
            tasks.withType<Exec>().configureEach {
                if (name.startsWith("run")) {
                    standardInput = System.`in`
                    standardOutput = System.out
                    errorOutput = System.err
                }
            }

            // Add a convenience 'run' task that points to the host's debug executable
            tasks.register("run") {
                group = "application"
                description = "Runs the application for the host platform"
                val hostTarget =
                    when (val os = System.getProperty("os.name").lowercase()) {
                        "mac os x" -> "MacosArm64"
                        else -> if (System.getProperty("os.arch") == "aarch64") "LinuxArm64" else "LinuxX64"
                    }
                dependsOn("runDebugExecutable$hostTarget")
            }
        }
    }
}
