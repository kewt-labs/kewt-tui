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
package dev.kewt.convention

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

internal fun Project.configureSpotlessForRootProject() {
    apply(plugin = "com.diffplug.spotless")
    extensions.configure<SpotlessExtension> {
        kotlin {
            target("build-logic/convention/src/**/*.kt")
            ktlint(libs.version("ktlint"))
            licenseHeaderFile(rootDir.resolve("config/spotless/copyright.kt"))
            endWithNewline()
        }
        format("kts") {
            target("*.kts", "build-logic/*.kts", "build-logic/convention/*.kts")
            // Look for the first line that doesn't have a block comment (assumed to be the license)
            licenseHeaderFile(rootDir.resolve("config/spotless/copyright.kts"), "^[a-zA-Z@]")
            endWithNewline()
        }
    }
}

internal fun Project.configureSpotlessCommon() {
    extensions.configure<SpotlessExtension> {
        kotlin {
            target("src/**/*.kt")
            ktlint(libs.version("ktlint"))
                .editorConfigOverride(
                    mapOf("ktlint_standard_function-naming" to "disabled"),
                )
            licenseHeaderFile(rootDir.resolve("config/spotless/copyright.kt"))
            endWithNewline()
        }
        format("kts") {
            target("*.kts")
            licenseHeaderFile(rootDir.resolve("config/spotless/copyright.kts"), "^[a-zA-Z@]")
            endWithNewline()
        }
    }
}
