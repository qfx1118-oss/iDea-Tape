<p align="right"><a href="DESIGN.zh_CN.md">简体中文</a> · <strong>English</strong></p>

# iDea Tape visual system

The reference is the author's black cassette graphic. Preserve its cream italic wordmark, charcoal casing, restrained burnt-orange print and mechanical reels. Library and map remain task-oriented; the cassette is the primary expressive surface.

- Canvas: paper `#F3EEE3`; panels `#FCF9F2`; text `#2B302B`; secondary text `#68675C`.
- Cassette: shell `#202624` / `#1E2422`, lettering `#EEE0C1`, trim `#A99E82`, recording indicator `#E54D2D`, print `#CF6B38`.
- Wordmark: bundled Fraunces italic, weight 900. Draw dotless i and the status dot separately. Cream means idle/playback; red means recording. Launcher icons remain static cream.
- Shell aspect: approximately 1.586:1 with uniform scaling. Android hub centers at (67,83) and (173,83) on a 240 by 151.3 canvas. Device centers at (64,136) and (176,136) within the 212 by 134 shell at (14,68). Hubs sit near the middle rather than against the lower guide holes.
- Motion: accelerate, coast 16 degrees, recoil 4 degrees, then settle; visible Android motion is scheduled at about 30 fps. Hidden views and sleeping device screens do not continuously render.
- Playback: library selection opens cassette detail with one short insertion motion, timeline, play/pause, ten-second skip and speed choice. Long press temporarily accelerates playback. Map remains compact.
- Controls: consistent 24-unit icon family, minimum 48 dp hit targets, native accessible names. Destructive swipe icon is geometrically centered in its 84 dp action region. Undo visibly counts down five seconds.

Validate actual Android and LVGL output, not only mockups. Device screen, BLE continuity and battery savings require physical acceptance after any relevant change.

The 0.6.1 refinement centers header lettering by visible glyph bounds, enlarges the cassette wordmark to a centered 190-unit width, removes the Chinese caption from the Android cassette, and uses large two-line iDea/Tape outlines in the launcher icon. The recording dot and reel geometry/motion remain unchanged.
