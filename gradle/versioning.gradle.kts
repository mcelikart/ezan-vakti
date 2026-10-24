import java.io.ByteArrayOutputStream
import javax.inject.Inject
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations

/**
 * A ValueSource is checked again when reusing the configuration cache. A changed
 * source tree therefore produces a new version before Android's cached version
 * configuration can be reused. Tests, compilation and IDE sync only read.
 */
abstract class ApplicationVersionValueSource : ValueSource<String, ApplicationVersionValueSource.Parameters> {
    interface Parameters : ValueSourceParameters {
        val projectDirectory: DirectoryProperty
        val preparePackage: Property<Boolean>
        val bump: Property<String>
    }

    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): String {
        val root = parameters.projectDirectory.get().asFile
        val windows = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
        val javaExecutable = java.io.File(System.getProperty("java.home"), "bin/${if (windows) "java.exe" else "java"}")
        val output = ByteArrayOutputStream()
        val errors = ByteArrayOutputStream()
        val result = execOperations.exec {
            commandLine(javaExecutable.absolutePath, java.io.File(root, "scripts/Versioning.java").absolutePath,
                if (parameters.preparePackage.get()) "prepare" else "current", root.absolutePath,
                parameters.bump.get())
            standardOutput = output
            errorOutput = errors
            isIgnoreExitValue = true
        }
        if (result.exitValue != 0) throw GradleException("Application version: ${errors.toString("UTF-8").trim()}")
        return output.toString("UTF-8").trim()
    }
}

// These are the application's APK/AAB entry points. Compile/test tasks, including
// packageDebugUnitTestForUnitTest, must never consume a source change as a release.
val packagingTask = Regex("(?:assemble|bundle|package|install)(?:Debug|Preview|Release)?(?:Bundle)?")
val requestedPackaging = gradle.startParameter.taskNames.any { task ->
    val name = task.substringAfterLast(':')
    name in setOf("build", "buildNeeded", "buildDependents") || packagingTask.matches(name)
}
val preparedVersion = providers.of(ApplicationVersionValueSource::class) {
    parameters.projectDirectory.set(rootProject.layout.projectDirectory)
    // Gradle deliberately shares configuration-cache entries between --dry-run
    // and execution. Reserve versions consistently for packaging requests in both.
    parameters.preparePackage.set(requestedPackaging)
    parameters.bump.set(providers.gradleProperty("versionBump").orElse("patch"))
}.get().split('|')
require(preparedVersion.size == 2) { "Invalid application version result" }
extra["applicationVersionName"] = preparedVersion[0]
extra["applicationVersionCode"] = preparedVersion[1].toInt()
