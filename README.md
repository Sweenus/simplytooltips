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
      borders/
        my_border.json
```

- `themes/*.json`: defines how a tooltip looks.
- `item_themes/*.json`: maps items/tags to theme keys and border keys.
- `borders/*.json`: defines a border (pattern + colours). Optional — see section 5.

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
  "borderStyle": "none",
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
- `border` / `borderInner` are the frame **colors**. `borderStyle` picks the decorative
  **pattern** drawn on the frame. If you leave `borderStyle` out it follows `motif`, which
  is how borders worked before — existing themes are unaffected.

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
  "components": [
    {
      "component": "mod:rarity",
      "value": "mod:rare",
      "theme": "my_theme",
      "badges": ["RARE"]
    },
    {
      "component": "mod:charged=true",
      "theme": "my_theme"
    },
    {
      "component": "mod:has_socket",
      "badges": ["SOCKETED"]
    }
  ],
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
- First matching `components` entry with a theme wins.
- If no component theme matches, exact item matches in `items` win.
- If no exact item theme matches, provider-supplied themes are used.
- If no provider theme exists, first matching `tags` entry with a theme is used.
- If nothing matches, vanilla rarity fallback is used.

Badge overrides resolve separately:
- First matching `components` entry with badges wins.
- If none matches, exact item badges win.
- If none matches, first matching `tags` entry with badges wins.
- If nothing matches, provider/default badges are used.

Component entries use Minecraft data component IDs. Add `value` to match a specific
component value, omit `value` to match component presence only, or use shorthand
`"component": "namespace:component_id=namespace:value_id"`.

Any `items`, `components` or `tags` entry may also carry a `border` key. Borders resolve on
their own axis, so an entry can restyle just the frame and leave the theme alone:

```json
{
  "components": [
    { "component": "apotheosis:rarity=apotheosis:mythic", "border": "rarity_mythic", "badges": ["MYTHIC"] }
  ]
}
```

A mythic item matched this way keeps whatever theme its own item/tag mapping gives it and
only gains a mythic frame. Border overrides resolve component → item → tag, first match wins,
and they take precedence over the theme's own `borderStyle`.

## 4) Borders

A tooltip's frame is a **pattern** (the pixel art along the top and bottom lines) plus the
**colors** it is drawn in. You can use a built-in pattern as-is, or define your own border.

### Using a built-in

Reference any built-in pattern key directly — no extra files needed:

```json
{ "borderStyle": "runic" }
```

The pattern keys are the same as the motif keys listed in section 5, plus `none`.

### Defining your own

Create `assets/simplytooltips/borders/my_border.json`. The file name is the border key.

```json
{
  "pattern": "runic",
  "frame": "0xFF55FFFF",
  "frameInner": "0xFF00AAAA",
  "accentA": "0xFF88FFFF",
  "accentB": "0xFF00AAAA",
  "accentC": "0xFFCFFFFF"
}
```

- `pattern`: which pattern to draw. `none` (or omitted) draws a plain frame.
- `frame` / `frameInner`: the outline and inner highlight colors. Omit to use the theme's
  `border` / `borderInner`.
- `accentA`–`accentE`: the pattern's decoration colors. Each pattern uses as many as it needs
  and documents what they mean; omitted accents keep the pattern's own defaults.

Then reference it from a theme (`"borderStyle": "my_border"`) or from an `item_themes` entry
(`"border": "my_border"`).

### Which border wins

1. A `border` on a matching `item_themes` entry (component → item → tag).
2. The resolved theme's `borderStyle`.
3. That theme's `motif`, if it has no `borderStyle` (this is the pre-existing behaviour).
4. Otherwise no pattern, just the plain themed frame.

Vanilla item rarity never sets a border on its own; it only picks a full theme when nothing
else matched. If you want rarity-driven borders, map them explicitly as shown in section 3.

### Built-in rarity borders

`rarity_common`, `rarity_uncommon`, `rarity_rare`, `rarity_epic`, `rarity_mythic` — each pairs
a pattern with that rarity's hue. Note these are *borders*; the identically named **themes**
still exist and still recolor the whole tooltip if you reference them as `"theme"`.

### For mod developers

Register a pattern in code, and it becomes available to every theme and pack:

```java
BorderRegistry.register("mymod_flames", new BorderPattern() {
    @Override
    public BorderPalette defaultPalette(TooltipTheme theme) {
        return BorderPalette.accents(0xFFFF8A4A, 0xFFE3522E, 0xFFFFC178);
    }

    @Override
    public void draw(DrawContext ctx, int x, int y, int w, int h, TooltipTheme theme, BorderPalette p) {
        // draw along the top/bottom lines using p.a(), p.b(), p.c()
    }
});
```

## 5) Available Preset Theme Elements

These are the built-in values you can reuse in your own theme JSON.

### `motif`

- `none`
- `vine`, `ember`, `enchanted`, `bee`, `blossom`, `bubble`, `earth`, `echo`, `cosmic`, `ice`, `lightning`, `autumn`, `soul`, `deepdark`, `poison`, `ocean`, `rustic`, `honey`, `jade`, `wood`, `stone`, `iron`, `gold`, `diamond`, `netherite`, `runic`

### `borderStyle`

- `none`
- Every motif key above except `cosmic` (which has no border pattern), plus any border
  defined in `borders/*.json` or registered by a mod. See section 4.

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

## 6) Built-In Preset Theme Keys

Built-in theme keys you can reference directly:

- `autumn`, `bee`, `blossom`, `bubble`, `cosmic`, `deepdark`, `default`, `diamond`, `earth`, `echo`, `ember`, `enchanted`, `gold`, `honey`, `ice`, `iron`, `jade`, `lightning`, `netherite`, `obfuscated`, `ocean`, `poison`, `rarity_common`, `rarity_epic`, `rarity_mythic`, `rarity_rare`, `rarity_uncommon`, `runic`, `rustic`, `soul`, `stone`, `unstable`, `vine`, `wood`

## 7) Reload and Test

- Reload resources in-game (`F3 + T`), then hover items.
- If tooltips are not applying to your target items, check your client config flags:
  - `enableTooltipRendering`
  - `general.applyTooltipsToVanillaItems`
  - `general.applyTooltipsToModItems`

## 8) Addon Mod Integration: Simply Swords Compat

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
