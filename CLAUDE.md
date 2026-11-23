# Vault Hunters Discord Integrator

## Project Overview

Forge 1.18.2 server-side mod that bridges Vault Hunters items and events to Discord through optional bot mods: **DCIntegration**, **MC2Discord**, **SDLink** (mixins, loaded only when that bot is present) and **MasuChatLink** (a plain Forge listener on its `ChatRelayEvent`, registered only when the mod is present). On the MasuCraft networks only the MasuChatLink path is live: a linked item gets Discord text plus the lines and slots of a picture of its in-game tooltip.

## Build

```bash
./gradlew build        # Output: build/libs/vhdiscord-<version>.jar; runs the JUnit tests; -Xlint:all -Werror
./gradlew clean build  # Clean build (recommended when dependency issues occur)
```

- Java 17 required
- Forge 40.2.9 / Minecraft 1.18.2
- Vault Hunters is not on a public Maven: `-PvaultJarPath=<path>/the_vault-<version>.jar` (default `/mnt/data/AI/docs/minecraft/artifacts/forge/1.18.2/the_vault-1.18.2-22.0.0-remastered.7209.jar`); the build resolves it through a flatDir on the jar's folder, so the file must be named `the_vault-<version>.jar`.
- MasuChatLink is not on CurseMaven: `-Pmasuchatlink_jar=<path>` (default this machine's `MasuChatLink/build/libs/masuchatlink-1.3.0.jar`).
- `gradlew` is not executable in git; run it with `sh ./gradlew` in a fresh checkout or worktree.

## Architecture

### Package Structure

- `lv.id.bonne.vhdiscord` - `VaultHuntersDiscordMod`: registers the MasuChatLink listener when that mod is loaded and sets the parser's deck modifier writer (`DeckModifierTextWriter.use(DeckTooltips::appendModifierText)`)
- `lv.id.bonne.vhdiscord.config` - Mixin config plugins for conditional loading
- `lv.id.bonne.vhdiscord.masuchatlink` - `MasuChatLinkEventListener` (entry for linked items: parses the hover NBT, which comes from the player's client and is untrusted), `MarkdownTooltipLines` (description to picture lines), `DeckPicture` (deck screen)
- `lv.id.bonne.vhdiscord.parser` - item text, see below
- `lv.id.bonne.vhdiscord.dcintegration`, `.mc2discord`, `.sdlink` - upstream bot mixins and configs (not loaded on our servers; left as upstream wrote them)
- `lv.id.bonne.vhdiscord.vaulthunters` - `DeckTextGreedTier` and `DeckModifierTextWriter` (state the deck mixin reads, kept here so `vaulthunters.mixin` never imports `parser`), `VaultHuntersModConfiguration`; `.mixin` - VH accessor mixins and the server-only text mixins

Package dependencies run one way: root -> `masuchatlink` -> `parser` -> `vaulthunters` (`.mixin` -> `vaulthunters`).

### Mixin Configs

Four mixin JSON files in `src/main/resources/`:
- `mixins.vhdiscord.dcintegration.json` - 7 mixins, conditional on dcintegration mod
- `mixins.vhdiscord.mc2discord.json` - 1 mixin, conditional on mc2discord (excludes v3.x)
- `mixins.vhdiscord.sdlink.json` - 7 mixins, conditional on sdlink mod
- `mixins.vhdiscord.vaulthunters.json` - always loaded, server only: 2 accessor mixins, plus `MixinCardDeck`, `MixinDeckModifier` and `MixinDeckModifierShiftKey`, which let VH's client-side deck tooltip (and Wold's Vaults' deck modifiers, `@Pseudo`) run on a dedicated server for `parser/DeckTooltips`, and `MixinVaultGearModifierReader` and `MixinVaultCharmItem`, which answer `applyAccessibilityColour` and the god charm name as the default options do (since 7209 `ModOptions` cannot initialise on a dedicated server; without the charm one every server lookup of an identified god charm's name throws, Quark links included). A new deck modifier class that asks `Screen.hasShiftDown()` belongs in `MixinDeckModifierShiftKey`; until then it is written by name.

### Item text (`parser`)

`VaultItemsHandler` holds the public entry points the bot integrations call (`generateVaultHuntersItemTooltips` overloads, `clientEventLines`, `playerTitleOf`), the client-event lines (Curios slot, Soul Value, tooltip config) and `describeItem`, the dispatcher (an `instanceof` chain, then an item id `switch`). The handlers live in one class per item family: `GearTooltips` (gear, god charms, uses, affixes, etchings, inscriptions), `CrystalTooltips` (crystals, catalysts, augments, sigils), `CardTooltips` (cards, decks, deck sockets), `BottleTooltips` (potions), `CompanionTooltips` (companions, relics, herald trophy), `MiscItemTooltips` (trinkets, charms, flasks, focuses, title scrolls, soul flames, pouches, Greedy Meal and Charm, Temporal Shard, keyring, doll, antique, attribute filter). `TooltipLines` has the shared line writers and colours, `TooltipLimits` the caps for text built from NBT numbers, `HeartRow` the picture's heart row, `LegacyVaultItemApi` the version checks.

When adding support for a new item type:
1. Add a branch in `describeItem`: an `instanceof` for a class both Vault Hunters versions have, otherwise a `case` in the id `switch` (an `instanceof` against a class missing on Wold's Vaults throws for every item there).
2. Add a `handle<ItemType>Tooltip(StringBuilder, ItemStack[, ServerPlayer])` in the family class it belongs to.
3. Write every line through `TooltipLines.appendGameLine(builder, Component)` (`**Label:** value` markdown), `appendTextLine` or `appendStyledLine(builder, markdown, Component)`, never plain `builder.append`: they record the line's styled runs, and `MarkdownTooltipLines` draws a recorded line exactly in MasuChatLink's in-game picture (the recorded runs replace the markdown line, so a picture line can differ from its Discord text, as the potion's heart row does). Unrecorded lines come out white.
4. Prefer calling the game's own text code when it is server-safe (no `@OnlyIn(CLIENT)`, `Screen`, `Minecraft`, `ClientScheduler`, `Client*Data`, `ModOptions`, `VaultBarOverlay`, `UIHelper`), guarded like `crystalGameLines` (fallback, logged once per class); otherwise rebuild the same Components with the colours from the decompiled game source.
5. The mod runs on Vault Hunters Remastered (Skyvault) and Wold's Vaults: a Wold's-only layout is chosen with `ModList.get().isLoaded(TooltipLines.WOLDS_VAULTS_MOD_ID)` and Wold's classes are only reached by reflection. It compiles against Remastered 7209; item API that differs between 7209 and Wold's Vaults (VH 3.21) is checked with a `LegacyVaultItemApi.has...()` method before the version-specific path (old potion bottles, soul flame owner, shard pouch stack, tierless deck attributes, fixed god charm values, plain etching lines, unranged Greedy Meal, theme series, temporal modifier lock); API added after 7209 the same way (`hasActivePotion`: 7226's active/spare potion, read through `ActivePotion.of` and `carried`). Values the game works out for the viewer (potion tier, charges and effects, charm reputation, greed tier, vault level, prestige powers) are read for the linking player passed to `generateVaultHuntersItemTooltips(..., ServerPlayer)`, which may be null.
6. Numbers read from the item's NBT must not size text: use `TooltipLimits.repeat` and the deck checks.
7. Write only the item's own lines: `generateVaultHuntersItemTooltips` adds the lines client tooltip events add in game (Curios `Slot:` line, VH's Soul Value from `vault_diffuser`, VH's `tooltip` config description and blank line) through `withClientEventLines`; items this handler does not describe get them above MasuChatLink's server lines in `MasuChatLinkEventListener`.
8. Check a change with the golden link probe (throwaway, never committed): `/mnt/data/AI/work/vhdiscord-r12-impl-2026-11-22/probe/run-golden.sh <skyvault|wolds> <out.txt> [git-ref]` builds the tree (or a commit) with `GoldenLinkProbe` and runs about 1,700 (7209, local `Skyvault-4.0.0` server with the VH-Remastered MAIN player data) or 2,300 (Wold's Vaults 0.34.2 with the Wolds HUB player data) real items through MasuChatLink's describe and this listener, one line per item; diff two outputs. The server's mod folder is restored afterwards. Wold's Vaults rainbow names depend on the clock and differ between runs; Wolds sometimes hangs on a cold start (the script exits 4, rerun it).

### Pictures

The listener attaches a `ChatRelayItem` with lines, slots and deck; the MasuCraftBot renders it through VHArmory's `/api/render/tooltip/`. A Vault Pouch's slots use the icons `coin_bronze`, `coin_silver`, `coin_gold`, `coin_platinum`, `coin_greed`, `soul_dust`, `soul_shard`, `ember`: VHArmory refuses the whole picture for an icon it does not know, so a new icon ships in VHArmory first. A segment made only of U+2665/U+2661 is drawn as the game's heart sprites.

## Dependencies

### Vault Hunters Version

- Compiles against Vault Hunters Remastered 7209 (Skyvault Update 4), see Build.
- Decompiled 7209 source: `git show new:iskallia/vault/<path>` in `/mnt/data/Projects/skyvault-update4-2026-11-20/2-jar-diff/source-diff` (tag `old` = 6956).
- Wold's Vaults runs Vault Hunters 3.21.6 (`/mnt/data/Minecraft/Servers/Wolds/servers/ZEST/mods/the_vault-1.18.2-3.21.6.6884.jar`): check a built jar against it with `~/.claude/tools/jar-link-check --package iskallia/vault/ --target <that jar> build/libs/vhdiscord-<version>.jar`; only the 7209 paths behind `LegacyVaultItemApi` and the id `switch` may be missing there.

Always verify methods exist in the actual compiled dependency before using them.

### Dependency Versions (gradle.properties)

| Dependency | CurseForge File ID |
|---|---|
| Vault Hunters | Remastered 7209 local jar (`-PvaultJarPath`) |
| DCIntegration | 4775508 |
| MC2Discord | 4769533 |
| SDLink | 4777012 |
| Curios | 4418032 (hardcoded in build.gradle) |
| Create | 4835188 (compileOnly, the Wolds build) |

## Tests

`src/test` (JUnit 5): `TooltipLimitsTest`, `HeartRowTest`. Classes that touch Vault Hunters need its bootstrap and are checked with the golden probe instead.

## Conventions

- Upstream style: Allman braces, the `lv.id.bonne.vhdiscord` package, a `// Created by <author>` header (Masuary on the files this fork added).
- Suppress deprecation warnings from VH APIs with `@SuppressWarnings("deprecation")` on the method level - these are VH-side deprecations we cannot avoid
- Use `@SuppressWarnings({"rawtypes", "unchecked"})` (comma-separated array syntax) for raw type casts required by VH's generic gear attribute system
- Failures are logged once per item class (`VaultItemsHandler.GAME_TEXT_FAILURES`) or item id (listener), never a stack trace per link.
- Server-side only mod - all mixins target server side
