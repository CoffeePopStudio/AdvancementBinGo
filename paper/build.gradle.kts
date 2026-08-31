plugins {
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    implementation(project(":core"))
    implementation(project(":storage"))

    compileOnly(libs.paper.api)

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
