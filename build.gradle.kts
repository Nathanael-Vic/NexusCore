plugins {
    id("net.neoforged.moddev") version "2.0.140"
}

fun prop(name: String) = project.property(name) as String

version = prop("mod_version")
group = prop("mod_group_id")

base {
    archivesName = prop("mod_id")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

repositories {
    mavenCentral()
    maven("https://maven.neoforged.net/releases/") { name = "NeoForge" }
    maven("https://maven.isxander.dev/releases") { name = "isXander (YACL)" }
    maven("https://maven.caffeinemc.net/releases") { name = "CaffeineMC (Sodium)" }
    maven("https://maven.gegy.dev") { name = "Gegy (LambDynamicLights, Yumi)" }
    maven("https://maven.maxhenkel.de/repository/public") { name = "Henkelmax (Simple Voice Chat)" }
    exclusiveContent {
        forRepository {
            maven("https://api.modrinth.com/maven") { name = "Modrinth" }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
}

neoForge {
    version = prop("neo_version")

    runs {
        register("client") { client() }
        register("server") { server() }
    }

    mods {
        register(prop("mod_id")) {
            sourceSet(sourceSets.main.get())
        }
    }
}

// Alguns jars do Gegy publicam variantes por mapeamento; pedimos a mojmap.
val mappingsAttribute: Attribute<String> = Attribute.of("net.minecraft.mappings", String::class.java)

/**
 * Um canto de codigo que existe so para o compilador ler: `net.fabricmc.api`.
 *
 * Varios mods de que este projeto depende (SpruceUI, LambDynamicLights, Yumi, CreativeCore) sao
 * publicados tambem para Fabric, e as classes deles carregam `@Environment(CLIENT)` gravado dentro.
 * O javac le a anotacao ao abrir o arquivo, nao acha o enum, e avisa — trinta vezes por compilacao.
 * Nada quebra (a anotacao e do Fabric e ninguem aqui a consulta), mas aviso repetido e ruido que
 * esconde aviso de verdade.
 *
 * Com o enum resolvivel, ele cala. Fica num source set a parte, que entra so no caminho de
 * compilacao: o `jar` empacota o `main`, entao nada disso vai para dentro do mod.
 */
val stubs = sourceSets.create("stubs")

dependencies {
    attributesSchema {
        attribute(mappingsAttribute)
    }

    // O canto de codigo la de cima: so para o javac resolver o @Environment do Fabric e calar.
    compileOnly(stubs.output)

    // Anotacoes @NonNull usadas no codigo portado do XSO.
    compileOnly("org.jspecify:jspecify:1.0.0")

    // ---- compat.sodium ----
    // YACL monta a tela de opcoes; vai embutido para ninguem instalar a parte.
    implementation("dev.isxander:yet-another-config-lib:${prop("yacl_version")}")
    jarJar("dev.isxander:yet-another-config-lib:${prop("yacl_version")}")

    compileOnly("net.caffeinemc:sodium-neoforge-mod:${prop("sodium_version")}")
    runtimeOnly("net.caffeinemc:sodium-neoforge:${prop("sodium_version")}")

    // Integracoes opcionais do XSO: so compilacao; em jogo, so ativam se o mod estiver instalado.
    // (O Sodium Extra nao entra aqui: o XSO so reconhece o mod pelo id, sem tocar em classe dele.)
    compileOnly("maven.modrinth:moreculling:${prop("moreculling_modrinth")}")
    compileOnly("maven.modrinth:iris:${prop("iris_modrinth")}")
    compileOnly("maven.modrinth:fancymenu:${prop("fancymenu_modrinth")}")
    compileOnly("dev.lambdaurora.lambdynamiclights:lambdynamiclights-runtime:${prop("ldl_version")}") {
        attributes { attribute(mappingsAttribute, "mojmap") }
    }
    compileOnly("dev.lambdaurora:spruceui:${prop("spruceui_version")}") {
        attributes { attribute(mappingsAttribute, "mojmap") }
    }
    // O LambDynamicLights estende classes do Yumi; o XSO ja embutia, mantemos.
    compileOnly("dev.yumi.mc.core:yumi-mc-foundation:${prop("yumi_version")}") {
        attributes { attribute(mappingsAttribute, "mojmap") }
    }
    jarJar("dev.yumi.mc.core:yumi-mc-foundation:${prop("yumi_version")}") {
        attributes { attribute(mappingsAttribute, "mojmap") }
    }

    // ---- compat.waterframes ----
    compileOnly("maven.modrinth:waterframes:${prop("waterframes_modrinth")}")
    compileOnly("maven.modrinth:creativecore:${prop("creativecore_modrinth")}")

    // ---- backrooms ----
    // So a API do Simple Voice Chat (um jar pequeno, sem o mod): e por ela que o skinwalker
    // guarda e repete a voz das pessoas. Sem o mod instalado, a parte de voz nao carrega.
    compileOnly("de.maxhenkel.voicechat:voicechat-api:${prop("voicechat_api_version")}")

    // Mods do modpack com que as backrooms conversam. So compilacao: em jogo, cada integracao
    // confere se o mod esta instalado antes de tocar em qualquer classe dele.
    // (O Xaero nao aparece aqui de proposito: o bloqueio dos mapas usa os efeitos de status que os
    // proprios mods registram, procurados pelo id — nao precisa do jar para compilar.)
    compileOnly("maven.modrinth:curios:${prop("curios_modrinth")}")
    compileOnly("maven.modrinth:playerrevive:${prop("playerrevive_modrinth")}")
    // O WaterMedia toca a musica da radio no cliente (o servidor so guarda o endereco).
    compileOnly("maven.modrinth:watermedia:${prop("watermedia_modrinth")}")

    // O Jade e quem mostra a plaquinha de quem voce esta olhando: dele so precisamos da API para
    // tirar a linha que entrega de que mod o skinwalker e.
    compileOnly("maven.modrinth:jade:${prop("jade_modrinth")}")

    // O AppleSkin desenha o contorno de saturacao por cima da barra de fome; para o vampiro, o
    // NexusCore troca esse contorno pelo das gotas de sangue. So a API de eventos dele.
    compileOnly("maven.modrinth:appleskin:${prop("appleskin_modrinth")}")

    // O Envelope e o correio por pombos: o NexusCore faz o pombo vir de longe buscar as cartas e,
    // para os lordes, troca o pombo por um morcego. Mexe em classes internas dele (mixins em
    // mixin/envelope), por isso a versao fica presa na faixa testada.
    compileOnly("maven.modrinth:envelope:${prop("envelope_modrinth")}")

    // ---- vampiro ----
    // O Vampirism inteiro fica como esta; o que o NexusCore faz e cercar: so lorde progride, nao
    // existe cacador, e nada nasce sozinho no mundo. Tudo pelos eventos que a API dele publica.
    compileOnly("maven.modrinth:vampirism:${prop("vampirism_modrinth")}")

    // ---- magia ----
    // O Iron's Spells inteiro fica no mundo; o que o NexusCore faz e fechar a magia para quem nao e
    // mago. Um mixin em AbstractSpell (mixin/magic), por isso a versao fica presa na faixa testada.
    // O Iron's Lib vai junto so porque classes do Iron's Spells o citam nas assinaturas.
    compileOnly("maven.modrinth:irons-spells-n-spellbooks:${prop("irons_spellbooks_modrinth")}")
    compileOnly("maven.modrinth:irons-lib:${prop("irons_lib_modrinth")}")
    // O JEI so para esconder as paginas das magias de quem nao e mago (magic/client/jei).
    compileOnly("maven.modrinth:jei:${prop("jei_modrinth")}")
}

// Licao do bug "åž" do port antigo: sem isto o javac le os .java como windows-1252.
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.processResources {
    filteringCharset = "UTF-8"
    val props = mapOf(
        "mod_id" to prop("mod_id"),
        "mod_name" to prop("mod_name"),
        "mod_version" to prop("mod_version"),
        "mod_license" to prop("mod_license"),
        "minecraft_version" to prop("minecraft_version"),
    )
    inputs.properties(props)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(props)
    }
}

tasks.jar {
    from("LICENSE") {
        rename { "LICENSE_${prop("mod_id")}" }
    }
}

// Nexus Resources e um resource pack NORMAL, separado do jar: pode sobrescrever texturas de qualquer mod.
// O build gera o zip pronto para distribuir em build/resourcepacks/.
val packNexusResources by tasks.registering(Zip::class) {
    group = "build"
    description = "Empacota resourcepacks/Nexus Resources em um zip de resource pack."
    from(layout.projectDirectory.dir("resourcepacks/Nexus Resources"))
    archiveFileName = "Nexus Resources.zip"
    destinationDirectory = layout.buildDirectory.dir("resourcepacks")
}

// Instalacao automatica (opcional): com nexuscore.installDir definido em ~/.gradle/gradle.properties,
// todo build poe o jar novo nessa pasta mods, no lugar do antigo. Sem a propriedade, nao faz nada.
val installDir = providers.gradleProperty("nexuscore.installDir").map { file(it) }
val installMod by tasks.registering(Copy::class) {
    group = "build"
    description = "Copia o jar do mod para a pasta mods definida em nexuscore.installDir."
    onlyIf { installDir.orNull?.isDirectory == true }
    doFirst {
        installDir.get().listFiles { f -> f.name.startsWith("${prop("mod_id")}-") && f.name.endsWith(".jar") }
            ?.forEach { it.delete() }
    }
    from(tasks.jar)
    into(installDir.orElse(layout.buildDirectory.dir("install").map { it.asFile }))
}

tasks.named("build") {
    dependsOn(packNexusResources)
    finalizedBy(installMod)
}
