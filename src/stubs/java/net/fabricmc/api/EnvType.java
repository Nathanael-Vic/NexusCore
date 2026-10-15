package net.fabricmc.api;

/**
 * Os dois lados, do jeito que o Fabric os nomeia.
 *
 * <p>Esta classe nao e usada por nada aqui dentro: ela existe so para o compilador ter o que ler.
 * Alguns mods de que este projeto depende (SpruceUI, LambDynamicLights, Yumi, CreativeCore) sao
 * publicados tambem para Fabric, e por isso as classes deles carregam {@code @Environment(CLIENT)}
 * gravado dentro. O javac le essa anotacao ao abrir o arquivo, nao acha o enum, e avisa — trinta
 * vezes por compilacao. Nada quebra: a anotacao e do Fabric e ninguem aqui a consulta.
 *
 * <p>Com o enum aqui, ele acha, resolve e cala. E um <b>source set separado</b>, que entra so no
 * caminho de compilacao: nao vai para o jar, nao e registrado, nao existe em jogo. Se um dia essas
 * dependencias sairem, esta pasta sai junto sem deixar rastro.
 */
public enum EnvType {
    CLIENT,
    SERVER
}
