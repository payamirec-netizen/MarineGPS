# RC2 build reproducibility

The wreck snapshot and release dependency lockfile are committed. Actions are pinned to commit hashes. Normal builds do not write locks. Run 46067011978 passed 60 unit tests and verified the final APK signature. Both builds had identical decompressed ZIP entry contents, but different APK SHA-256 hashes. Metadata diagnostics are added to identify the remaining differences; byte-identical reproducibility is not yet established. Java major version is pinned, not its patch. Field validation remains required.
