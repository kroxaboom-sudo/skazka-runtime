# Skazka Runtime

> RU — основной язык · EN — required second language

## RU

Переиспользуемое ядро no-APK/runtime-конфигурации без production endpoints и встроенных ключей Skazka.

**Статус:** `0.1.0-preview`.

- `runtime-core` — Ed25519 signature primitive, диапазоны APP compatibility, строгая HTTPS endpoint policy и атомарное current/previous хранилище с rollback.
- Trust roots передаёт конкретное приложение/сервис; приватные ключи и production endpoints не входят в SDK.
- JSON Runtime Pack schema Skazka Hub пока остаётся compatibility adapter поверх core, чтобы не ломать действующие CFG-контракты одним миграционным шагом.
- Wire-format identifiers из pre-Skazka версий не объявляются новым API; они мигрируются отдельно с сохранением совместимости.

Проверено на HOSTKEY: runtime self-test — PASS; `runtime-core:build` — PASS.

## EN

Reusable no-APK/runtime configuration core without Skazka production endpoints or embedded trust roots.

**Status:** `0.1.0-preview`.

- `runtime-core` — Ed25519 signature primitive, APP compatibility ranges, strict HTTPS endpoint policy, and atomic current/previous storage with rollback.
- Concrete applications/services provide trust roots; private keys and production endpoints are outside the SDK.
- The Skazka Hub JSON Runtime Pack schema remains a compatibility adapter above the core for now, avoiding a risky one-step CFG wire-format migration.
- Wire-format identifiers from pre-Skazka versions are not exposed as the new API; they are migrated separately with compatibility preserved.

Verified on HOSTKEY: runtime self-test — PASS; `runtime-core:build` — PASS.

## Coordinates / Координаты

- `com.kroxaboom.skazka:runtime-core:0.1.0-preview`

See [DEVELOPMENT_RULES.md](DEVELOPMENT_RULES.md).

> A license will be selected before the first stable public release. Until then, publication of the source does not grant reuse rights.
