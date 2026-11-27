# RC2 build provenance

Release uses the same signing identity and application ID. Java 17, Gradle 8.9, SDK 35 and existing explicit dependency versions are retained. Wreck data is frozen to artifact 15231270491 with its archive SHA-256 verified before extraction. This is an interim pin: artifact expires 2027-02-25; commit the exact dataset to the repository before expiry. Transitive dependency locking and immutable action/toolchain pins remain outstanding. CI archives test XML, APK metadata, dataset and hashes. Byte-identical builds are not yet established.
