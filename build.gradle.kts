plugins {
    `maven-publish`
    kotlin("jvm") version "1.9.21"
    application
}

repositories {
    mavenLocal()
    mavenCentral()
}

group = "ru.fiddlededee"
version = "0.6"

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.2")
    testImplementation("com.approvaltests:approvaltests:18.5.0")
    testImplementation("org.asciidoctor:asciidoctorj:2.5.7")
    implementation("org.jsoup:jsoup:1.17.1")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.16.1")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.16.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.16.1")
    implementation("org.redundent:kotlin-xml-builder:1.9.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

application {
    mainClass.set("MainKt")
}
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
