# RC2 build reproducibility

Run 46067353553: both builds have identical decompressed entries, entry order and compared ZIP metadata; signing block hashes differ. Final APK contains v2 RSA PKCS#1 SHA-256 (0x0103), optional dependency metadata block 0x504b4453, and padding. Optional encrypted dependency metadata is a plausible source of randomness; disable includeInApk and compare individual block hashes in the next build. This is a hypothesis pending verification, not proof. The permanent signing certificate remains unchanged. Java patch is not pinned. Field approval remains required.
