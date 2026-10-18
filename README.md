<p align="center">
  <img src="src/main/resources/nexuscore_logo.png" width="128" alt="NexusCore">
</p>

<h1 align="center">NexusCore</h1>

<p align="center">
  O mod central do modpack NexusSMP.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.1-62B47A?style=flat-square" alt="Minecraft 1.21.1">
  <img src="https://img.shields.io/badge/NeoForge-21.1-E68C37?style=flat-square" alt="NeoForge 21.1">
  <img src="https://img.shields.io/badge/vers%C3%A3o-0.1.4%20beta-8C6BC8?style=flat-square" alt="Versão 0.1.4 beta">
  <img src="https://img.shields.io/badge/licen%C3%A7a-LGPL--3.0-3B6FB6?style=flat-square" alt="Licença LGPL-3.0">
</p>

---

O NexusCore é a base do modpack NexusSMP, um servidor pequeno que montei para jogar com os meus
amigos. É nele que ficam os sistemas próprios do servidor e os ajustes que fazem os outros mods do
pack funcionarem bem juntos.

Não é um projeto feito para crescer nem para rodar em outros servidores: ele existe para a nossa
diversão e vai mudando conforme as ideias aparecem nas partidas. Mesmo assim, não depende de nenhum
outro mod para abrir: cada integração só liga se o mod dela estiver instalado. Os detalhes de cada
parte ficam em [`docs/`](docs).

## Compilando

Precisa do JDK 21.

```
./gradlew build
```

O jar sai em `build/libs/`.

## Licença

[LGPL-3.0-or-later](LICENSE). Inclui partes de terceiros sob LGPL-3.0: um port do Xander's Sodium
Options: Continued, os sons das Backrooms do SP-Backrooms Revamped (créditos em
`src/main/resources/assets/nexuscore/sounds/backrooms/`) e o YetAnotherConfigLib, embutido no jar.

---

<sub>Feito por [Nathanael](https://github.com/Nathanael-Vic), com uma ajuda do [Claude Code](https://claude.com/claude-code). O ícone é arte de um amigo do servidor.</sub>
