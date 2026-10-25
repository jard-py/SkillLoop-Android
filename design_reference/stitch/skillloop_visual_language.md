---
name: SkillLoop Visual Language
colors:
  surface: '#fcf9f2'
  surface-dim: '#dcdad3'
  surface-bright: '#fcf9f2'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f6f3ec'
  surface-container: '#f1eee7'
  surface-container-high: '#ebe8e1'
  surface-container-highest: '#e5e2db'
  on-surface: '#1c1c18'
  on-surface-variant: '#5b4137'
  inverse-surface: '#31312c'
  inverse-on-surface: '#f3f0e9'
  outline: '#907065'
  outline-variant: '#e4beb1'
  surface-tint: '#a83900'
  primary: '#a83900'
  on-primary: '#ffffff'
  primary-container: '#ff5a00'
  on-primary-container: '#511700'
  inverse-primary: '#ffb59a'
  secondary: '#5f5e5e'
  on-secondary: '#ffffff'
  secondary-container: '#e2dfde'
  on-secondary-container: '#636262'
  tertiary: '#685d4d'
  on-tertiary: '#ffffff'
  tertiary-container: '#9e907f'
  on-tertiary-container: '#332a1d'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#ffdbcf'
  primary-fixed-dim: '#ffb59a'
  on-primary-fixed: '#380d00'
  on-primary-fixed-variant: '#802900'
  secondary-fixed: '#e5e2e1'
  secondary-fixed-dim: '#c8c6c5'
  on-secondary-fixed: '#1c1b1b'
  on-secondary-fixed-variant: '#474746'
  tertiary-fixed: '#f1e0cc'
  tertiary-fixed-dim: '#d4c4b1'
  on-tertiary-fixed: '#221a0e'
  on-tertiary-fixed-variant: '#504537'
  background: '#fcf9f2'
  on-background: '#1c1c18'
  surface-variant: '#e5e2db'
typography:
  display-hero:
    fontFamily: Bebas Neue
    fontSize: 56px
    fontWeight: '400'
    lineHeight: 56px
    letterSpacing: 0.02em
  headline-lg:
    fontFamily: Bebas Neue
    fontSize: 40px
    fontWeight: '400'
    lineHeight: 42px
    letterSpacing: 0.03em
  headline-lg-mobile:
    fontFamily: Bebas Neue
    fontSize: 32px
    fontWeight: '400'
    lineHeight: 34px
    letterSpacing: 0.03em
  headline-md:
    fontFamily: Bebas Neue
    fontSize: 26px
    fontWeight: '400'
    lineHeight: 28px
    letterSpacing: 0.04em
  headline-sm:
    fontFamily: Bebas Neue
    fontSize: 20px
    fontWeight: '400'
    lineHeight: 22px
    letterSpacing: 0.04em
  title-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '700'
    lineHeight: 24px
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '500'
    lineHeight: 24px
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '700'
    lineHeight: 18px
    letterSpacing: 0.01em
  label-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '700'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 10px
    fontWeight: '700'
    lineHeight: 12px
    letterSpacing: 0.04em
rounded:
  sm: 0.5rem
  DEFAULT: 1rem
  md: 1.5rem
  lg: 2rem
  xl: 3rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-sm: 0.75rem
  margin: 1rem
  margin-lg: 1.5rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style
The design system positions peer-to-peer campus skill swapping at the intersection of high-energy collegiate culture and editorial print publications. It caters directly to ambitious, creative university students seeking fast, authentic collaboration without bureaucratic friction. 

The aesthetic is energetic, tactile, and boldly editorial. By anchoring visual hierarchy in punchy, condensed uppercase display titles and balancing them with approachable geometric body type, the interface projects both authority and effortless warmth. Interfaces feature high-impact vivid orange surfaces juxtaposed against rich warm cream modular plates, creating an unmistakable poster-like rhythm optimized for fast mobile discovery.

## Colors
The palette balances vibrant athletic energy with tactile editorial poise:

- **Primary (`#FF5A00`)**: Vivid dynamic orange. Acts as the primary active canvas, core brand identifier, and primary interactive trigger for critical CTAs.
- **Secondary (`#171717`)**: Near-black charcoal. Delivers sharp contrast for primary typographic elements, heavy display headlines, and high-emphasis controls.
- **Tertiary (`#D8C8B5`)**: Warm beige/taupe. Used for secondary badge backgrounds, subtle metadata chips, and grounding tonal accents.
- **Neutral (`#F5F2EB`)**: Warm cream. Serves as the primary content tablet surface, breaking the intense orange background into legible, focused interactive islands.
- **Muted Text (`#716B62`)**: Warm gray dedicated to secondary captions, timestamps, and secondary metadata to maintain high-readability ratios without harsh visual dissonance.
- **Dividers (`#D8D2C8`)**: Crisp, light-neutral borders providing sharp editorial delineation without disrupting the warm physical feel.
- **White (`#FFFFFF`)**: Pure high-contrast surface reserved for image frames, avatar badges, and selected floating island overlays.

