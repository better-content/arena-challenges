plugins {
    java
    id("net.minecraftforge.gradle") version "6.0.24"
}

group = property("mod_group") as String
version = property("mod_version") as String
base { archivesName.set("arena-challenges") }

java.toolchain.languageVersion.set(JavaLanguageVersion.of(17))

repositories {
    maven("https://maven.minecraftforge.net")
    maven("https://thedarkcolour.github.io/KotlinForForge/")
    mavenCentral()
    flatDir { dirs(System.getenv("BC_CUSTOM_MOD_JAR_DIR") ?: project.file("test-libs")) }
}

dependencies {
    minecraft("net.minecraftforge:forge:${property("minecraft_version")}-${property("forge_version")}")
    implementation("thedarkcolour:kotlinforforge:${property("kotlinforforge_version")}")
    implementation(fg.deobf("local:player-traces:0.1.0"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

minecraft {
    mappings("official", property("minecraft_version") as String)
    runs {
        val baseClient = create("client")
        configureEach {
            workingDirectory(project.file("run"))
            property("forge.logging.console.level", "info")
            mods { create("arena_challenges") { source(sourceSets.main.get()) } }
        }
        create("visualClient") {
            parent(baseClient)
            workingDirectory(project.file("build/visual-run"))
            property("arena.visualValidation", "true")
            property("forge.logging.console.level", "info")
        }
        create("server") { args("--nogui") }
        create("gameTestServer") {
            workingDirectory(project.file("run-gametest"))
            args("--nogui")
            property("forge.enabledGameTestNamespaces", "arena_challenges")
        }
    }
}

tasks.processResources {
    val tokens = mapOf(
        "mod_id" to project.property("mod_id"),
        "mod_name" to project.property("mod_name"),
        "mod_version" to project.property("mod_version"),
        "minecraft_version" to project.property("minecraft_version"),
        "forge_version" to project.property("forge_version")
    )
    inputs.properties(tokens)
    filesMatching("META-INF/mods.toml") { expand(tokens) }
}

tasks.withType<JavaCompile>().configureEach { options.release.set(17) }
tasks.test { useJUnitPlatform() }

val prepareGameTestStructures by tasks.registering(Copy::class) {
    from("src/main/resources/gameteststructures")
    into("run-gametest/gameteststructures")
}
tasks.configureEach {
    if (name == "runGameTestServer") dependsOn(prepareGameTestStructures)
}

tasks.register("verifyFast") { dependsOn("check") }
tasks.register("headlessGameTest") {
    dependsOn("runGameTestServer")
    doLast {
        val log = layout.projectDirectory.file("run-gametest/logs/latest.log").asFile
        val output = if (log.isFile) log.readText() else ""
        check(output.contains("Started game test server")) {
            "Arena Challenges GameTest server did not reach server startup; inspect run-gametest/logs/latest.log"
        }
        val completed = Regex("All (\\d+) required tests passed").find(output)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        check(completed > 0) {
            "Arena Challenges GameTest run completed no tests; inspect run-gametest/logs/latest.log"
        }
        check(!output.contains("[main/FATAL]") && !output.contains("Failed to complete lifecycle event")) {
            "Arena Challenges GameTest server logged a fatal/error; inspect run-gametest/logs/latest.log"
        }
    }
}
tasks.register("verifyFull") { dependsOn("verifyFast", "headlessGameTest") }

val runtimeArtifactName = "arena-challenges-${project.version}.jar"
val stageRuntimeJar by tasks.registering(Copy::class) {
    dependsOn("reobfJar")
    from(layout.buildDirectory.file("reobfJar/output.jar"))
    into(layout.buildDirectory.dir("libs"))
    rename { runtimeArtifactName }
}
tasks.named("assemble") { dependsOn(stageRuntimeJar) }
