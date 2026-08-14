# AGENTS.md

Instructions for AI coding agents working in this repository.

## Commit messages — MANDATORY rule

**NEVER add yourself (the agent / client) as a co-author of a commit** unless the
user explicitly asks for it in the message.

Do **not** append footers like:

```
Generated with Codebuff 🤖
Co-Authored-By: Codebuff <noreply@codebuff.com>
```

or any `Co-Authored-By:` / `Generated with …` trailer that credits the agent or
the client, unless the user explicitly instructs otherwise in that request.

Write a normal, conventional commit message that describes the change. Example:

```
feat(desktop): use VIVI Music DE logo for the native app icons
```

## General conventions

- Branch: `vivi-music-de`.
- Commit style: Conventional Commits (`feat:`, `fix:`, `ci:`, `refactor:`, …).
- Commit and push after making changes, when asked.
- Do not commit unrelated files (e.g. stray artifacts) unless relevant.
