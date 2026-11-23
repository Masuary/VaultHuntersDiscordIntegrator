# Vault Hunters Discord Integrator (fork, branch masuchatlink): plan

## VH Remastered 4.0.1 (the_vault 7226) potion tooltip: Skyvault Update 4 phase R2, 2026-11-23

Sir chose to update the potion link tooltip now (project `PLAN.md` "Phase R2", `/mnt/data/Projects/skyvault-update4-2026-11-20/`). Version stays 1.14.0 (not shipped); still compiles against 7209.
- [x] `BottleTooltips.appendUseLines`: on 7226 and later (`ActivePotion.of(Player)` found by reflection) "Kept when you die", the game's "Active potion" / "Spare: ..." line when the linking player carries more than one potion (the carried stack with the same item and tag stands for the linked copy), and "Right-click to drink, Sneak to change the Drink Mode" + "Right-click it in your inventory to equip it" for a potion outside the slot; 7209 keeps its two lines; Wold's Vaults keeps the typed-bottle path.
- [x] `gradlew build` clean; `jar-link-check` against 7226 reports the same JDK/Forge-inherited members as against 7209.
- [ ] 4.0.1 testnet: link a slotted potion, a second carried potion (Spare line) and one in the hand; text matches the linking player's own tooltip.

## 1.14.0: Vault Hunters Remastered 7209 (Skyvault Update 4) - Done (code), in-game tests pending

Started and code-complete 2026-11-20 (commit 97f1a28). Source: `/mnt/data/Projects/skyvault-update4-2026-11-20/2-jar-diff/reports/our-mods-impact.md`, section `vhdiscord-1.13.2.jar`; network plan `/mnt/data/Projects/skyvault-update4-2026-11-20/PLAN.md` (Phase A step 4). Check outputs: `/mnt/data/AI/work/vhdiscord-7209-port-2026-11-20/`.

