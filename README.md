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
    },
    "anothermod:special_item": {
      "enabled": false
    }
  },
  "namespaces": {
    "create": {
      "enabled": false
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

Rendering eligibility resolves before themes and providers:
- An exact item with `"enabled": false` always uses its normal vanilla/modded tooltip.
- A `namespaces` entry applies to every item whose registry ID starts with that namespace.
- An exact item `"enabled": true` overrides a disabled namespace, but the normal global
  settings and mapped-item requirements still decide whether Simply Tooltips applies.
- If neither rule exists, rendering eligibility defaults to enabled.

For example, this disables Simply Tooltips for all `create:*` items except the wrench:

```json
{
  "namespaces": {
    "create": { "enabled": false }
  },
  "items": {
    "create:wrench": { "enabled": true, "theme": "iron" }
  }
}
```

Use the explicit `enabled` field for exclusions. JSON `null` and the string `"null"` do not
mean disabled; an absent theme continues through the normal provider/tag/rarity fallback.

Simply Tooltips gathers content through Minecraft's normal loader-aware tooltip pipeline. Text
added by other mods (including Create descriptions and Ponder prompts) is therefore included, and
standard non-text tooltip components are embedded below the themed header unless a dedicated
integration already replaces them. A mod that draws its own tooltip UI after that pipeline may
still need a dedicated compatibility adapter; it can be excluded with an item or namespace
`enabled` rule in the meantime.

### Blacklisting tooltip components

Some mods contribute *decoration* components rather than content — chrome that is redundant once
Simply Tooltips draws its own panel. `tooltip_components` blacklists those by implementation
class name, either exactly or as a package prefix ending in `.` or `*`:

```json
{
  "tooltip_components": {
    "com.example.tooltip.WidgetComponent": { "enabled": false },
    "com.example.tooltip.*": { "enabled": false }
  }
}
```

Note this is unrelated to the `components` array above, which matches item data components.

Legendary Tooltips is blacklisted out of the box: it inserts a framed, rotating item render at
the top of every tooltip, which duplicates the item icon Simply Tooltips already draws in its
header. To get it back, re-enable the rule:

```json
{
  "tooltip_components": {
    "com.anthonyhilyard.legendarytooltips.tooltip.*": { "enabled": true }
  }
}
```

Rules resolve per component class: an exact match wins, then the longest matching prefix, then
the built-in defaults. A rule on a base class also covers subclasses. As with the other
sections, only an explicit `enabled` field counts — an absent one is not a blacklist.

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
- `vine`, `ember`, `enchanted`, `bee`, `blossom`, `bubble`, `earth`, `echo`, `cosmic`, `ice`, `lightning`, `autumn`, `soul`, `deepdark`, `poison`, `blood`, `ocean`, `rustic`, `honey`, `jade`, `wood`, `stone`, `iron`, `gold`, `diamond`, `netherite`, `runic`, `corrupted_eye`, `spectral`, `radiant`, `candle`, `amethyst`, `tome`

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

### `textShadow`

- `true` (default) — text is drawn with Minecraft's built-in drop shadow.
- `false` — text is drawn flat, with no shadow.

Minecraft derives the shadow by darkening the text colour, so on a **light background with dark
text** the shadow is near-black and smears the 1px glyph strokes. Light themes should set this to
`false`. Omitting the key keeps the shadow, so existing themes are unaffected.

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

- `amethyst`, `autumn`, `bee`, `blood`, `blossom`, `bubble`, `candle`, `corrupted_eye`, `cosmic`, `deepdark`, `default`, `diamond`, `earth`, `echo`, `ember`, `enchanted`, `gold`, `honey`, `ice`, `iron`, `jade`, `lightning`, `netherite`, `obfuscated`, `ocean`, `poison`, `radiant`, `rarity_common`, `rarity_epic`, `rarity_mythic`, `rarity_rare`, `rarity_uncommon`, `runic`, `rustic`, `soul`, `spectral`, `stone`, `tome`, `unstable`, `vine`, `wood`

## 7) Reload and Test

- Reload resources in-game (`F3 + T`), then hover items.
- If tooltips are not applying to your target items, check your client config flags:
  - `enableTooltipRendering`
  - `general.applyTooltipsToVanillaItems`
  - `general.applyTooltipsToModItems`

## 8) Theme Studio (in-game editor)

Run `/simplytooltips` in game to open the Theme Studio.

| Command | Does |
| --- | --- |
| `/simplytooltips` | Opens the Studio on the item you are holding |
| `/simplytooltips <item_id>` | Opens it previewing that item |
| `/simplytooltips reload` | Re-reads `config/simplytooltips/` without a resource reload |

The screen has three columns:

- **Left** — every known theme, searchable. A gold dot marks a theme you can edit.
- **Middle** — the item id, its badges, and a **live preview** of the real tooltip. Use `<` `>`
  or the arrow keys to cycle themes and watch the preview change. Editing the badges field updates
  the preview immediately; clearing it goes back to the item's own badges.
  Tall tooltips are scaled to fit and the percentage is shown; the preview is not scrollable or
  tab-switchable.

  | In the preview | Does |
  | --- | --- |
  | Mouse wheel | Zoom toward the cursor, snapping to `25% 33% 50% 100% 200% 300% 400%` |
  | Left-drag | Pan (only where the tooltip is bigger than the stage) |
  | Double-click | Back to fit |
  | Tab chips / `G` | Switch tab, when the item has more than one and `general.tooltipTabs` is on |

  Typing in the item field suggests matching ids: `Up`/`Down` to highlight one, `Tab` or `Enter` to
  accept it, `Esc` to dismiss the list, or just click a row.

  The preview honours `general.tooltipTabs`: with tabs on you get the same tab dots and key hint the
  real tooltip shows, and with tabs off every section is stacked in one panel — whichever the player
  would actually see.
- **Right** — the editor. **COLOURS** lists all 19 colours (click one for a colour wheel with
  value and alpha bars), **STYLE** holds `motif`, `borderStyle`, the two animation styles,
  `itemBorderShape` and `textShadow`, and **TEXT** edits `customTextKeys`.

### Saving

Everything the Studio writes goes to the config folder, never into the mod jar or a resource pack:

```text
<gamedir>/config/simplytooltips/
  themes/
    my_theme.json      <- Save as new / Override
  item_themes/
    studio.json        <- Assign to item
```

These are loaded **after** resource packs and win over them, so a save applies immediately — no
pack to enable and no `F3 + T`. The files use exactly the schema in sections 2 and 3, so you can
copy one into a resource pack to ship it.

- **Save as new** always works. Keys must be lowercase `a-z 0-9 _ - .`.
- **Override** replaces a theme you already saved. It is disabled for the themes built into the
  mod — those can be opened, edited and saved under a new key, but never overwritten.
- **Assign to item** writes the current item and badges into `item_themes/studio.json`, which
  overrides the shipped `defaults.json` for that item.

## 9) Addon Mod Integration: Simply Swords Compat

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
