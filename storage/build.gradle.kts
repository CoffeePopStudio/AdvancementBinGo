plugins {
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    implementation(libs.sqlite.jdbc)

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
