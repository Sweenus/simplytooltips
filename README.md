# Simply Tooltips: Data-Driven Theming Guide


If you are building your own theme pack, this README documents every data-driven surface currently available, including all built-in preset themes.

## Quick Reference

### Paths

- Themes: `assets/simplytooltips/themes/*.json`
- Item mappings: `assets/simplytooltips/item_themes/*.json`
- Lang: `assets/simplytooltips/lang/<locale>.json`

### Theme JSON Fields

- Color keys (hex `0xAARRGGBB`):
  - `border`, `borderInner`, `bgTop`, `bgBottom`, `name`, `badgeBg`, `badgeCutout`, `sectionHeader`, `body`, `separator`, `diamondFrame`, `diamondFrameInner`, `footerDot`, `stringColor`, `frameColor`, `runeColor`, `slotFilled`, `slotEmpty`, `hint`
- Behavior keys:
  - `motif`: `none`, `vine`, `ember`, `enchanted`, `bee`, `blossom`, `bubble`, `earth`, `echo`, `ice`, `lightning`, `autumn`, `soul`, `deepdark`, `poison`, `ocean`, `rustic`, `honey`, `jade`, `wood`, `stone`, `iron`, `gold`, `diamond`, `netherite`, `runic`
  - `itemAnimStyle`: `breathe_spin_bob`, `spin`, `bob`, `breathe`, `static`
  - `titleAnimStyle`: `wave`, `shimmer`, `pulse`, `flicker`, `shiver`, `quiver`, `breathe_spin_bob`, `drop_bounce`, `hinge_fall`, `obfuscate`, `static` (also accepts `shivering`)
  - `itemBorderShape`: `diamond`, `square`, `circle`, `cross`, `none`
  - `customTextKeys`: string array

### Item Mapping JSON Forms

```json
{
  "items": {
    "minecraft:iron_sword": "lightning",
    "minecraft:netherite_sword": { "theme": "lightning", "badges": ["SWORD", "NETHERITE"] }
  },
  "tags": [
    { "tag": "minecraft:swords", "theme": "lightning", "badges": ["SWORD"] }
  ]
}
```

### Theme Assignment Priority

1. Exact item mapping (`items`)
2. Provider `themeKey`
3. First matching tag mapping (`tags`)
4. Rarity theme (`rarity_common`, `rarity_uncommon`, `rarity_rare`, `rarity_epic`)
5. Internal fallback definition

### Presets

Preset themes and their motif and title animation styles.

| Theme key | Motif | Title animation |
|---|---|---|
| `autumn` | `autumn` | `wave` |
| `bee` | `bee` | `wave` |
| `blossom` | `blossom` | `wave` |
| `bubble` | `bubble` | `wave` |
| `deepdark` | `deepdark` | `pulse` |
| `default` | `none` | `static` |
| `diamond` | `diamond` | `static` |
| `earth` | `earth` | `wave` |
| `echo` | `echo` | `wave` |
| `ember` | `ember` | `pulse` |
| `enchanted` | `enchanted` | `wave` |
| `gold` | `gold` | `static` |
| `honey` | `honey` | `wave` |
| `ice` | `ice` | `wave` |
| `iron` | `iron` | `static` |
| `jade` | `jade` | `wave` |
| `lightning` | `lightning` | `wave` |
| `netherite` | `netherite` | `static` |
| `obfuscated` | `runic` | `obfuscate` |
| `ocean` | `ocean` | `wave` |
| `poison` | `poison` | `wave` |
| `rarity_common` | `none` | `static` |
| `rarity_epic` | `none` | `wave` |
| `rarity_rare` | `none` | `wave` |
| `rarity_uncommon` | `none` | `static` |
| `runic` | `runic` | `static` |
| `rustic` | `rustic` | `static` |
| `soul` | `soul` | `wave` |
| `stone` | `stone` | `static` |
| `unstable` | `autumn` | `hinge_fall` |
| `vine` | `vine` | `wave` |
| `wood` | `wood` | `static` |

## Data-Driven Surfaces

Simply Tooltips currently exposes these developer-facing data surfaces:

1. Theme definitions (`assets/simplytooltips/themes/*.json`)
2. Item and tag theme mappings (`assets/simplytooltips/item_themes/*.json`)
3. Badge overrides (inside `item_themes/*.json`)
4. Localization keys (`assets/simplytooltips/lang/*.json`)
5. Runtime config toggles and layout values (fzzy_config-backed client config)

All resource-pack JSON must use namespace `simplytooltips` to be discovered by the built-in loaders.

