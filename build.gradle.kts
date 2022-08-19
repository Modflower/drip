import java.net.URLEncoder
import java.nio.charset.StandardCharsets

plugins {
	java
	`java-library`
	alias(libs.plugins.loom)
	`maven-publish`
	alias(libs.plugins.spotless)
	alias(libs.plugins.minotaur)
}

val modrinthId: String by project
val minecraftCompatible: String by project
val project_version: String by project

val isPublish = System.getenv("GITHUB_EVENT_NAME") == "release"
val isRelease = System.getenv("BUILD_RELEASE").toBoolean()
val isActions = System.getenv("GITHUB_ACTIONS").toBoolean()
val baseVersion: String = "$project_version+mc.${libs.versions.minecraft.version.get()}"

version =
	when {
		isRelease -> baseVersion
		isActions ->
			"$baseVersion-build.${System.getenv("GITHUB_RUN_NUMBER")}-commit.${System.getenv("GITHUB_SHA").substring(0, 7)}-branch.${System.getenv("GITHUB_REF")?.substring(11)?.replace('/', '.') ?: "unknown"}"
		else -> "$baseVersion-build.local"
	}

java {
	sourceCompatibility = JavaVersion.VERSION_17
	targetCompatibility = JavaVersion.VERSION_17
	withSourcesJar()
	withJavadocJar()
}

repositories {
	mavenCentral()
	maven("Nexus Repository OSS") {
		url = uri("https://oss.sonatype.org/content/repositories/snapshots")
	}
	maven("Modrinth") {
		url = uri("https://api.modrinth.com/maven")
	}
}

dependencies {
	minecraft(libs.minecraft)
	// TODO: Migrate to QM when possible
	mappings("net.fabricmc", "yarn", libs.versions.yarn.get(), classifier = "v2")
	// mappings(loom.layered {
	// 	addLayer(quiltMappings.mappings("org.quiltmc:quilt-mappings:${libs.versions.quilt.mappings.get()}:v2"))
	// })
	modImplementation(libs.quilt.loader)
	modRuntimeOnly(libs.bundles.mod.runtime)
}

spotless {
	val licenseHeader = rootDir.resolve(".internal/license-header.java")
	java {
		importOrderFile(rootDir.resolve(".internal/spotless.importorder"))

		// If the spotless config doesn't exist, this will fall back to the eclipse default.
		val eclipse = eclipse()
		val eclipseConfig = rootDir.resolve(".internal/spotless.xml")
		if (eclipseConfig.exists()) eclipse.configFile(eclipseConfig)

		// If the license header doesn't exist, it'll simply not be applied.
		if (licenseHeader.exists()) licenseHeaderFile(licenseHeader)
	}
	kotlinGradle {
		target("*.gradle.kts")
		if (licenseHeader.exists()) licenseHeaderFile(licenseHeader, "(import|plugins|rootProject)")
	}
}

tasks {
	withType<JavaCompile> {
		options.encoding = "UTF-8"
		options.isDeprecation = true
		options.isWarnings = true
	}
	processResources {
		val map =
			mapOf(
				"id" to project.name,
				"java" to java.targetCompatibility.majorVersion,
				"version" to project.version,
				"project_version" to project_version,
				"minecraft_required" to libs.versions.minecraft.required.get()
			)
		inputs.properties(map)

		filesMatching("fabric.mod.json") { expand(map) }
	}
	withType<Jar> { from("LICENSE") }
}

modrinth {
	token.set(System.getenv("MODRINTH_TOKEN"))
	projectId.set(modrinthId)
	versionType.set(
		System.getenv("RELEASE_OVERRIDE") ?: when {
			"alpha" in project_version -> "alpha"
			!isRelease || '-' in project_version -> "beta"
			else -> "release"
		}
	)
	val ref = System.getenv("GITHUB_REF")
	changelog.set(
		System.getenv("CHANGELOG") ?: if (ref != null && ref.startsWith("refs/tags/")) "You may view the changelog at https://github.com/KJP12/drip/releases/tag/${URLEncoder.encode(ref.substring(10), StandardCharsets.UTF_8)}"
		else "No changelog is available. Perhaps poke at https://github.com/KJP12/drip for a changelog?"
	)
	uploadFile.set(tasks.remapJar.get())
	gameVersions.set(minecraftCompatible.split(","))
	loaders.addAll("fabric", "quilt")
}
