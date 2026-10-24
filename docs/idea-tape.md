<p align="right"><a href="idea-tape.zh_CN.md">简体中文</a> · <strong>English</strong></p>

# iDea Tape device firmware

Public project: https://github.com/qfx1118-oss/iDea-Tape . Contact: qfx1118@qq.com .

## Controls

Hold UP to record, release to save. DOWN starts latched recording and DOWN stops/saves it. Tap OK to enter the library; UP/DOWN select, OK plays/stops. Hold UP/DOWN to request deletion, OK confirms and UP/DOWN cancel. Hold OK to return to standby. From standby hold OK to open Wi-Fi export; connect a computer to the displayed iDea-Tape hotspot using its displayed password, then open http://192.168.4.1 to preview/download WAV. Hold OK to close export. Wi-Fi export and BLE sessions are exclusive.

Pair the Android companion while the device is in standby. The device displays a fresh six-digit passkey for authenticated pairing. Audio blocks are streamed while recording, with final missing-range backfill. Phone-reported received-block counts drive progress. Completion reaches 100% only after a matching final saved acknowledgement and successful cache deletion; pause/disconnect retains recordings. Playback, recording, manual delete confirmation and export protect storage from conflicting background deletion.

## Storage and timing

Device files use 16kHz mono IMA ADPCM (256-byte blocks / 505 samples), exported as PCM WAV. The cache holds roughly six to seven minutes in total with up to 64 entries; capacity is not unlimited even during live transfer. The phone records an optional fresh start position; the device does not acquire GPS itself. Reels accelerate and decelerate independently of audio stopping, coast 16 degrees, return 4 degrees and settle with damped movement. The application owns state in a worker; LVGL reads locked snapshots.

BLE uses the documented NUS UUIDs with authenticated encryption. Frames carry version/type/length/CRC, and recording identity combines a persistent ID and random token. Final deletion checks complete status, identity, block count and CRC. The phone syncs WAV and metadata before COMMIT. See main/tape_protocol.*, main/tape_sync.* and tests/test_tape_protocol.c. This is an application protocol, not a claim of protection against a fully compromised paired phone.

## Build and release artifacts

Use ESP-IDF 5.5.3 and the repository gate:

```sh
./tools/validate.sh --static
./tools/validate.sh --firmware
./tools/validate.sh
```

The gate checks layout and retains a matching ELF/MAP/application/bootloader/partition bundle. Verify it with tools/archive_firmware.py before release. The public clean-install full image is padded with erased bytes through the complete 8MB layout, so initial flashing clears old NVS and recording data. This is deliberate and must be disclosed. Export important recordings first.

For compatible upgrades that must retain data, use the verified bootloader at 0x0, partition table at 0x8000 and application at 0x10000; never substitute the merged image, which spans NVS. The unchanged recording partition begins at 0x310000 and must not be erased for a preserving update. Device flashing requires separate authorization; a publication does not authorize it.

## Validation status

Earlier development versions received user confirmation for device recording/save/playback, Wi-Fi export, Android pairing, audio synchronization, location recording, automatic device deletion, locked-screen receiving and phone microphone recording. The transfer UI was host-rendered and inspected. Build and host tests are repeated for the open-source release. This does not establish physical acceptance of the new public-map APK or every firmware interaction. Disconnect/backfill, map networking, long sessions, permission/storage failures and broad phone compatibility remain unverified. Rendered cover/gallery images are labeled and contain no credentials.

The original FoloToy MIT license is retained, with Noto CJK font subsets separately covered by SIL OFL. No personal recordings or development/publishing credentials belong in this repository.

This derivative disables automatic upstream synchronization and automatic release publishing. GitHub releases are created from explicitly verified firmware/APK artifacts, without pushing upstream tags.

## 0.5.0 place label

The optional authenticated BLE PLACE packet (type 21) contains state/UTF-8 length followed by at most 96 bytes. Strict UTF-8, state and size checks reject malformed input. State 0 clears, 1 waits for a fix/lookup, 2 displays a supplied place and 3 reports unavailable. The worker clears the transient label after 25 seconds or disconnection, and publishes locked UI snapshots. The 12-pixel font includes the source font’s basic CJK glyphs through U+9FEF; Latin UI fonts remain unchanged. Recording storage layout is unchanged. User confirmed all 0.4.0 tests passed and the 0.5.0 device interface and paired road-name display worked.

## 0.6.0 screen standby

After 30 seconds without a key gesture, the backlight turns off and UI rendering is suspended. Audio capture/playback, BLE receiving and storage continue. Any key wakes the screen; that complete first gesture (including its long/release events) is consumed. Press again to operate. Held recording and visible pairing protect the screen from timing out; a new pairing prompt wakes it. Bluetooth remains available and the phone reconnects after interrupted links. This is screen standby, not deep sleep: no all-day battery claim has been measured. Partition layout and existing recordings are unchanged.
