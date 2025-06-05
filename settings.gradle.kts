pluginManagement {

    /**
     * The pluginManagement.repositories block configures the
     * repositories Gradle uses to search or download the Gradle plugins and
     * their transitive dependencies. Gradle pre-configures support for remote
     * repositories such as JCenter, Maven Central, and Ivy. You can also use
     * local repositories or define your own remote repositories. Here we
     * define the Gradle Plugin Portal, Google's Maven repository,
     * and the Maven Central Repository as the repositories Gradle should use to look for its
     * dependencies.
     */

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()

        maven("https://plugins.gradle.org/m2/") {
            content {
                includeGroupByRegex("com\\.google.*")
                includeGroup("com.squareup")
                includeGroupByRegex("commons-.*")
                includeModule("org.jdom", "jdom2")
                includeModule("org.ow2", "ow2")
                includeGroup("org.ow2.asm")
                includeGroupByRegex("org\\.jetbrains.*")
                includeGroup("org.slf4j")
                includeModule("org.bitbucket.b_c", "jose4j")
                includeModule("org.checkerframework", "checker-qual")
                includeGroup("net.java.dev.jna")
                includeModule("net.java", "jvnet-parent")
                includeModule("javax.annotation", "javax.annotation-api")
                includeGroupByRegex("org\\.apache.*")
                includeGroupByRegex("com\\.sun.*")
                includeModule("xerces", "xercesImpl")
                includeModule("xml-apis", "xml-apis")
                includeGroup("org.bouncycastle")
                includeGroupByRegex("net\\.sf.*")
                includeModule("javax.inject", "javax.inject")
                includeModule("org.tensorflow", "tensorflow-lite-metadata")
                includeModule("org.json", "json")
                includeGroup("io.grpc")
                includeGroup("io.netty")
                includeModule("io.perfmark", "perfmark-api")
                includeModule("org.codehaus.mojo", "animal-sniffer-annotations")
                includeGroup("org.glassfish.jaxb")
                includeGroupByRegex("jakarta.*")
                includeModule("org.jvnet.staxex", "stax-ex")
                includeGroup("com.testdroid")
                includeModule("log4j", "log4j")
                includeModule("com.fasterxml", "oss-parent")
                includeGroupByRegex("com\\.fasterxml\\.jackson.*")
                includeModule("org.sonatype.oss", "oss-parent")
                includeModule("org.eclipse.ee4j", "project")
                includeGroup("org.codehaus.mojo")
            }
        }
    }
}
dependencyResolutionManagement {

    /**
     * The dependencyResolutionManagement.repositories
     * block is where you configure the repositories and dependencies used by
     * all modules in your project, such as libraries that you are using to
     * create your application. However, you should configure module-specific
     * dependencies in each module-level build.gradle file. For new projects,
     * Android Studio includes Google's Maven repository and the Maven Central
     * Repository by default, but it does not configure any dependencies (unless
     * you select a template that requires some).
     */

    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        gradlePluginPortal()
        maven("https://raw.githubusercontent.com/guardianproject/gpmaven/master") {
            content {
                includeModule("org.proofmode", "android-libproofmode")
            }
        }

        maven("https://jitpack.io") {
            content {
                includeModule("com.github.esafirm", "android-image-picker")
                includeModule("com.github.derlio", "audio-waveform")
                includeModule("com.github.abdularis", "circularimageview")
                includeModule("com.github.guardianproject", "sardine-android")
            }
        }
    }
}
rootProject.name = "save-android-old"

include(":app")
