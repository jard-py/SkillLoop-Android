# SKILLLOOP — Android Design System & Architecture Specification (DESIGN.md)

## 1. Product Vision & Principles
SkillLoop is a peer-to-peer skill exchange platform designed exclusively for university students across faculties, degrees, and years.
- **Core Loop:** CREATE PROFILE → DISCOVER STUDENTS → SEND INTRODUCTION → RECEIVE/ACCEPT REQUEST → UNLOCK CHAT → EXCHANGE SKILLS.
- **Aesthetic Stance:** Bold, tactile, editorial student zine meets refined modern mobile OS. Never feels like a dating app, generic corporate LMS, or bland social media feed.

---

## 2. Design Tokens

### 2.1 Color Palette
| Token Name | Hex Code | Role / Usage |
|---|---|---|
| `color-primary-orange` | `#FF5A00` | Global background canvas, high-energy brand anchor |
| `color-near-black` | `#171717` | Text headers, active pill backgrounds, high-contrast buttons |
| `color-warm-cream` | `#F5F2EB` | Primary surface tablets, student cards, floating nav container |
| `color-warm-beige` | `#D8C8B5` | Photo fallback initials background, secondary chips |
| `color-muted-text` | `#716B62` | Secondary labels, timestamps, metadata, captions |
| `color-thin-separator` | `#D8D2C8` | 1px editorial dividing lines, subtle outlines |
| `color-surface-white` | `#FFFFFF` | Input fields inside cards, pure white highlights, icon contrast |
| `color-badge-green` | `#22C55E` | Online status indicator, pending request counters |

### 2.2 Typography Scale
- **Display Typeface:** `'Big Shoulders Display'`, `'Big Shoulders Text'`, weight `800` (ExtraBold), uppercase tracking `-0.02em` to `+0.05em`.
  - Display 1: `38px` / `line-height: 0.95` (e.g., "YOUR NEXT COLLABORATION.", "YOUR SPACE.")
  - Display 2: `28px` / `line-height: 1.0` (Student names on cards, intro manifesto)
  - Section Headers: `13px` / uppercase, tracking `+0.08em`, font-weight `800` (e.g., "CAN TEACH", "WANTS TO LEARN", "CURRENTLY BUILDING")
- **Body & UI Typeface:** `'Plus Jakarta Sans'`, `'Inter'`, weights `400`, `500`, `600`, `700`.
  - Body Regular: `13px` / `line-height: 1.4` (Bios, intro notes)
  - Pill / Meta: `11px` / `font-weight: 600` / uppercase tracking `+0.05em`
  - Floating Nav: No labels, pure icon geometry

### 2.3 Radii & Elevation
- `radius-tablet`: `24px` to `26px` (Discover cards, incoming request cards, modal sheets)
- `radius-media`: `18px` to `20px` (Inner photo hero boxes)
- `radius-pill`: `9999px` (Action pills, badges, floating nav bar & active tab slider)
- `radius-input`: `14px` (Form fields and composer inputs)
- Shadows: Soft ambient drop shadows (`0 8px 30px rgba(23,23,23,0.08)` on cream cards; `0 12px 32px rgba(23,23,23,0.22)` on floating nav island).

---

## 3. Global Components

### 3.1 Floating Navigation Island
- Positioned fixed `bottom: 24px`, centered horizontally (`left: 50%`, `transform: translateX(-50%)`).
- Dimensions: `width: calc(100% - 64px)`, max-width `320px`, height `56px`.
- Container: `#F5F2EB` (Warm Cream) with `border-radius: 9999px`, inner padding `4px`.
- 4 Tabs: Discover (House), Connect / Requests (Users), Chats (MessageCircle), Profile (User).
- Sliding Indicator: Near-black pill (`#171717`), smooth transition `240ms cubic-bezier(0.16, 1, 0.3, 1)`.

### 3.2 Discover Card
- Background: `#F5F2EB`.
- Photo Hero: Aspect ratio `4:3` or `1:1`, rounded `20px`, containing Domain Badge ("TECH", "DESIGN", "BUSINESS") and Card Counter ("03 / 18").
- Student Details: Name in Big Shoulders ExtraBold, Branch & Year chip ("CS & AI · 3RD YEAR").
- Dual Skill Tags:
  - "CAN TEACH": Vivid orange tinted tags with black text.
  - "WANTS TO LEARN": Warm taupe tinted tags with dark muted text.
- Action Bar: Circular Pass (X), Center Connect (Person+), Undo (Counter-clockwise arrow).
- In-Card Message Composer: Reveals in place of action buttons upon tapping Connect.

### 3.3 Connect / Requests Tablet
- Stacks vertically on vivid orange background.
- Shows sender photo/initials, domain, academic tags, bio preview, skills match, and the highlight quote of their actual introductory message.
- Actions: "PASS" (subtle stroke) and "ACCEPT & CHAT" (solid near-black pill).

---

## 4. Interaction & Motion System
- **Connect Flow:** Tapping '+' triggers composer slide-up/fade-in (200ms ease-out). Typing a note and hitting Send animates a tactile checkmark before advancing card.
- **Card Exit:** Pass swipe or button click slides card left with subtle tilt (-8deg). Connect acceptance dissolves card upwards while incrementing the chat badge.
- **Tab Switching:** Active island pill glides fluidly across navigation items without jumping or repainting layout.
