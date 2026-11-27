# Build provenance

CI uses Java 17, Gradle 8.9 and Android SDK 35. The permanent certificate fingerprint is enforced. CI archives the exact wreck dataset, APK hashes, source commit, tool versions, test XML and permission report. The dataset is still fetched live from EMODnet; archive it for later reproduction. Byte-for-byte reproducibility is not established: external dataset, action tags, toolchain patches and dependencies can vary. Never publish signing secrets.