## Where Files Live

- Built-in themes: `common/src/main/resources/assets/simplytooltips/themes`
- Built-in item mappings: `common/src/main/resources/assets/simplytooltips/item_themes`
- Built-in lang file: `common/src/main/resources/assets/simplytooltips/lang/en_us.json`
- Config schema source: `common/src/main/java/net/sweenus/simplytooltips/config/SimplyTooltipsConfig.java`

## Theme Definition (`themes/*.json`)

Each file name is the `theme key` (for example, `vine.json` -> `vine`).

### JSON Schema

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

### Color Field Notes

- Format: `0xAARRGGBB` string
- Missing color field: falls back to built-in default palette value
- Invalid hex value: falls back to built-in default palette value for that field

### Behavior Field Notes

- `motif`: background motif + border style selection
- `itemAnimStyle`: animation for header item icon
- `titleAnimStyle`: animation for tooltip title text
- `itemBorderShape`: icon frame shape
- `customTextKeys`: accepted/loaded list of translation keys

### Valid Values

`motif` values:

- `none`
- `vine`, `ember`, `enchanted`, `bee`, `blossom`, `bubble`, `earth`, `echo`, `ice`, `lightning`, `autumn`, `soul`, `deepdark`, `poison`, `ocean`, `rustic`, `honey`, `jade`, `wood`, `stone`, `iron`, `gold`, `diamond`, `netherite`, `runic`

`itemAnimStyle` values:

- `breathe_spin_bob` (default)
- `spin`
- `bob`
- `breathe`
- `static`

`titleAnimStyle` values:

- `wave` (default)
- `shimmer`
- `pulse`
- `flicker`
- `shiver`
- `quiver`
- `breathe_spin_bob`
- `drop_bounce`
- `hinge_fall`
- `obfuscate`
- `static`

`itemBorderShape` values:

- `diamond` (default)
- `square`
- `circle`
- `cross`
- `none`

### Unknown or Missing Value Behavior

- Unknown `motif`: motif layer is skipped and border style falls back to default border pattern
- Unknown `itemAnimStyle`: behaves as `breathe_spin_bob`
- Unknown `titleAnimStyle`: behaves as `wave`
- Unknown `itemBorderShape`: behaves as `diamond`

## Item and Tag Mapping (`item_themes/*.json`)

These files map item IDs and tag IDs to theme keys and badge labels.

### JSON Schema

```json
{
  "items": {
    "minecraft:iron_sword": "lightning",
    "minecraft:netherite_sword": {
      "theme": "lightning",
      "badges": ["SWORD", "NETHERITE"]
    }
  },
  "tags": [
    {
      "tag": "minecraft:swords",
      "theme": "lightning",
      "badges": ["SWORD"]
    },
    {
      "tag": "c:ingots",
      "badges": ["INGOT"]
    }
  ]
}
```

### Mapping Rules

- `items.<id>` can be:
  - a string (theme key only)
  - an object with optional `theme` and optional `badges`
- `tags[]` entries require `tag`; `theme` and `badges` are optional
- invalid item IDs or tag IDs are ignored
- `badges` must be a non-empty string array to apply

### Multi-File Merge Behavior

For multiple files in `assets/simplytooltips/item_themes/`:

- `items` mappings are merged; later entries overwrite prior entries for the same item ID
- `tags` entries are appended; first matching tag entry wins at runtime

## Runtime Theme Resolution Order

For a given stack, theme resolution follows this order:

1. Exact item mapping from `item_themes` (`items`)
2. Provider-supplied `themeKey` (if present)
3. First matching tag mapping from `item_themes` (`tags`)
4. Rarity theme (`rarity_common`, `rarity_uncommon`, `rarity_rare`, `rarity_epic`)
5. Registry fallback (`ThemeDefinition.defaultDefinition()`)

Badge resolution order:

1. Exact item badge override
2. First matching tag badge override
3. Provider/default badges

Render gating behavior:

- Vanilla items render only when `applyTooltipsToVanillaItems=true`
- Modded items render when either:
  - `applyTooltipsToModItems=true`, or
  - item/tag mapping exists in `item_themes`

## Built-In Item Mapping Preset

- Built-in file: `common/src/main/resources/assets/simplytooltips/item_themes/defaults.json`
- Includes:
  - Vanilla material set mappings (`wood`, `stone`, `iron`, `gold`, `diamond`, `netherite`)
  - Rarity/unique-style mappings for supported mod content
  - Tag-based badge defaults (for common tag groups)

Use this file as a reference for large mapping packs.
