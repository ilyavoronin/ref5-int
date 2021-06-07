plugins {
    java
    kotlin("jvm") version "1.4.10"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation(platform("org.junit:junit-bom:5.7.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks {
    task("buildInt", type = Jar::class) {
        archiveFileName.set("../../ref5.jar")
        manifest {
            attributes(mapOf("Main-Class" to "RunInterpreterKt"))
        }
        isZip64 = true
        from(project.configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
        with(getByName("jar") as CopySpec)
    }
}