## Typography
All primary titles utilize Bebas Neue in uppercase to project a punchy, magazine-headline presence with tight vertical tracking. To counteract the vertical density of the display face, structural body content, labels, and forms utilize Plus Jakarta Sans with generous vertical leading and open geometric apertures. 

Always render Bebas Neue with uppercase text transforms. Body content must prioritize medium and regular weights (`400`, `500`) to keep long skill descriptions effortless to skim on mobile screens, while pill badges and action items utilize bold (`700`) metadata weights.

## Layout & Spacing
The layout model employs a mobile-first fluid grid based on an 8pt system, using a primary 4-column structure for mobile viewports and adapting to 8 columns for tablet layouts.

Outer canvas margins are fixed at `1rem` (16px) on mobile viewports to maximize usable area while preserving an intentional edge framing of the vivid orange background. Card content containers sit directly on the orange canvas with a gap of `1rem`, establishing an organic modular scroll. Tablet and large screen viewports increase horizontal margins to `1.5rem` and center the core mobile activity column (maximum width: 480px) to sustain deliberate app-focused interactions.

## Elevation & Depth
Depth is created primarily through high-contrast tonal layering and tactile physical borders rather than diffuse, artificial drop shadows.

- **Canvas Tier**: Vivid orange (`#FF5A00`) serves as the base layer (`elevation-0`).
- **Surface Tier**: Warm cream (`#F5F2EB`) cards sit directly on the orange surface (`elevation-1`), defined by crisp hairline dividers (`#D8D2C8`, 1px) and zero blur, relying on color value differentiation.
- **Floating Island Tier**: Navigation pills and primary active modals float over content (`elevation-2`) using a subtle ambient lift: `0 8px 24px rgba(23, 23, 23, 0.12)`.
- **Tactile Pressed State**: Active interactive cards and buttons simulate mechanical depression through micro-scale downshifts (`scale(0.98)`) and high-contrast stroke emphasis rather than standard shadow fade.

## Shapes
The design embraces generous pill-shaped geometry (`roundedness: 3`) across all tactile touchpoints to counter the severe vertical lines of the headline typography. 

Core content cards and large media containers employ `rounded-lg` (2rem / 32dp) to establish soft, friendly tablets. Interactive buttons, domain badges, chips, and floating navigation bars adopt full pill profiles (`rounded-full` / 9999px). Secondary containers and inner image viewports use standard pill radius (`1rem` / 16dp) to preserve structural coherence throughout nested components.

## Components

### Buttons
- **Primary Action**: Full pill shape, `#171717` background with `#F5F2EB` text in Plus Jakarta Sans Bold (`label-lg`), vertical padding `0.875rem`, horizontal padding `1.75rem`. Tactile scale down to `0.98` on touch.
- **Secondary / Swap Action**: Pill shape, `#FF5A00` background with `#FFFFFF` text, subtle 1px border (`rgba(255,255,255,0.2)`).
- **Outline Action**: Pill shape, transparent background with 1.5px `#171717` border and `#171717` text.

### Cards & Content Tablets
- Constructed with `#F5F2EB` surfaces and `rounded-lg` (32px) corners.
- Nested imagery features oversized rounded frames (16px to 24px radius) with subtle 1px `#D8D2C8` inner borders.
- Skill metadata sections inside cards are separated by crisp `#D8D2C8` 1px hairline dividers.

### Chips & Domain Badges
- Pill-shaped (`rounded-full`), height `28px` to `32px`.
- High-contrast categorized styling: Warm Beige/Taupe (`#D8C8B5`) background with `#171717` bold micro-text (`label-sm`).
- Active/selected state transforms background to `#171717` with `#FFFFFF` text.

### Floating Navigation Island
- Positioned `1rem` above the bottom screen safe area.
- Centered pill enclosure utilizing Near-Black (`#171717`) or Warm Cream (`#F5F2EB`) with blurred backdrop filtration.
- Hosts key navigation triggers with subtle active indicators (circular vivid orange dots).

### Input Fields
- Outer container matches `rounded-full` (for search pills) or `rounded-lg` (for multi-line skill forms).
- Background `#FFFFFF` with `#D8D2C8` 1px border.
- Focus state activates an intense 1.5px `#FF5A00` outline with zero exterior shadow.

### Checkboxes & Radio Controls
- Radio controls use concentric circles with `#171717` fill and `#FF5A00` active core centers.
- Checkboxes use `rounded-md` (8px) squares filled with `#171717` upon selection and a crisp `#FFFFFF` checkmark glyph.