# ViviMusic Updater Changelog & SDUI Guide

This document explains exactly how to structure your custom `changelog.json` or `changelogbet.json` payloads to take full advantage of the Server-Driven UI (SDUI) layout engine inside the ViviMusic Updater!

## 1. Native HTML Rich Text (Colors, Bold, Links)
The updater's text parser features a powerful HTML translation engine natively hooked into Jetpack Compose. You can inject these tags freely into any string (e.g. `description` or inside `items` arrays)! 

**Supported Tags:**
- **Bold**: `<b>Target</b>` or `<strong>Target</strong>`
- **Italic**: `<i>Target</i>` or `<em>Target</em>`
- **Links**: `<a href="https://example.com">Click Here</a>` (Natively tinted and underlined)
- **Colors**: `<font color="#HEXCODE">Your Text</font>`
- **Sizing**: `<small>Text</small>`

### Recommended Colors (Material Palettes):
You can use ANY standard 6-digit hex code! Here are some crisp Material 3 colors that look great on Dark Mode:
- **Blue (Accent)**: `<font color="#2196F3">Text</font>`
- **Green (Success)**: `<font color="#4CAF50">Text</font>`
- **Red (Warning)**: `<font color="#FF5252">Text</font>`
- **Yellow (Notice)**: `<font color="#FFC107">Text</font>`
- **Purple (Premium)**: `<font color="#9C27B0">Text</font>`

*(Example):* `Added <font color="#FFC107"><b>Glassmorphic Animations</b></font> globally.`


## 2. Global JSON Architecture Schema
Your release JSON file should sit at the top-level format:
```json
{
  "version": "6.0.6",
  "description": "Optional global release description text.",
  "image": "Optional URL to a global header graphic.",
  "changelog": [
    {
       "title": "What's New",
       "description": "Optional section description.",
       "items": [
           "Added native SDUI engine.",
           "Spoofed API for <font color=\"#4CAF50\">safer streaming</font>."
       ],
       "blocks": [
          // (SDUI Media blocks go here natively!)
       ]
    }
  ]
}
```

## 3. Server-Driven UI (SDUI) Blocks
The `"blocks"` array lets you inject native layout components seamlessly (Videos, Images, Rows, Spacing) directly into a changelog section!

### Supported Block Types:
- `"image"`: Renders an asynchronous network image optimally via Coil.
- `"video"`: Renders a muted, looping, auto-playing video frame natively via ExoPlayer.
- `"row"`: Renders a horizontal container sequence (FlexBox). This block MUST contain a `"children"` array.
- `"text"`: Standard text block.

### SDUI Modifiers (`"modifier"`):
You can attach a `"modifier"` object to ANY block to control bounds!
- `"fillMaxWidth"`: (Boolean) Forces the block to consume the entire screen width.
- `"heightDp"`: (Integer) Explicit height in Display Pixels.
- `"paddingDp"`: (Integer) Applies padded scaling spacing globally.
- `"aspectRatio"`: (Float) Crops media accurately (e.g. `1.77` for 16:9 widescreen, `1.0` for square).
- `"weight"`: (Float) **Only use inside a "row" block!** Dictates flex width sharing (e.g., two images with `"weight": 1.0` will take 50% left-right perfectly).

### Complex SDUI Example (Two Images Side-by-Side)
```json
"blocks": [
  {
    "type": "row",
    "modifier": { "fillMaxWidth": true, "paddingDp": 8 },
    "children": [
      {
        "type": "image",
        "url": "https://example.com/asset1.png",
        "modifier": { "weight": 1.0, "aspectRatio": 1.77 }
      },
      {
        "type": "image",
        "url": "https://example.com/asset2.png",
        "modifier": { "weight": 1.0, "aspectRatio": 1.77 }
      }
    ]
  }
]
```

### Video Rendering Example
```json
"blocks": [
  {
    "type": "video",
    "url": "https://example.com/recording.mp4",
    "modifier": {
      "fillMaxWidth": true,
      "paddingDp": 16,
      "aspectRatio": 0.56 
    }
  }
]
```
*(Note: Since mobile portrait screens are vertical, a standard vertical mobile recording mathematically holds a `0.56` aspect ratio (9:16). A standard horizontal desktop monitor holds a `1.77` aspect ratio (16:9).)*
