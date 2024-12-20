// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
}
allprojects {
    gradle.projectsEvaluated {

        tasks.withType<JavaCompile> {
            options.compilerArgs.addAll(arrayOf("-parameters", "-Xlint:deprecation"))
        }
    }
}