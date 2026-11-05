rootProject.name = "MzbotanicalsMock"
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositories { google(); mavenCentral() }
}
include(":mockEngine", ":composeApp")
