# Comic Reading Companion — Comic Print Design Specification

This document is the canonical visual-design reference for **Comic Reading Companion**. New screens, components, and mockups should follow this system unless the design is explicitly changed and approved.

## 1. Core Design Principle

The app uses a **Comic Print** visual system: bold, graphic, tactile, and comic-inspired without looking like a novelty comic-book page.

The interface should feel cohesive across Home, Library, Create/Edit, Browse, and detail screens. New screens must look like siblings of the existing approved screens rather than introducing a new visual language.

Implementation convenience is **not** a reason to deviate from an approved mockup. Approved mockups are design specifications.

## 2. Intellectual-Property / Mockup Content Rule

All mockups must use **original fictional content only**.

Do not include:

- Spider-Man
- Marvel
- DC
- Any real comic publisher branding
- Any recognizable comic character
- Real publisher logos
- Copied or recognizable comic covers
- Third-party franchise names, logos, characters, costumes, or branding

Use fictional publishers, series, issues, characters, logos, and artwork instead.

Existing app/test/catalog data may contain real publisher names where required by the product, but **generated mockups must not**.

## 3. Overall Visual Language

Use:

- Cream / paper-toned backgrounds
- Strong black ink borders
- Modest black offset shadows
- Selective halftone texture
- Bold flat accent colors
- Rounded controls and cards
- Compact, readable, data-dense layouts

Avoid:

- Generic Material styling as the primary visual language
- Glossy gradients
- Glassmorphism
- Soft pastel UI
- Large empty whitespace for decoration
- Fake 3D title extrusion
- Overly rounded “bubble” styling
- Introducing new visual systems per screen

## 4. Color System

Primary Comic Print accent colors include:

- Yellow
- Blue
- Red
- Green
- Purple
- Orange
- Gray
- Black ink
- Cream / paper

Existing project theme colors should be reused rather than inventing new equivalents.

Examples already in the project include:

- `ComicPaper`
- `ComicInk`
- `ComicYellow`
- `ComicRed`
- `ComicGreen`
- `ComicBlueLight`
- `ComicPurple`
- `ComicOrange`
- `ComicGray`

Color use should remain deliberate. Browse and detail screens should generally be visually quieter than Home.

## 5. Typography

### Body / UI Typeface

Use **Barlow** for normal UI text.

Available weights/styles include:

- Regular
- Medium
- Semibold
- Bold
- Extrabold
- Black
- Italic variants where appropriate

### Comic Accent Typeface

Use **Lilita One** for major comic-style display titles and accent headings.

Do not substitute another display font in mockups or implementation unless explicitly approved.

## 6. Locked Main Header Title Treatment

Main Comic Print page titles use the established Lilita One treatment:

- Lilita One
- 54sp
- 54sp line height
- 1sp letter spacing
- Normal font weight
- Yellow face
- Heavy black outline underneath
- No drop shadow / fake extrusion
- Left aligned
- Slight forward lean using the locked geometric transform

Locked transform:

```kotlin
val ComicHeaderTextTransform =
    TextGeometricTransform(
        scaleX = 1.00f,
        skewX = -0.1763f
    )
```

Locked title-style basis:

```kotlin
val titleStyle =
    MaterialTheme.typography.displayLarge
        .copy(
            fontFamily = LilitaOneFontFamily,
            fontSize = 54.sp,
            lineHeight = 54.sp,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Normal,
            fontStyle = FontStyle.Normal,
            textGeometricTransform = ComicHeaderTextTransform
        )
```

Existing implementation uses approximately a 10.2dp-equivalent black outline width.

## 7. Primary Header Structure

Use the established shared Comic Print primary-header family.

Characteristics:

- Blue halftone background
- Status-bar-safe spacing
- Approximately 136dp content height in the shared primary-header component
- Black bottom ink rule
- Yellow / black Comic Print title
- Optional shared back control for non-top-level screens

Examples:

- Home
- Library
- Create Reading List
- Edit Reading List

Top-level screens should visually belong to the same header family.

## 8. Section Headers

Use the established red slanted section-banner treatment.

Characteristics:

- Red `ComicSlantedShape`
- Lilita One
- 20sp default
- 22sp line height
- White face
- Black outlined lettering
- Modest black offset shadow
- Locked accent transform

```kotlin
val ComicAccentTextTransform =
    TextGeometricTransform(
        scaleX = 1.00f,
        skewX = -0.1405f
    )
```

Section headers should be compact and functional, not oversized decorative banners.

## 9. Cards and Controls

Cards and controls should generally use:

- Cream / white surfaces
- Approximately 2dp black outlines
- Rounded corners
- Modest offset black shadows
- Compact internal spacing
- Barlow typography for metadata and controls
- Strong hierarchy without excessive decoration

