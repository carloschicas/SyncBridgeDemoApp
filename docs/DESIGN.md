# Design System Specification: The Architectural Sales Suite

## 1. Overview & Creative North Star: "The Authoritative Anchor"
This design system moves away from the "disposable" feel of standard enterprise apps and toward a "The Authoritative Anchor" aesthetic. In a high-stakes sales environment, the UI must feel as stable and intentional as a physical boardroom. 

We achieve this through **Organic Professionalism**: a layout strategy that favors large-scale editorial typography, generous whitespace (the "Luxury of Space"), and tonal depth over structural lines. By breaking the rigid, boxed-in grid of traditional Android apps, we create a fluid, premium experience that feels custom-built for the elite sales professional.

## 2. Color & Surface Philosophy
The palette is anchored by **Deep Indigo** (`#000666`), signaling tradition and reliability, while using a sophisticated range of surface tones to define hierarchy.

### The "No-Line" Rule
To achieve a high-end feel, **do not use 1px solid borders to section content.** Boundaries are defined strictly through tonal shifts. For example, a `surface-container-low` section sits on a `surface` background to create a "pocket" of information.

### Surface Hierarchy & Nesting
Treat the UI as a series of stacked, physical layers.
- **Base Layer:** `surface` (`#fbf8ff`)
- **Secondary Sectioning:** `surface-container-low` (`#f3f2fe`)
- **Primary Content Cards:** `surface-container-lowest` (`#ffffff`)
- **Elevated Intersections:** `surface-container-high` (`#e8e7f2`)

### The "Glass & Gradient" Rule
For floating elements, like the **Persistent Network Status Indicator**, use glassmorphism:
- **Background:** `surface_variant` at 60% opacity.
- **Effect:** 16px Backdrop Blur.
- **Stroke:** `outline-variant` at 20% opacity (The "Ghost Border").

### Signature Textures
Main Action CTAs must use a subtle **Linear Gradient**:
- **Start:** `primary` (`#000666`)
- **End:** `primary_container` (`#1a237e`) at a 135-degree angle. This adds a "weighted" feel that flat colors cannot replicate.

## 3. Typography: The Editorial Voice
We use **Inter** for its robust x-height and neutral, trustworthy character.

- **The Power Header (`display-md`):** Use for daily sales targets. It should feel unavoidable and monumental.
- **The Narrative Body (`body-lg`):** For client notes and order details. High-readability spacing (1.5 line-height) is mandatory.
- **Micro-Information (`label-md`):** Used for status badges and timestamps. Always uppercase with +5% letter spacing to maintain a "prestige" feel.

| Role | Token | Size | Weight |
| :--- | :--- | :--- | :--- |
| Hero Stats | `display-md` | 2.75rem | 700 |
| Page Titles | `headline-sm` | 1.5rem | 600 |
| Section Headers | `title-sm` | 1rem | 600 |
| Primary Read | `body-lg` | 1rem | 400 |
| Metadata | `label-sm` | 0.6875rem | 500 |

## 4. Elevation & Depth: Tonal Layering
Traditional shadows are often "dirty." We use **Ambient Depth** and **Tonal Stacking**.

- **The Layering Principle:** To lift a card, place a `surface-container-lowest` card on a `surface-container-low` background. The subtle contrast creates a natural edge.
- **Ambient Shadows:** For floating Order Cards, use a highly diffused shadow:
  - **Color:** `on_surface` at 6% opacity.
  - **Blur:** 24px.
  - **Y-Offset:** 8px.
- **Ghost Borders:** If accessibility requires a stroke (e.g., in high-glare outdoor sales environments), use `outline-variant` (`#c6c5d4`) at **15% opacity**. Never use a 100% opaque border.

## 5. Components

### Order Cards & Lists
**Constraint:** Absolute prohibition of divider lines.
- **Structure:** Use 24px of vertical white space (from the Spacing Scale) to separate list items.
- **Background:** Alternate between `surface` and `surface-container-low` for extremely long lists to guide the eye without adding visual clutter.

### Status Badges (The "Signal" Component)
Status is the heartbeat of this application.
- **Online:** `secondary` (`#1b6d24`) text on `secondary_container` (`#a0f399`) background.
- **Offline/Pending:** `tertiary_fixed_variant` (`#773200`) text on `tertiary_fixed` (`#ffdbca`) background.
- **Shape:** Rounded-full (`9999px`) to contrast against the `lg` (`0.5rem`) corners of cards.

### Input Fields
- **Container:** `surface-container-highest`.
- **Active State:** Instead of a thick border, use a 2px bottom-bar in `primary` and a subtle `primary_fixed` background tint to "glow" the active field.

### Persistent Network Indicator
- **Position:** Floating bottom-center.
- **Style:** Glassmorphic pill (`full` roundedness).
- **Behavior:** Transitions from `tertiary` (Offline) to `secondary` (Online) using a 300ms ease-in-out color morph.

## 6. Do’s and Don’ts

### Do
- **Do** use `surface-container-lowest` for cards to make them "pop" against the standard `surface`.
- **Do** use `display-sm` for numbers (currency, units) to give them financial authority.
- **Do** allow content to bleed to the edges of containers where appropriate to create a modern, "unboxed" feel.

### Don’t
- **Don’t** use black (`#000000`) for text. Use `on_surface` (`#1a1b23`) to maintain a premium, ink-like softness.
- **Don’t** use standard Material 3 "Elevated" shadows. Always use the specified Ambient Shadows.
- **Don’t** use icons without labels in the main navigation. In enterprise, clarity is the highest form of luxury.