# MarineGPS 1.0.0 stable readiness

- [x] RC2 Release build 46067353553: 60 tests passed; final APK v2 signature verified; permanent certificate matches; no INTERNET; non-debuggable.
- [x] Wreck snapshot and release dependency locks committed; actions pinned.
- [ ] Confirm clean-build binary equality after disabling optional APK dependency metadata (no signing key change).
- [ ] Verify upgrade on device without data loss, offline GNSS, compass, routes, TrackBack and recording recovery.
- [ ] Verify anchor alarms and notifications with screen off; catch log and backup restore.
- [ ] Only after field approval bump versionCode to 23 and versionName to 1.0.0; build, verify and publish.

Historical wreck positions are not safe destinations. Backup across separate databases is not atomic. MarineGPS is not certified marine navigation equipment.
