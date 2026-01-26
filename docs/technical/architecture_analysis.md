# Architecture Analysis & Refactoring Strategy (Phase 1)

## 1. Deep Think & Status Quo Analysis

### Status Quo
The examination of the `ui` folder reveals a classic "growing pains" structure.
- **Monolithic Files**: The file `Items.kt` is an extreme example of "Anti-SFC". It is over 1500 lines long and contains definitions for everything from `SongListItem` and `AlbumListItem` to generic `GridItem` and `OverlayPlayButton`. This severely violates the **Single Responsibility Principle** and the **Single File Component (SFC)** principle.
- **Flat Hierarchy**: The `ui/component` directory is a "graveyard" of 32 files without clear semantic grouping. `volume`, `lyrics`, and `dialogs` all exist on the same level.
- **Visibility**: A search for `internal fun` in the `ui` package returned **0 results**. This means *every* small helper composable (e.g., a specific title formatter within `Items.kt`) is theoretically visible throughout the entire project. This pollutes the global namespace and hampers refactoring, as the IDE auto-complete suggests everything.

### SFC Violations
1.  **Lack of Physical Separation**: Logically distinct components (e.g., `SongGridItem` vs. `PlaylistListItem`) reside in the same physical file (`Items.kt`). SFC requires: One Component = One File.
2.  **Missing Encapsulation**: `GridItem` (a base component) is defined *publicly* in `Items.kt` and used by specific implementations. If `GridItem` is intended only as an implementation detail for consistency, it should be `internal` or `private`.
3.  **Domain Mixing**: `Items.kt` knows about both "Songs" (Music Domain) and generic "ListItems" (UI Basics).

---

## 2. Structural Proposal (The Barrel Principle for Kotlin)

Since Kotlin does not have `index.js`-style barrels, we emulate this through **Package Structure** and **Facade Files**.

**Target Structure:**

```text
com/music/vivi/ui/
├── components/
│   ├── core/              <-- The "Core Barrel"
│   │   ├── lists/
│   │   │   ├── BaseListItem.kt
│   │   │   └── internal/  <-- Hidden implementation details
│   │   ├── grids/
│   │   │   └── BaseGridItem.kt
│   │   └── inputs/
│   │       ├── ViviCheckbox.kt
│   │       └── ViviSlider.kt
│   │
│   ├── media/             <-- "Media Barrel" (Domain Components)
│   │   ├── songs/
│   │   │   ├── SongListItem.kt   <-- SFC
│   │   │   └── SongGridItem.kt   <-- SFC
│   │   ├── albums/
│   │   │   ├── AlbumCard.kt
│   │   │   └── AlbumRow.kt
│   │   └── MediaComponents.kt    <-- OPTIONAL: "Barrel File" bundling imports (via Typealias or Inline)
│   │
│   └── sheets/
│
└── screens/ ...
```

**Implementing the Barrel Principle:**
Each sub-package (e.g., `media`) acts as a logical module.
- **Public API**: Only top-level components (e.g., `SongListItem`) are `public`.
- **Internal API**: Helper methods within `SongListItem.kt` are `private` or `internal`.

---

## 3. Refactoring Plan (Phase 2)

We will proceed iteratively to minimize build breaks.

### Step 1: Core Extraction ("Foundation Barrel")
Isolate the primitive building blocks from `Items.kt`.
- **Action**: Create `ui/components/core/ListItem.kt` and `ui/components/core/GridItem.kt`.
- **Content**: Move the generic `ListItem(...)` and `GridItem(...)` functions there.
- **Visibility**: Set these to `public` (as they are base components) or `internal` (if we want to restrict usage).

### Step 2: Media Component Separation (SFC)
Break down the rest of `Items.kt`.
- **Action**: Create a dedicated package under `ui/components/media/` for each type (`Song`, `Album`, `Artist`, `Playlist`).
- **Execution**:
    - `Songcomponents.kt`: Can contain `SongListItem` and `SongGridItem` (as related SFCs for "Song Display"), or strictly two files.
    - Copy code from `Items.kt` -> New File.
    - Delete code from `Items.kt`.
    - Fix imports in the project.

### Step 3: Visibility Lockdown
- **Action**: Review the new files.
- **Check**: Anything not directly called by a Screen (e.g., sub-composables for thumbnails, badges) becomes `private`.
- **Goal**: When typing in a Screen, the IDE should only suggest `SongListItem`, not `SongListItemBadgeRow`.

### Step 4: Cleanup
- Delete the empty `Items.kt`.
- Review `Lyrics.kt` and split it if it is also too large (SFC Principle).

---

**Recommendation for Start:**
Shall we begin with **Step 1 (Core Extraction)** and slowly dissolve `Items.kt`?
