# Vault Hunters Discord Integrator

## Project Overview

Forge 1.18.2 mod that bridges Vault Hunters game events to Discord via three optional Discord bot mods: **DCIntegration**, **MC2Discord**, and **SDLink**. Uses Mixin bytecode manipulation with conditional loading based on which Discord mod is present.

## Build

```bash
./gradlew build        # Output: build/libs/vhdiscord-<version>.jar
./gradlew clean build  # Clean build (recommended when dependency issues occur)
```

- Java 17 required
- Forge 40.2.9 / Minecraft 1.18.2

## Architecture

### Package Structure

- `lv.id.bonne.vhdiscord` - Main mod entry point (`VaultHuntersDiscordMod`)
- `lv.id.bonne.vhdiscord.config` - Mixin config plugins for conditional loading
- `lv.id.bonne.vhdiscord.parser` - `VaultItemsHandler` — parses VH item tooltips into Discord-friendly text
- `lv.id.bonne.vhdiscord.dcintegration` - DCIntegration mixins and config
- `lv.id.bonne.vhdiscord.mc2discord` - MC2Discord mixins and config
- `lv.id.bonne.vhdiscord.sdlink` - SDLink mixins and config
- `lv.id.bonne.vhdiscord.vaulthunters` - VH accessor/invoker mixins (always loaded)

### Mixin Configs

Four mixin JSON files in `src/main/resources/`:
- `mixins.vhdiscord.dcintegration.json` - 8 mixins, conditional on dcintegration mod
- `mixins.vhdiscord.mc2discord.json` - 2 mixins, conditional on mc2discord (excludes v3.x)
- `mixins.vhdiscord.sdlink.json` - 8 mixins, conditional on sdlink mod
- `mixins.vhdiscord.vaulthunters.json` - always loaded: 2 accessor mixins, plus `MixinCardDeck`, `MixinDeckModifier` and `MixinDeckModifierShiftKey`, which let VH's client-side deck tooltip (and Wold's Vaults' deck modifiers, `@Pseudo`) run on a dedicated server for `parser/DeckTooltips`. A new deck modifier class that asks `Screen.hasShiftDown()` belongs in `MixinDeckModifierShiftKey`; until then it is written by name.

### Key File: VaultItemsHandler.java

Central tooltip parser in `parser/VaultItemsHandler.java`. Handles all VH item types (gear, crystals, cards, decks, trinkets, charms, bottles, relics, augments, inscriptions, etc.) and converts their tooltips to Discord markdown.

When adding support for a new item type:
1. Add an `instanceof` check in `generateVaultHuntersItemTooltips()`
2. Create a `handle<ItemType>Tooltip(StringBuilder, ItemStack)` method
3. Write every line through `appendGameLine(builder, Component)` (`**Label:** value` markdown) or `appendStyledLine(builder, markdown, Component)`, never plain `builder.append`: they record the line's styled runs, and `MarkdownTooltipLines` draws a recorded line exactly in MasuChatLink's in-game picture. Unrecorded lines come out white.
4. Prefer calling the game's own text code when it is server-safe (no `@OnlyIn(CLIENT)`, `Screen`, `Minecraft`, `ClientScheduler`, client data), guarded like `crystalGameLines` (fallback, logged once per class); otherwise rebuild the same Components with the colours from the decompiled game source.
5. The mod runs on Vault Hunters Remastered (Skyvault) and Wold's Vaults: a Wold's-only layout is chosen with `ModList.get().isLoaded(WOLDS_VAULTS_MOD_ID)` and Wold's classes are only reached by reflection. It compiles against Remastered 7209; item API that 7209 removed but Wold's Vaults (VH 3.21) still has (old potion bottles, soul flame owner, shard pouch stack, tierless deck attributes) is reached by reflection in `parser/LegacyVaultItemApi`, checked with its `has...()` methods before the 7209 path. Values the game works out per player (potion charges and effects, a deck's greed bonus) are read for the linking player passed to `generateVaultHuntersItemTooltips(..., ServerPlayer)`.
6. Write only the item's own lines: `generateVaultHuntersItemTooltips` adds the lines client tooltip events add in game (Curios `Slot:` line, VH's Soul Value from `vault_diffuser`, VH's `tooltip` config description and blank line) through `withClientEventLines`; items this handler does not describe get them above MasuChatLink's server lines in `MasuChatLinkEventListener`.
7. Check a change with a throwaway server probe on the Wolds HUB test copy, never committed: `/mnt/data/AI/work/masuchatlink-crystal-tooltip-2026-11-19/run-probe.sh` with `round3/RealItemProbe.java` runs every item in the world's saved inventories through the listener's path; `stop-probe.sh` restores the server.

## Dependencies

### Vault Hunters Version

- Project dependency: local jar `libs/the_vault-1.18.2-22.0.0-remastered.7209.jar` (Vault Hunters Remastered 7209, Skyvault Update 4; gitignored, resolved through the `libs` flatDir as `the_vault:the_vault:1.18.2-22.0.0-remastered.7209`). Copy it from `/mnt/data/Projects/skyvault-update4-2026-11-20/2-jar-diff/jars/new/`.
- Decompiled 7209 source: `git show new:iskallia/vault/<path>` in `/mnt/data/Projects/skyvault-update4-2026-11-20/2-jar-diff/source-diff` (tag `old` = 6956).
- Wold's Vaults runs Vault Hunters 3.21.6 (`/mnt/data/Minecraft/Servers/Wolds/servers/ZEST/mods/the_vault-1.18.2-3.21.6.6884.jar`): check a built jar against it with `~/.claude/tools/jar-link-check --package iskallia/vault/ --target <that jar> build/libs/vhdiscord-<version>.jar`; only the 7209 paths behind `LegacyVaultItemApi` may be missing there.

Always verify methods exist in the actual compiled dependency before using them.

### Dependency Versions (gradle.properties)

| Dependency | CurseForge File ID |
|---|---|
| Vault Hunters | Remastered 7209 local jar (hardcoded in build.gradle) |
| DCIntegration | 4775508 |
| MC2Discord | 4769533 |
| SDLink | 4777012 |
| Curios | 4418032 (hardcoded in build.gradle) |

## Conventions

- Suppress deprecation warnings from VH APIs with `@SuppressWarnings("deprecation")` on the method level — these are VH-side deprecations we cannot avoid
- Use `@SuppressWarnings({"rawtypes", "unchecked"})` (comma-separated array syntax) for raw type casts required by VH's generic gear attribute system
- Server-side only mod — all mixins target server side
