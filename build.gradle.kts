import org.apache.commons.lang3.SystemUtils
plugins {
    idea
    java
    id("gg.essential.loom") version "0.10.0.+"
    id("dev.architectury.architectury-pack200") version "0.1.3"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}
//Constants:
val baseGroup: String by project
val mcVersion: String by project
val version: String by project
val mixinGroup = "$baseGroup.mixin"
val modid: String by project
val jarName: String by project
val transformerFile = file("src/main/resources/accesstransformer.cfg")
// Toolchains:
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}
// Minecraft configuration:
loom {
    log4jConfigs.from(file("log4j2.xml"))
    launchConfigs {
        "client" {
            // If you don't want mixins, remove these lines
            property("mixin.debug", "true")
            arg("--tweakClass", "org.spongepowered.asm.launch.MixinTweaker")
        }
    }
    runConfigs {
        "client" {
            if (SystemUtils.IS_OS_MAC_OSX) {
                // This argument causes a crash on macOS
                vmArgs.remove("-XstartOnFirstThread")
            }
        }
        remove(getByName("server"))
    }
    forge {
        pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter())
        // If you don't want mixins, remove this lines
        mixinConfig("mixins.$modid.json")
	    if (transformerFile.exists()) {
			println("Installing access transformer")
		    accessTransformer(transformerFile)
	    }
    }
    // If you don't want mixins, remove these lines
    mixin {
        defaultRefmapName.set("mixins.$modid.refmap.json")
    }
}
sourceSets.main {
    output.setResourcesDir(sourceSets.main.flatMap { it.java.classesDirectory })
}
// Dependencies:
repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/maven/")
    // If you don't want to log in with your real minecraft account, remove this line
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
}
val shadowImpl: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}
dependencies {
    minecraft("com.mojang:minecraft:1.8.9")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")
    // If you don't want mixins, remove these lines
    shadowImpl("org.spongepowered:mixin:0.7.11-SNAPSHOT") {
        isTransitive = false
    }
    annotationProcessor("org.spongepowered:mixin:0.8.5-SNAPSHOT")
    // If you don't want to log in with your real minecraft account, remove this line
    runtimeOnly("me.djtheredstoner:DevAuth-forge-legacy:1.2.1")
}
// Tasks:
tasks.withType(JavaCompile::class) {
    options.encoding = "UTF-8"
    // Mixin 的注解处理器每次编译都会「重写」mixins.ataloss.refmap.json，只写本次参与编译的类。
    // 打开增量编译后，小改动会把 refmap 截成几条，jar 里 @Inject/@Accessor 全部按 MCP 原名去找
    // 生产环境（srg）的目标 → could not find any targets / InvalidAccessorException。
    // 因此关闭增量编译，保证每次都是完整 refmap。
    options.isIncremental = false
}
tasks.withType(org.gradle.jvm.tasks.Jar::class) {
    archiveBaseName.set(jarName)
    manifest.attributes.run {
        // 显式 core plugin：由它 MixinBootstrap.init() + addConfiguration，
        // 不再依赖 FML 对普通 jar 的 TweakClass 扫描（那是静默失败的重灾区）
        this["FMLCorePlugin"] = "myau.init.ATALOSSCorePlugin"
        this["FMLCorePluginContainsFMLMod"] = "true"
        this["ForceLoadAsMod"] = "true"
        this["TweakClass"] = "org.spongepowered.asm.launch.MixinTweaker"
        this["MixinConfigs"] = "mixins.$modid.json"
	    if (transformerFile.exists())
			this["FMLAT"] = "${modid}_at.cfg"
    }
}
tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("mcversion", mcVersion)
    inputs.property("modid", modid)
    inputs.property("basePackage", baseGroup)
    filesMatching(listOf("mcmod.info", "mixins.$modid.json","version.json")) {
        expand(inputs.properties)
    }
    rename("accesstransformer.cfg", "META-INF/${modid}_at.cfg")
}
val remapJar by tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
    archiveClassifier.set("")
    from(tasks.shadowJar)
    input.set(tasks.shadowJar.get().archiveFile)
}
tasks.jar {
    archiveClassifier.set("without-deps")
    destinationDirectory.set(layout.buildDirectory.dir("intermediates"))
}
tasks.shadowJar {
    destinationDirectory.set(layout.buildDirectory.dir("intermediates"))
    archiveClassifier.set("non-obfuscated-with-deps")
    configurations = listOf(shadowImpl)
    doLast {
        configurations.forEach {
            println("Copying dependencies into mod: ${it.files}")
        }
    }
    // If you want to include other dependencies and shadow them, you can relocate them in here
    fun relocate(name: String) = relocate(name, "$baseGroup.deps.$name")
}
tasks.assemble.get().dependsOn(tasks.remapJar)