- [x] Version 1.13.2 -> 1.14.0; compiles against `the_vault-1.18.2-22.0.0-remastered.7209.jar` (local jar in `libs/`, not committed). MasuChatLink default jar path is now 1.3.0.
- [x] Potion handler on the 7209 bottle API (`BottleItem.getCharges/getMaxCharges/getDrinkMode/getSlotEffects/getInfused/getFill/getBoundVault`, `PotionTiers.tierOf` -> `PotionConfig.Tier`, `PotionRolls`, `PotionEffectDisplay`), read for the linking player (`ChatRelayEvent.getPlayer()`); the viewer's key bind line is left out.
- [x] Soul flame handler: "Turns into N Embers when carried" (`getEmberValue`).
- [x] Shard pouch handler: `getStoredShardCount` count line plus "Becomes a Vault Pouch in your inventory".
- [x] Deck: `CardDeck.getSnapshotAttributes(int)` with the linking player's greed tier (`PlayerGreedTreeData`); the deck tooltip text gets the same tier through a `MixinCardDeck` redirect of `ClientGreedTreeData.getGreedTier()`.
- [x] `MixinCardDeck`: full selectors `addText(Ljava/util/List;ILnet/minecraft/world/item/TooltipFlag;FZ)V` (7209) plus the 4-argument one (Wold's Vaults), checked with javap; the `gearColours` redirect that never matched is gone.
- [x] Wold's Vaults (VH 3.21.6) keeps working: the removed pre-7209 APIs are reached by reflection in `parser/LegacyVaultItemApi`. `SkillAltarContainer$Default` (gone in 7209) is now a `@Pseudo` string target in the plugin-gated dcintegration/sdlink mixins.
- [x] Checks: `gradlew build` clean (no compiler warnings, no tests in this repo); `jar-link-check` against 7209: only members inherited from JDK/Forge or resolved through VH supertypes, all confirmed with javap; against the Wolds 6884 jar: only the 7209 paths behind `LegacyVaultItemApi`; client-only check: only `DeckModifier.addText`, which `MixinDeckModifier` restores (as before).

### Phase C in-game link tests (VH-Remastered testnet on 7209, MasuChatLink relay, check Discord text and the in-game picture)
- [x] (review 12 TestA pass 2026-11-23) Potion: link a vault potion with slotted effects (and one infused); charges, drink mode, heals and effect lines match the linking player's own tooltip. Also a potion linked inside a vault (fill line).
- [ ] Soul flame: ember count matches the in-game tooltip.
- [ ] Shard pouch: soul shard count and the vault pouch line.
- [x] (review 12 TestA pass 2026-11-23) Socketed deck: attributes match the linking player's in-game deck tooltip at their greed tier (test with a player above greed tier 0), sockets line and core modifier lines present, no `Screen`/client class errors in the server log.
- [x] (review 12 golden probe on Wold's Vaults 0.34.2, 2,355 real items) Wold's Vaults: one linked potion, soul flame, shard pouch and deck on the Wolds HUB test copy (`/mnt/data/AI/work/masuchatlink-crystal-tooltip-2026-11-19/run-probe.sh`), to prove the reflection paths and that `VaultItemsHandler` links on VH 3.21.6.

## Phase R review 12 (Skyvault Update 4): decided by sir 2026-11-22

Findings and the full answers: `/mnt/data/Projects/skyvault-update4-2026-11-20/9-mod-review/findings/12-vhdiscord.md`. Version stays 1.14.0 (not shipped).

- [x] S4 build: VH through `-PvaultJarPath` (default the shared 7209 artifacts jar), no `libs/` flatDir, MDK comments trimmed.
- [x] S3 tests: JUnit 5 set up; B1 cap tests first, then `MarkdownTooltipLines`, `AffixCategoryStyle`, `TooltipColorHints`, time formatting.
- [x] B1: capped repeats (companion hearts, repair dots, crystal curses), deck text skipped for absurd socket/card counts, oversized descriptions dropped.
- [x] S1 tidy: imports, dead code (paxel/artifact handlers, unregistered mc2discord `MixinPlayerList`, MDK stub, unused gradle property, stale comment), docs (CLAUDE.md counts, README changelog to 1.14.0), author headers on our own files.
- [x] S2 split of `VaultItemsHandler` into family classes with one line helper, public entry points kept, `parser <-> vaulthunters` cycle broken; descriptions identical before and after on 7209 and Wolds.
- [x] B2 god charm 7209 layout for the linking player (Wolds keeps the old one); B3 Vault Pouch from `getContainedStacks` with all eight contents, owner must be the linker; B4 charm slot, etching, greedy meal, potion lines aligned with 7209; B5 warn-once logging.
- [x] F2 handlers for the new 7209 items (stash, bounty crate, looks and unlock items); F3 sigil tiers for the linker's vault level; F4 potion title = tier name, slotted-bottle line, heals drawn as hearts.
- [x] VHArmory: slot icons soul dust, soul shard, ember and the heart sprites (VHArmory master cf6c6c6; push + deploy only with sir's go, before this jar goes live).
- [x] Independent review (Opus, 0C 0H 0M 4L, fixed 856a8b2); testnet + CMA link pass with the temporary Redis on TestA, all PASS, one fix (41d010f): `/mnt/data/AI/work/vhdiscord-r12-impl-2026-11-22/qa/testnet-results.md`. Also done: identified god charm names no longer throw on a 7209 server (f368da1, sir 2026-11-22). Open: VHArmory deploy (needs a fresh panel key) before this jar goes live.

### Left open
- dcintegration/sdlink only (not loaded on our servers): `MixinAchievementCompleteTask`'s `PlayerList.broadcastMessage` call is gone from `AchievementCompleteTask.onConsume` in 7209 and the skill altar share moved to `SkillAltarContainer.share`; those Discord announcements do not fire on 7209.
- New 7209 item types (stash, looks, unlock items) have no handler (cosmetic). The potion picture title is the item name, not the tier name the client shows.
