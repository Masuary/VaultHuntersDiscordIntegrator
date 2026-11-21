# Vault Hunters Discord Integrator (fork, branch masuchatlink): plan

## 1.14.0: Vault Hunters Remastered 7209 (Skyvault Update 4) - Done (code), in-game tests pending

Started and code-complete 2026-11-20 (uncommitted). Source: `/mnt/data/Projects/skyvault-update4-2026-11-20/2-jar-diff/reports/our-mods-impact.md`, section `vhdiscord-1.13.2.jar`; network plan `/mnt/data/Projects/skyvault-update4-2026-11-20/PLAN.md` (Phase A step 4). Check outputs: `/mnt/data/AI/work/vhdiscord-7209-port-2026-11-20/`.

- [x] Version 1.13.2 -> 1.14.0; compiles against `the_vault-1.18.2-22.0.0-remastered.7209.jar` (local jar in `libs/`, not committed). MasuChatLink default jar path is now 1.3.0.
- [x] Potion handler on the 7209 bottle API (`BottleItem.getCharges/getMaxCharges/getDrinkMode/getSlotEffects/getInfused/getFill/getBoundVault`, `PotionTiers.tierOf` -> `PotionConfig.Tier`, `PotionRolls`, `PotionEffectDisplay`), read for the linking player (`ChatRelayEvent.getPlayer()`); the viewer's key bind line is left out.
- [x] Soul flame handler: "Turns into N Embers when carried" (`getEmberValue`).
- [x] Shard pouch handler: `getStoredShardCount` count line plus "Becomes a Vault Pouch in your inventory".
- [x] Deck: `CardDeck.getSnapshotAttributes(int)` with the linking player's greed tier (`PlayerGreedTreeData`); the deck tooltip text gets the same tier through a `MixinCardDeck` redirect of `ClientGreedTreeData.getGreedTier()`.
- [x] `MixinCardDeck`: full selectors `addText(Ljava/util/List;ILnet/minecraft/world/item/TooltipFlag;FZ)V` (7209) plus the 4-argument one (Wold's Vaults), checked with javap; the `gearColours` redirect that never matched is gone.
- [x] Wold's Vaults (VH 3.21.6) keeps working: the removed pre-7209 APIs are reached by reflection in `parser/LegacyVaultItemApi`. `SkillAltarContainer$Default` (gone in 7209) is now a `@Pseudo` string target in the plugin-gated dcintegration/sdlink mixins.
- [x] Checks: `gradlew build` clean (no compiler warnings, no tests in this repo); `jar-link-check` against 7209: only members inherited from JDK/Forge or resolved through VH supertypes, all confirmed with javap; against the Wolds 6884 jar: only the 7209 paths behind `LegacyVaultItemApi`; client-only check: only `DeckModifier.addText`, which `MixinDeckModifier` restores (as before).

### Phase C in-game link tests (VH-Remastered testnet on 7209, MasuChatLink relay, check Discord text and the in-game picture)
- [ ] Potion: link a vault potion with slotted effects (and one infused); charges, drink mode, heals and effect lines match the linking player's own tooltip. Also a potion linked inside a vault (fill line).
- [ ] Soul flame: ember count matches the in-game tooltip.
- [ ] Shard pouch: soul shard count and the vault pouch line.
- [ ] Socketed deck: attributes match the linking player's in-game deck tooltip at their greed tier (test with a player above greed tier 0), sockets line and core modifier lines present, no `Screen`/client class errors in the server log.
- [ ] Wold's Vaults: one linked potion, soul flame, shard pouch and deck on the Wolds HUB test copy (`/mnt/data/AI/work/masuchatlink-crystal-tooltip-2026-11-19/run-probe.sh`), to prove the reflection paths and that `VaultItemsHandler` links on VH 3.21.6.

### Left open
- dcintegration/sdlink only (not loaded on our servers): `MixinAchievementCompleteTask`'s `PlayerList.broadcastMessage` call is gone from `AchievementCompleteTask.onConsume` in 7209 and the skill altar share moved to `SkillAltarContainer.share`; those Discord announcements do not fire on 7209.
- New 7209 item types (stash, looks, unlock items) have no handler (cosmetic). The potion picture title is the item name, not the tier name the client shows.
