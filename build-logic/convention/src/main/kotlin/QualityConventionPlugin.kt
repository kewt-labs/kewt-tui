/*
* Copyright 2026 lscythe
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
import dev.kewt.convention.configureDetekt
import dev.kewt.convention.configureDokka
import dev.kewt.convention.configureKover
import dev.kewt.convention.configureSpotlessCommon
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.withType

class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "dev.detekt")
            apply(plugin = "org.jetbrains.dokka")
            apply(plugin = "org.jetbrains.kotlinx.kover")
            apply(plugin = "com.diffplug.spotless")

            configureDokka()
            configureKover()
            configureSpotlessCommon()
            configureDetekt()

            tasks.withType<AbstractTestTask>().configureEach {
                testLogging {
                    events("passed", "skipped", "failed")
                    showStandardStreams = false
                    showExceptions = true
                    showCauses = true
                    showStackTraces = true
                    exceptionFormat = TestExceptionFormat.FULL
                }
            }

            tasks.register("qualityCheck") {
                group = "verification"
                description = "Runs all quality checks (tests, coverage, lint, detekt, docs)"

                dependsOn(
                    tasks.named("allTests"),
                    tasks.named("koverVerify"),
                    tasks.named("ktlintCheck"),
                    tasks.named("detekt"),
                    tasks.named("dokkaGeneratePublicationHtml"),
                )
            }
        }
    }
}
