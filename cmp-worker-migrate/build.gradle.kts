plugins {
    kotlin("jvm")
    `java-gradle-plugin`
}

group = "io.github.mobilebytelabs"
version = providers.gradleProperty("worker.version").get()

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.0.21")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

gradlePlugin {
    plugins {
        create("workerMigrate") {
            id = "io.github.mobilebytelabs.worker.migrate"
            implementationClass = "io.github.mobilebytelabs.worker.migrate.WorkerMigratePlugin"
            displayName = "worker-kmp v2 → v3 Migration"
            description = "Scans v2.x consumer source via Kotlin compiler frontend + emits unified diffs for v3 migration. confidence-classified."
        }
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

// Note: this plugin module is intentionally simple at alpha08; the AST scanner +
// confidence classifier + 5 integration fixtures land in alpha08.X follow-ups.
