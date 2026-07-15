plugins {
    `java-library`
}

group = "fi.juhpaza"
version = "0.2.5-SNAPSHOT"

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")

    runtimeOnly("org.xerial:sqlite-jdbc:3.49.1.0")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.xerial:sqlite-jdbc:3.49.1.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    processResources {
        filteringCharset = "UTF-8"
        inputs.property("version", project.version)
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }

    test {
        useJUnitPlatform()
    }
}
