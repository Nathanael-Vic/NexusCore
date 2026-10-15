# Fase 0: levantamento dos projetos antigos

Inventário feito antes da migração. Serve de checklist de regressão da Fase 1 e registra o que foi conferido no bytecode dos mods alvo.

## 1. Projetos antigos

| Projeto | Onde | Mod ID / pacote | Situação |
|---|---|---|---|
| Port do Xander's Sodium Options | `Desktop\xso-port` (Stonecutter multi-versão, alvo `versions/1.21.1-neoforge`) | `xanders_sodium_options` / `dev.isxander.xso` | base do `compat.sodium`; jar antigo sai do modpack |
| vanillaframes (cores do WaterFrames) | `Desktop\vanillaframes` | `vanillaframes` / `dev.nathanael.vanillaframes` | **substituído** pelo `compat.waterframes` (Fase 2B) |
| Textura antiga do frame | `Desktop\waterframes-skin\pack-frame-classico` | resource pack | movida para `resourcepacks/Nexus Resources` sem alteração |
| Datapack "só frame" | `Desktop\waterframes-skin\datapack-so-frame` | datapack | **não migrar** (fora de escopo, ver abaixo) |

## 2. Versões (profile `[WIP]`)

| Mod | Versão | Observação |
|---|---|---|
| Minecraft / NeoForge | 1.21.1 / 21.1.250 | |
| Sodium | 0.8.13+mc1.21.1 | Config API 0.8 (faixa aceita pelo módulo: `[0.8.12,0.9)`) |
| Sodium Extra | 0.9.3+mc1.21.1 | mod id no NeoForge é `sodium_extra` (o XSO procurava `sodium-extra` e nunca detectava) |
| YACL | 3.8.1+1.21.1-neoforge | embutido no jar do NexusCore |
| Yumi MC Foundation | 1.0.0-alpha.15+1.21.1 | embutido (exigido pela integração do LambDynamicLights) |
| WaterFrames | 2.1.23 (NeoForge) | faixa aceita: `[2.1.23,2.2)` |
| CreativeCore | 2.13.46 | faixa aceita: `[2.13.13,2.14)` |
| WaterMedia | **2.1.37** + YouTube plugin 2.1.2 | a 3.0.0.23 + binaries 3.0.0.6 **crasham** com o WaterFrames 2.1.x: ficam `.disabled` |
| WaterVision | 0.1.0-alpha | pede WaterMedia `[2.1.30,2.2)`, compatível com a 2.1.37 |

Base do XSO: **XSO Continued 3.4.2** (versão 1.21.11), portado para 1.21.1 / Sodium 0.8.13. O bug "åž" no botão "Open Original Screen ➔" era encoding do `javac`; o NexusCore compila com `options.encoding = "UTF-8"`.

## 3. Mixins herdados do XSO (`mixin/sodium/`)

Aplicados só com Sodium na faixa; os de `integration/` também exigem o mod integrado.

