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
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "dev.kewt.buildlogic"

java {
    targetCompatibility = JavaVersion.VERSION_21
    sourceCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

dependencies {
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.dokka.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.kover.gradlePlugin)
    compileOnly(libs.spotless.gradlePlugin)
}

tasks.validatePlugins {
    enableStricterValidation = true
    failOnWarning = true
}

gradlePlugin {
    plugins {
        register("quality") {
            id = "kewt.quality"
            implementationClass = "QualityConventionPlugin"
        }
        register("root") {
            id = "kewt.root"
            implementationClass = "RootPlugin"
        }
        register("kmpLibrary") {
            id = "kewt.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("kewtExampleApplication") {
            id = "kewt.example.application"
            implementationClass = "KewtExampleApplicationConventionPlugin"
        }
    }
}
