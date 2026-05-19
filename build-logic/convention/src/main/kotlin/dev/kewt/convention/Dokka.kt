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

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier

internal fun Project.configureDokka() {
    extensions.configure<DokkaExtension> {
        dokkaSourceSets.configureEach {
            documentedVisibilities(VisibilityModifier.Public)
            skipEmptyPackages.set(true)
            skipDeprecated.set(true)
            reportUndocumented.set(true)

            sourceLink {
                localDirectory.set(file("src"))
                remoteUrl("https://github.com/kewt-labs/kewt-tui/tree/main/${project.name}/src")

                remoteLineSuffix.set("#L")
            }

            externalDocumentationLinks.configureEach {
                url("https://kotlinlang.org/api/latest/jvm/stdlib/")
            }

            externalDocumentationLinks.configureEach {
                url("https://kotlinlang.org/api/kotlinx.coroutines/")
            }
        }
    }
}
