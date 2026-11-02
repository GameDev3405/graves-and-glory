# Vendored three.js

three.js 0.160.0 (MIT), copied from jsDelivr so the map engine runs offline with no install step.

| File | Source | SHA-256 (as committed) |
| --- | --- | --- |
| three.module.min.js | https://cdn.jsdelivr.net/npm/three@0.160.0/build/three.module.min.js | 3e690ac7d180b0aadf0891bea39eec643e29e2d3e75c99b18689518665f69ba6 |
| PointerLockControls.js | https://cdn.jsdelivr.net/npm/three@0.160.0/examples/jsm/controls/PointerLockControls.js | 01f24d8642e2c33e69e00859b3a4e9f78b745771c4d19cf8a9514c00cf2ee5fd |

One edit: in PointerLockControls.js the import line `from 'three'` was rewritten to `from './three.module.min.js'`
(no import map is used). The file as downloaded had SHA-256 b6ac6e2331d02fc877c49727c879b3f78e974dc56f43f2fcb89b03230e1f52ca.