Buttons may use established yellow, red, blue, green, or cream treatments depending on purpose.

Avoid default Material cards/buttons when a Comic Print equivalent exists.

## 10. Search Fields

Search fields should use the established Comic Print form/search language:

- Cream/white fill
- Black outline
- Rounded corners
- Compact height
- Barlow text
- Simple search and clear icons
- No glossy or floating treatment

Search should remain prominent but not visually overpower the screen.

## 11. Navigation

### Top-Level Screens

Top-level screens retain the established bottom navigation:

- Home
- Browse
- Library

Do not invent additional top-level destinations in mockups unless explicitly requested.

### Detail / Editing Screens

Detail and editing screens hide the top-level bottom navigation.

They use the shared back treatment in the primary header or corresponding approved detail-header style.

## 12. Density and Screen Personality

### Home

Home is the most playful screen.

It may use:

- Larger visual panels
- More expressive accents
- Stronger visual hierarchy
- Hero cards
- Quick-access tiles

### Library

Library is practical and compact.

It should emphasize:

- Search
- Sort
- Reading-list cards
- Progress
- Efficient scanning

### Browse and Detail Screens

Browse, Publisher Detail, Series Detail, and Issue Detail should be **quieter and more data-dense than Home**.

They should:

- Retain the Comic Print identity
- Use strong but restrained section accents
- Favor readable metadata
- Keep cards compact
- Avoid unnecessary ornamental UI
- Preserve clear scanning and navigation

## 13. Browse Screen — Functional Requirements

The Browse redesign must preserve the current behavior.

### Default State

When no search query is active:

- Show the Comic Print Browse header
- Show the search field
- Show a Publishers section
- Display available publishers
- Tapping a publisher opens Publisher Detail

### Search State

Search spans:

- Series
- Issues

Search results should preserve:

- Series title
- Volume where available
- Start year where available
- Publisher metadata where appropriate
- Read-count / total-count information for series
- Issue number
- Issue title where available
- Publication date where available
- Reading status for issues

Series and Issues should be visually distinct result sections.

### Browse Restrictions

Do not invent functionality merely for visual interest.

Unless separately implemented and approved, do not add:

- Character browsing
- Creator browsing
- Event browsing
- New filter systems
- New sort systems
- New tabs that change the information architecture
- New navigation destinations

## 14. Approved Create / Edit Form Language

Create and Edit Reading List establish the form-screen pattern:

- Blue halftone primary header
- Yellow/black title
- Cream paper background
- Red slanted section headers
- Comic Print text fields
- Square reading-list style selector
- Full-width Comic Print dropdown controls
- Search-like Add Issues / Series trigger
- Staged issue rows with explicit reorder/remove controls
- Cream Cancel button
- Yellow primary Save action
- No bottom navigation

Future forms should borrow from this treatment when appropriate.

## 15. Reading-List Style Colors

User-created reading lists support these visual styles:

- Green
- Red
- Blue
- Purple
- Orange
- Gray

The selected style should propagate consistently to reading-list cards where applicable.

## 16. Interaction / Accessibility

Stylized visuals must not compromise usability.

Maintain:

- Clear touch targets
- Readable contrast
- Legible body text
- Obvious enabled / disabled states
- Clear selected states
- Predictable back behavior
- Functional scrolling

Visual flair should never obscure functionality.

## 17. Mockup-to-Implementation Rule

Once a mockup is approved:

- Do not redesign it during implementation
- Do not simplify it without asking
- Do not change spacing, proportions, hierarchy, or component placement merely because another implementation is easier
- If something truly cannot be reproduced, explain the limitation and options before changing the design

When uncertain, ask rather than reinterpret.

## 18. Reuse Before Reinvention

Prefer existing shared components and established patterns whenever they fit.

Examples include:

- Shared primary headers
- Shared primary-header back button
- Comic Print section headers
- Comic Print form fields
- Comic Print dropdowns
- Comic Print action buttons
- Comic Print reading-list cards
- Existing halftone/background treatments

A new component should still look like it belongs to the existing system.

## 19. Canonical Design Summary

When designing a new screen, ask:

1. Does it clearly look like Comic Reading Companion?
2. Does it reuse the blue halftone / cream paper / black ink language?
3. Are Lilita One and Barlow used appropriately?
4. Are section accents consistent with the red slanted banner system?
5. Is the screen as dense and practical as its function requires?
6. Does it preserve existing functionality?
7. Does it avoid introducing an unrelated visual language?
8. Does the mockup contain only original fictional content?
9. Does the navigation match the established app structure?
10. Could this screen plausibly sit beside Home, Library, Create, and Edit without looking like a different app?

If the answer to any of these is no, the design should be revised before implementation.
