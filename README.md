# Simply Tooltips: Theme Quick Guide


## 1) Folder Layout

Put your files in a resource pack under the `simplytooltips` namespace.

```text
your_resource_pack/
  pack.mcmeta
  assets/
    simplytooltips/
      themes/
        my_theme.json
      item_themes/
        my_mappings.json
```

- `themes/*.json`: defines how a tooltip looks.
- `item_themes/*.json`: maps items/tags to theme keys.

## 2) Create a Theme

Create `assets/simplytooltips/themes/my_theme.json`.

Use this as a starter:

```json
{
  "border": "0xFFE2A834",
  "borderInner": "0xFF8A6A1E",
  "bgTop": "0xF02E2210",
  "bgBottom": "0xF0181208",
  "name": "0xFFFFF0CC",
  "badgeBg": "0xFFEEEEEE",
  "badgeCutout": "0xFF141008",
  "sectionHeader": "0xFFFFD5A0",
  "body": "0xFFE6ECF5",
  "separator": "0xFF8A6A1E",
  "diamondFrame": "0xFFE2A834",
  "diamondFrameInner": "0xFF2A1E0A",
  "footerDot": "0xFF8A6A1E",
  "stringColor": "0xFF9D62CA",
  "frameColor": "0xFF5E8ACF",
  "runeColor": "0xFFDB5E71",
  "slotFilled": "0xFFE2A834",
  "slotEmpty": "0xFF3D3020",
  "hint": "0xFFC7D2E2",
  "motif": "none",
  "itemAnimStyle": "breathe_spin_bob",
  "titleAnimStyle": "wave",
  "itemBorderShape": "diamond",
  "customTextKeys": []
}
```

Notes:
- File name is the theme key (`my_theme.json` -> `my_theme`).
- Color format is `0xAARRGGBB`.
- Missing or bad values fall back to defaults.

## 3) Assign Theme to Items

Create `assets/simplytooltips/item_themes/my_mappings.json`.

```json
{
  "items": {
    "minecraft:diamond_sword": "my_theme",
    "minecraft:netherite_sword": {
      "theme": "my_theme",
      "badges": ["SWORD", "CUSTOM"]
    }
  },
  "tags": [
    {
      "tag": "minecraft:swords",
      "theme": "my_theme",
      "badges": ["SWORD"]
    }
  ]
}
```

How it resolves:
- Exact item match in `items` wins.
- If no exact item match, first matching `tags` entry is used.

## 4) Available Preset Theme Elements

These are the built-in values you can reuse in your own theme JSON.

### `motif`

- `none`
- `vine`, `ember`, `enchanted`, `bee`, `blossom`, `bubble`, `earth`, `echo`, `ice`, `lightning`, `autumn`, `soul`, `deepdark`, `poison`, `ocean`, `rustic`, `honey`, `jade`, `wood`, `stone`, `iron`, `gold`, `diamond`, `netherite`, `runic`

### `itemAnimStyle`

- `breathe_spin_bob`
- `spin`
- `bob`
- `breathe`
- `static`

### `titleAnimStyle`

- `wave`
- `shimmer`
- `pulse`
- `flicker`
- `shiver` (also accepts `shivering`)
- `quiver`
- `breathe_spin_bob`
- `drop_bounce`
- `hinge_fall`
- `obfuscate`
- `static`

### `itemBorderShape`

- `diamond`
- `square`
- `circle`
- `cross`
- `none`

### `customTextKeys`

- Add translation keys (string array) in your theme JSON.
- These lines render below the Description section, with a separator line.

## 5) Built-In Preset Theme Keys

Built-in theme keys you can reference directly:

- `autumn`, `bee`, `blossom`, `bubble`, `deepdark`, `default`, `diamond`, `earth`, `echo`, `ember`, `enchanted`, `gold`, `honey`, `ice`, `iron`, `jade`, `lightning`, `netherite`, `obfuscated`, `ocean`, `poison`, `rarity_common`, `rarity_epic`, `rarity_rare`, `rarity_uncommon`, `runic`, `rustic`, `soul`, `stone`, `unstable`, `vine`, `wood`

## 6) Reload and Test

- Reload resources in-game (`F3 + T`), then hover items.
- If tooltips are not applying to your target items, check your client config flags:
  - `enableTooltipRendering`
  - `general.applyTooltipsToVanillaItems`
  - `general.applyTooltipsToModItems`

## 7) Addon Mod Integration: Simply Swords Compat

If your mod adds items that follow the Simply Swords tooltip structure, you can
opt them into the full Simply Swords rendering pipeline with a single data file.

**What tagged items receive:**
- **LORE tab** — ability description lines are separated from stats and rendered
  with a `◆ <AbilityName>` section header. The name is extracted automatically
  from the `Unique Effect: <Name>` line.
- **Action-label sub-headers** — lines such as `On Right Click:` are detected
  and rendered as `◆` sub-section headers with a visual separator above them.
- **STATS tab with stat bars** — the mainhand Attack Damage / Attack Speed /
  Attack Range block is pulled into the STATS tab and rendered as graphical
  progress bars.
- **Button-hint row** — the interactive hint line (info / search / config
  glyphs) is extracted and placed in the header area.

Badges and themes are **not** auto-detected by this tag. Assign them via
`assets/simplytooltips/item_themes/` as described in sections 2–3.

Create `data/simplytooltips/tags/item/simply_swords_compat.json` inside your
mod's resources:

```json
{
  "values": [
    "yourmod:your_unique_item",
    "#yourmod:your_uniques_tag"
  ]
}
```

> **Priority note:** `SimplySwordsCompatTooltipProvider` is registered at
> priority `1`. If your mod registers its own `TooltipProvider` at a higher
> priority for the same items, that provider takes precedence and this tag has
> no effect for those items.