| Mixin | Alvo | Tipo | Motivo |
|---|---|---|---|
| `MinecraftClientMixin` | `Minecraft#setScreen` | `@ModifyVariable` HEAD | troca a tela de vídeo do Sodium pela tela YACL convertida |
| `SodiumOptionAccessor` | `Option` (Sodium) | `@Accessor` | lê campos internos da opção para converter |
| `VideoSettingsScreenAccessor` | `VideoSettingsScreen` (Sodium) | `@Accessor` | tela anterior, para o botão "voltar"/"tela original" |
| `TabManagerMixin` | `TabManager#setCurrentTab` | `@Inject` RETURN | avisa a tela YACL da troca de aba (destaque/scroll da aba) |
| `yacl.YACLScreenMixin` | `YACLScreen#init`, `#tick`, `keyPressed` | `@Inject` + override | botão de doação, destaque da aba selecionada, navegação por teclado |
| `yacl.CategoryTabMixin` | `YACLScreen.CategoryTab#<init>` | `@Inject` TAIL | descrição padrão de cada categoria (`xso.category.*`) |
| `yacl.OptionEntryMixin` | `OptionListWidget.OptionEntry#keyPressed`, `#children` | `@Inject` | reset por teclado; espaçadores vazios fora do foco por Tab |
| `yacl.OptionListWidgetMixin` | `OptionListWidget#setHoverDescription` + filtro de entradas | `@ModifyVariable` | descrição de fallback; pula espaçadores vazios |
| `integration.DynamicLightsConfigAccessor` | `DynamicLightsConfig` (LambDynamicLights) | `@Accessor` | valores padrão das opções do LDL |
| `integration.SettingEntryAccessor` | `SettingEntry` (LambDynamicLights) | `@Accessor` | valor padrão de cada entrada |
| `integration.FancyMenuScreenCustomizationMixin` | `ScreenCustomization#reInitCurrentScreen(ZZ)V` (FancyMenu) | `@Inject` HEAD cancelável, `require = 0` | FancyMenu não reinicia a tela YACL (perdia o estado) |

## 4. Config do XSO

- Arquivo antigo: `config/xanders-sodium-options.json` → novo: `config/nexuscore-sodium-options.json` (cópia automática na primeira inicialização, com log).
- Chaves: `enabled`, `lenientOptions`, `hardCrash`, `externalMenus` (mantidas).

## 5. Traduções

- `assets/nexuscore/lang/*.json` com as chaves herdadas `xso.*`, `options.iris.*`, `options.moreculling.*` (37 chaves).
- Idiomas herdados: `en_us`, `ms_my`, `zh_cn`, `zh_tw`, `zlm_arab`. Adicionado `pt_br`.

## 6. WaterFrames: como o visual antigo era trocado

- **vanillaframes** trocava, por reflection no `FMLClientSetupEvent`, os `DisplayColor` dos campos de `ScreenStyles` e dos `GuiStyle` `DISPLAYS`/`REMOTE_CONTROL` por cinzas.
- **Não funcionou em jogo** (a tela continuou azul): as telas sobrescrevem `getStyle`/`getBorder`/`getBackground` e os widgets usam as constantes estáticas diretamente; o `CompiledText` ainda ignora as cores do estilo.
- Substituído pelos mixins da Fase 2B, que agem no ponto de desenho.

## 7. WaterFrames 2.1.23 / CreativeCore 2.13.46 conferidos no bytecode

**Telas** (`me.srrapero720.waterframes.common.screens`):

| Classe | O que foi confirmado |
|---|---|
| `DisplayScreen`, `PlayListScreen`, `RemoteControlScreen` | sobrescrevem `getStyle` (`ScreenStyles.DISPLAYS`/`REMOTE_CONTROL`), `getBorder` (`SCREEN_BORDER`), `getBackground` (`SCREEN_BACKGROUND`); `tick` usa `ChatFormatting.AQUA/GREEN/RED` (Display) e `AQUA` (PlayList) |
| `RemoteControlScreen$1` / `$2` | botões com `RED_*` / `BLUE_*` |
| `styles.ScreenStyles` | campos `public static final`; `DISPLAYS`/`REMOTE_CONTROL` com `borderThickWidth = 6` |
| `widgets.WidgetURLTextField` | `getBorder` `BLUE_BORDER`/`RED_BORDER` (erro), `getBackground` `DARK_BLUE_BACKGROUND`, tooltip `BLUE`/`RED` |
| `widgets.WidgetPlaylistEntry` | `getBackground` `DARK_BLUE_HIGHLIGHT`/`DARK_BLUE_BACKGROUND` |
| `widgets.WidgetStatusIcon` | tooltip de status `AQUA/GREEN/YELLOW/RED/DARK_RED` |
| `widgets.WidgetClickableArea` | tooltip `AQUA` |

**Atlas de ícones:** `waterframes:textures/screen_atlas.png`, grade 16 px. Células de erro que continuam vermelhas: `STATUS_ERROR (4,1)`, `INTERNAL_ERROR (3,4)`, `INTERNAL_ERROR_2 (4,4)`.

