<a href="https://www.curseforge.com/minecraft/mc-mods/vaulthunters-discord-bot-connector"><img src="http://cf.way2muchnoise.eu/839602.svg" alt="CF"></a>

# Vault Hunters integration with Discord

This mod hooks with discord bots and adds Vault Hunter related things in them.

### Supported Mods
- [Discord Integration for Forge](https://www.curseforge.com/minecraft/mc-mods/dcintegration)
- [MC2Discord](https://www.curseforge.com/minecraft/mc-mods/mc2discord)
- [Simple Discord Link Bot](https://www.curseforge.com/minecraft/mc-mods/simple-discord-link-bot-forge-fabric-spigot)
- MasuChatLink (MasuCraft's chat relay): linked items get the Discord text and a picture of the in-game tooltip

### Added Things:
- Messages when player enters/exits vault.
- The item sharing in chat with proper item tooltips:
  - Vault Gear Items (armor, sword, axe, shield, totems and magnets)
  - Vault Doll Item
  - Vault Tools
  - Etching Item
  - Vault Rune Item
  - Vault Crystal Item
  - Trinket Item
  - Relic Fragment Item
  - Vault Catalyst Infused Item

### Example Vault Sword
![vault_sword](https://user-images.githubusercontent.com/1344358/226194742-838d4a67-cc9a-497b-a37c-95f66c681cdc.png)

### Changelog

**1.14.0** - Vault Hunters Remastered 7209 (Skyvault Update 4) with Wold's Vaults (Vault Hunters 3.21) still supported by the same jar. Potions, soul flames, shard pouches and decks on the 7209 item API, read for the player who linked the item; god charms scaled by that player's reputation; all eight Vault Pouch contents (only the linker's own pouch); the 7209 charm slot, etching, Greedy Meal and potion lines; potion pictures titled with the player's potion tier and drawn with hearts; Greedy Charm, Temporal Shard and theme series augment text; sigil crate tiers for the linker's vault level; deck text no longer fails on the server. Linked items can no longer make the server build huge text from forged numbers, a failing item logs one warning instead of a stack trace per link, and the item handler is split into one class per item family with tests.

**1.13.x** - Card deck pictures with the deck screen and the game's own deck text, and the item pictures in the game's colours: gear, crystals, cards, companions, trinkets, charms, jewels, inscriptions, etchings, catalysts, pouches and focuses, with the lines client tooltip events add on Wold's Vaults.

**1.12.x** - Styled MasuChatLink tooltip lines; Vault Hunters items keep their own tooltip lines; Imbued, Sealed and Corrupted in the game's colours.

**1.11.0** - Adds Discord tooltips for Create mod filter and attribute filter items, ported from BONNe's 1.10.0 release, active only when Create is loaded so it stays silent on networks without it; adds the MasuChatLink integration, which attaches the same Vault Hunters item tooltip text to chat messages relayed through MasuChatLink instead of only through MC2Discord; and carries forward the tooltip fixes already made since 1.9.3 (DeckSocket and time-string crashes, correct handling of non-VH and unmatched items, deck core stats, Sigil crate tiers, Greedy Meal/Herald Trophy/Companion/Attribute Filter tooltips, and the Treasure Keyring tooltip).
