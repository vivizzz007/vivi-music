# Master-Docs Structure

## Directory Layout
```
docs/
├── architecture/
│   ├── arch_decision_records/ (ADRs)
│   ├── system_overview.md
│   └── diagrams/ (Mermaid)
├── guides/
│   ├── onboarding.md (Zero-to-Hero)
│   ├── contributing.md
│   └── release_process.md
├── reference/
│   ├── api/ (Dokka generated)
│   └── style_guide.md
└── reports/
    ├── project_status/
    └── post_mortems/
```

## Critical Documents
1.  **`README.md`**: The landing page. High-level "What", "Why", "How to Run".
2.  **`Technical_Manual.md`**: The deep dive. Stack, patterns, and "How it works".
3.  **`CONTRIBUTING.md`**: The rulebook. Governance, PR process, TDD expectations.

## Doc-as-Code
- Write docs in Markdown.
- Commit docs with code changes.
- Use Mermaid for diagrams to keep them valid and editable.
