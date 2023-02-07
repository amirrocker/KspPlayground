plugins {
    kotlin("jvm")
    java
    id("com.google.devtools.ksp") version "1.8.0-1.0.8"
}

group = "de.yello"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    //implementation(project(":annotation"))
    implementation(project(":processor"))
    ksp(project(":processor"))
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}