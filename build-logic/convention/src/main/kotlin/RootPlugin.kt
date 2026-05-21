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
import dev.kewt.convention.configureSpotlessForRootProject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.dokka.gradle.DokkaExtension

/**
 * A convention plugin for the root project.
 *
 * Configures aggregate tasks like 'allTests' and project-wide tools
 * like Dokka HTML aggregation and Spotless for build scripts.
 */
abstract class RootPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.dokka")
            configureSpotlessForRootProject()

            tasks.register("allTests") {
                group = "verification"
                description = "Runs all tests in all modules"
                dependsOn(subprojects.mapNotNull { it.tasks.findByName("allTests") })
            }

            dependencies {
                subprojects.forEach { subproject ->
                    if (subproject.path.startsWith(":kewt-")) {
                        "dokka"(subproject)
                    }
                }
            }

            extensions.configure<DokkaExtension> {
                dokkaPublications.getByName("html") {
                    moduleName.set("Kewt")
                    outputDirectory.set(layout.buildDirectory.dir("dokka/html"))
                }
            }
        }
    }
}
