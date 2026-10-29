# Recovered build environment

The project still uses the unchanged ModDev 2.0.148 / NeoForge 21.1.259 build.
The workspace toolchain is Temurin 21.0.13+9 and Gradle 9.2.1. Official download
URLs and SHA-256 hashes are in `toolchain-provenance.json`.

Place this directory beside the `thaumcraft-port` project. Extract the official
JDK into `jdk-21.0.13+9` and Gradle into `gradle-9.2.1`. The runner owns its
Gradle cache and adapts the environment HTTP proxy and Java truststore:

    python3 build-support/run-gradle.py compileJava compileTestJava
    python3 build-support/run-gradle.py test runData runGameTestServer build

On a normal machine, the project's standard `gradle` commands are sufficient;
the workspace-only patch below is unnecessary.

In this container, Java's `ProcessHandle.current().info().command()` returns
`Optional.empty`. NeoFormRuntime 2.0.31 assumes the command exists and throws
before downloading or processing Minecraft. The failed runs are preserved in
`recovery-compile.log` and `recovery-compile-patched.log`. The two affected
classes are `NeoFormEngine` and `ExternalJavaToolAction`; their only source
change is the same fallback to
`Path.of(System.getProperty("java.home"), "bin", "java")` when the command is absent.

After a first dependency download, fetch the official 2.0.31 sources JAR from
the URL in `toolchain-provenance.json`, place it alongside the runner, and run
`python3 build-support/patch-runtime.py`. `runtime.init.gradle` then substitutes
that workspace build runtime for ModDev's artifact generation tasks. It changes
no game/mod source, final mod classes, dependency versions, or project settings.
Remove `runtime.init.gradle` when using an unpatched normal environment.

The downloaded JDK, Gradle, Maven caches and patched runtime JAR are build
dependencies, not intended for inclusion in the mod or source checkpoint.
