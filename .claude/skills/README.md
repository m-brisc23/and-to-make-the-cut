# Project skills

Claude Code loads these automatically when a task matches a skill's description. You can also ask
for one by name ("use the jetpack-compose-performance skill to …").

## Vendored from Google's Android skills

These folders are unmodified copies from [android/skills](https://github.com/android/skills),
licensed under Apache-2.0 (see `LICENSE-android-skills.txt`).

- **Source commit:** `5bda39f154fb859a08d45e2392f3568e049f83fb` (2026-10-14)

| Skill | Why it's here |
|---|---|
| `jetpack-compose-performance` | Diagnosing jank and recomposition in our Compose screens |
| `android-profiler` | Recording and analysing Perfetto traces to confirm performance fixes |
| `r8-analyzer` | Auditing `app/proguard-rules.pro` and the minified release build |
| `testing-setup` | Adding Compose UI and screenshot tests (a gap noted in ARCHITECTURE.md) |
| `edge-to-edge` | Insets handling (the app already calls `enableEdgeToEdge()`) |
| `adaptive` | Tablet and foldable layouts |
| `navigation-3` | A possible migration from Navigation Compose 2.9 |
| `agp-9-upgrade` | Moving past AGP 8.10 |

### Why vendor them as well as enable the plugin?

`.claude/settings.json` also enables the whole `android-skills` plugin from Google's marketplace.
The two do different jobs:

- **Plugin:** always current, and covers all 30 skills. Plugin skills are namespaced
  (`android-skills:<name>`).
- **Copies:** pinned to a known version, so behaviour doesn't change under us. They're reviewable
  in PRs and work even where plugins aren't installed.

### Updating the copies

```bash
git clone --depth 1 https://github.com/android/skills.git /tmp/android-skills
cp -r /tmp/android-skills/<category>/<skill> .claude/skills/   # e.g. jetpack-compose/adaptive
```

Then update the source commit above and review the diff before committing. Skills are
instructions the agent follows, so treat changes to them like code changes.
