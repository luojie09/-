# Secret Base Home Design Snapshot

## Product Context

- Product: "我们的秘密基地" mobile home page
- Platform: mobile-first, single-column portrait layout
- Current preview width: 430px
- Primary use case: a soft, intimate couple app homepage that surfaces relationship time, mood, quick capture, core modules, and recent activity
- Tone: warm, gentle, playful, romantic, not flashy

## Visual Direction

- Overall feel: airy pink-white surface, soft romance, rounded cards, friendly illustration-led interface
- Mood keywords: soft, cozy, affectionate, handwritten-memory energy, lightweight dashboard
- Density: medium-low
- Contrast: soft background with high-contrast primary text
- Shape language: large rounded corners, circular action buttons, pill countdown badge, rounded cards throughout

## Screen Structure

Top to bottom:

1. Header hero
   - Greeting in muted text
   - Couple name: `小羊 & 小耶`
   - Two circular top action buttons: message, settings
   - Large centered hero illustration below

2. Relationship summary card
   - Eyebrow copy: `我们已经在一起`
   - Large day count as the visual anchor
   - Secondary copy: `from 2026.05.06`
   - Anniversary sub-card with:
     - label `下个纪念日`
     - title `恋爱一周年`
     - pill badge `还有 316 天`
     - progress bar label `这一年的陪伴进度`

3. Mood cards
   - Two side-by-side cards
   - Left: 小羊 / 开心 / avatar / emoji
   - Right: 小耶 / 平静 / avatar / emoji

4. Quick record card
   - Label `快速记录`
   - Three small circular utility actions: gallery, camera, calendar
   - Input placeholder: `今天想说点什么……`

5. Core feature grid
   - Two-column card grid
   - Modules:
     - 纪念日 / 316天后
     - 甜蜜相册 / 128张
     - 愿望清单 / 3 / 8 完成
     - 留言墙 / 9条新消息
     - 心情日记 / 已连续12天
     - 情侣任务 / 2项进行中

6. Recent activity
   - Section title: `最近动态`
   - Trailing action: `查看全部`
   - Two stacked activity rows:
     - 小耶上传了 3 张新照 / 2小时前
     - 完成了愿望清单中的「看海」/ 昨天

7. Bottom tab bar
   - 首页
   - 纪念日
   - 相册
   - 我的

## Colors

Primary palette:

- Background: `#FFFBFA`
- Background top tint: `#FFF4F6`
- Hero gradient start: `#FFEEF3`
- Hero gradient middle: `#FFF7F8`
- Hero gradient end: `#FFFBFA`
- Surface white: `#FFFFFF`
- Primary pink: `#FF6F95`
- Soft pink: `#FFD8E4`
- Strong soft pink: `#FFB7CC`
- Outline pink: `#F3DFE6`
- Primary text: `#1F2024`
- Secondary text: `#8C8892`
- Success green token present in codebase: `#63B574`

Usage pattern:

- Primary emphasis is mostly text and small highlights, not large saturated blocks
- Cards stay white or near-white
- Accent pink is used for active state, dots, progress fill, and small badges
- Borders are soft and low-contrast

## Typography

Base type tokens from the current implementation:

- `headlineLarge`: 40 / 44, ExtraBold, tracking -1
- `titleLarge`: 22 / 28, Bold
- `titleMedium`: 18 / 24, Bold
- `bodyLarge`: 16 / 24, Medium
- `bodyMedium`: 14 / 20, Medium
- `bodySmall`: 13 / 18, Medium

Current key text behavior:

- Couple name is visually reduced from the base hero scale and rendered around 36px
- Day count is enlarged beyond the base system for emphasis
- Secondary labels use muted gray and smaller size
- Chinese content should feel breathable, not tightly tracked

## Layout and Spacing

- Outer horizontal padding: 16px
- Large card radii: 24px to 28px
- Action buttons: circular 48px
- Quick utility buttons: about 38px
- Relationship card overlaps the hero area
- Feature grid is 2 columns with comfortable inner padding
- Visual rhythm depends on stacked sections with generous vertical whitespace

## Component Notes

### Header
- Keep the title compact enough that it does not overpower the illustration
- Greeting should remain small and understated

### Relationship Card
- This is the primary information card on the page
- The day count should remain the dominant focal point
- Anniversary information should read as a nested, softer sub-card

### Feature Cards
- Two-column layout works better than three columns for Chinese labels
- Icon, title, summary hierarchy should stay clean and readable
- Some cards can have a slightly warmer tinted background for rhythm

### Activity List
- Keep the list airy and clean
- Dot + thumbnail + title + time + chevron
- Time should remain subtle

## Content Snapshot

- Couple names: `小羊`, `小耶`
- Relationship start date: `2026-05-06`
- Current preview relationship label: `我们已经在一起`
- Current preview days shown in screenshot: `50 天`
- Quick note placeholder: `今天想说点什么……`

## Asset Inventory

Main illustration:

- `app/src/main/res/drawable-nodpi/home_couple_hero.webp`

Avatars:

- `app/src/main/res/drawable-nodpi/avatar_sheep.png`
- `app/src/main/res/drawable-nodpi/avatar_chick.png`

Header icons:

- `app/src/main/res/drawable-nodpi/ic_message_bubble.png`
- `app/src/main/res/drawable-nodpi/ic_settings_gear.png`

Quick actions:

- `app/src/main/res/drawable-nodpi/ic_gallery_stack.png`
- `app/src/main/res/drawable-nodpi/ic_camera.png`
- `app/src/main/res/drawable-nodpi/ic_calendar_outline_pink.png`

Feature icons:

- `app/src/main/res/drawable-nodpi/ic_anniversary_card.png`
- `app/src/main/res/drawable-nodpi/ic_album_card.png`
- `app/src/main/res/drawable-nodpi/ic_wishlist_card.png`
- `app/src/main/res/drawable-nodpi/ic_message_wall_card.png`
- `app/src/main/res/drawable-nodpi/ic_diary_card.png`
- `app/src/main/res/drawable-nodpi/ic_task_card.png`

Activity icons:

- `app/src/main/res/drawable-nodpi/ic_activity_photo.png`
- `app/src/main/res/drawable-nodpi/ic_activity_checklist.png`

Bottom nav icons:

- `app/src/main/res/drawable-nodpi/ic_home_nav_active.png`
- `app/src/main/res/drawable-nodpi/ic_calendar_nav.png`
- `app/src/main/res/drawable-nodpi/ic_album_nav.png`
- `app/src/main/res/drawable-nodpi/ic_profile_nav.png`

## Source Files

Key implementation references:

- `app/src/main/java/com/secretbase/app/ui/home/HomeScreen.kt`
- `app/src/main/java/com/secretbase/app/ui/home/HomeScreenPreviews.kt`
- `app/src/main/assets/homepage_content.json`
- `app/src/main/assets/homepage_visual_config.json`
- `app/src/main/java/com/secretbase/app/ui/theme/Color.kt`
- `app/src/main/java/com/secretbase/app/ui/theme/Type.kt`
- `preview/homepage_preview.html`

## Designer Handoff Notes

- Preserve the soft emotional tone and illustration-centered identity
- Keep the page readable in Chinese, avoid dense 3-column micro-cards
- The relationship card is the hero information anchor, not the feature grid
- Prefer rounded, quiet surfaces over hard geometric or high-tech styling
- If redesigning, keep the structure recognizable enough that engineering can map the new layout back to the existing modules
