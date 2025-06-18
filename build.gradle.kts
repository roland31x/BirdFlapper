plugins {
    kotlin("jvm") version "1.9.0"
    id("org.jetbrains.compose") version "1.5.10"
    application
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.compose.material:material-desktop:1.5.10")
    implementation(compose.desktop.currentOs)
}

application {
    mainClass.set("MainKt")
}