**CreativeCore** (`team.creative.creativecore.common.gui`):

| Ponto | Confirmado |
|---|---|
| `style.display.StyleDisplay` | **classe abstrata**, `render(GuiGraphics, double, double, double, double)`, `StyleDisplay.NONE` |
| `GuiControl.render` | borda no retângulo inteiro → fundo no retângulo encolhido pela largura da borda → conteúdo |
| `ControlFormatting` | `GUI(BIG,5,BACKGROUND)`, `CLICKABLE(SMALL,2,CLICKABLE)`, `NESTED(SMALL,2,NESTED_BACKGROUND)`, `SLOT(NONE,0,SLOT)`, `TRANSPARENT(NONE,0,NONE)` |
| `GuiStyle.getBorder` | `BIG` → `borderThick`/`borderThickWidth`; `SMALL` → `border`/`borderWidth` |
| `GuiControl.getStyle()` | delega ao pai; `getBorder`/`getBackground` devolvem o parâmetro por padrão |
| `util.text.CompiledText` | cor (`defaultColor = -1` → branco) e sombra (`true`) próprias; ignora o `GuiStyle` |
| `GuiCheckBox` | caixa **7×7** (borda SMALL + miolo 5×5 `NESTED_BACKGROUND` + "x" com `fontColorHighlight`) |
| `GuiSlider` | alça = `style.get(CLICKABLE, false)`; texto branco com sombra |
| `GuiScrollY` | polegar = `style.get(CLICKABLE, false)` |
| `GuiTextfield` | estende `GuiFocusControl` (`isFocused()`) |

**Sprites vanilla 1.21.1 confirmados:** `widget/button`, `button_highlighted`, `button_disabled`, `slider`, `slider_highlighted`, `slider_handle(_highlighted)`, `text_field(_highlighted)`, `scroller`, `scroller_background`, `checkbox*` (20×20, sem nine-slice).

## 8. Texturas

| Arquivo | Caminho sobrescrito | Origem |
|---|---|---|
| `frame.png` | `assets/waterframes/textures/block/frame.png` | `waterframes-skin/pack-frame-classico` (face já desinvertida) |
| `displays.png` | `assets/waterframes/textures/block/displays.png` | idem |

## 9. Fora de escopo: não migrar

`datapack-so-frame` remove as receitas de `big_tv`, `projector`, `remote`, `tv`, `tv_box` (`data/waterframes/recipe/*.json`). Continua sendo datapack do servidor; **não entra no NexusCore**.

## 10. Checklist de regressão (funcionalidades que não podem mudar)

**Sodium (`compat.sodium`)**
- [ ] Vídeo → abre a tela YACL com as categorias General, Quality, Performance, Advanced
- [ ] Categorias extras aparecem com os mods instalados: Sodium Extra, More Culling, Iris, LambDynamicLights, Entity View Distance
- [ ] Botão "Open Original Screen ➔" com a seta correta (sem "åž")
- [ ] Mudar uma opção, salvar, reabrir: valor mantido
- [ ] Categoria do XSO: `enabled` desligado devolve a tela original do Sodium
- [ ] Navegação por teclado (Tab, setas, Enter no reset)
- [ ] Config antiga do XSO migrada

**WaterFrames (`compat.waterframes`)**
- [ ] Frame: URL, status, posição 3×3, sliders (volume, alcance, brilho...), checkboxes, botões salvar/playlist
- [ ] Playlist: adicionar, remover, ordenar entradas
- [ ] YouTube tocando (WaterMedia 2.1.37 + plugin)
- [ ] Ícone de status vermelho só em erro
- [ ] Textura de madeira do frame no mundo, face virada para o jogador

## Pendente

- [ ] Prints de referência das telas do WaterFrames (antes/depois) e da tela de vídeo do Sodium, tirados no teste em jogo
