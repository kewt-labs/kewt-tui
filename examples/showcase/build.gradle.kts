plugins {
    alias(libs.plugins.kewt.example.application)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.kewtBom)
        }
    }
}
