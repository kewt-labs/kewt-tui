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

import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

internal fun Project.configureKover() =
    extensions.configure<KoverProjectExtension> {
        reports {
            filters {
                excludes {
                    packages("*.generated.*")
                    packages("*.testing.*")
                }
            }
            verify {
                rule {
                    minBound(85)
                }
            }
            total {
                xml {
                    onCheck.set(true)
                    xmlFile.set(layout.buildDirectory.file("reports/kover/report.xml"))
                }
                html {
                    onCheck.set(true)
                    htmlDir.set(layout.buildDirectory.dir("reports/kover/html"))
                }
            }
        }
    }
