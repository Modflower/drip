rootProject.name = "drip"

pluginManagement {
	repositories {
		maven("Quilt") {
			url = uri("https://maven.quiltmc.org/repository/release")
		}
		maven("Fabric") {
			url = uri("https://maven.fabricmc.net/")
		}
		gradlePluginPortal()
	}
}
