<a href="https://www.curseforge.com/minecraft/mc-mods/vaulthunters-discord-bot-connector"><img src="http://cf.way2muchnoise.eu/839602.svg" alt="CF"></a>

# Vault Hunters integration with Discord

This mod hooks with discord bots and adds Vault Hunter related things in them.

### Supported Mods
- [Discord Integration for Forge](https://www.curseforge.com/minecraft/mc-mods/dcintegration)
- [MC2Discord](https://www.curseforge.com/minecraft/mc-mods/mc2discord)

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

**1.11.0** - Adds Discord tooltips for Create mod filter and attribute filter items, ported from BONNe's 1.10.0 release, active only when Create is loaded so it stays silent on networks without it; adds the MasuChatLink integration, which attaches the same Vault Hunters item tooltip text to chat messages relayed through MasuChatLink instead of only through MC2Discord; and carries forward the tooltip fixes already made since 1.9.3 (DeckSocket and time-string crashes, correct handling of non-VH and unmatched items, deck core stats, Sigil crate tiers, Greedy Meal/Herald Trophy/Companion/Attribute Filter tooltips, and the Treasure Keyring tooltip).
