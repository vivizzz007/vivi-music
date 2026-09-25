# Working with ViviMusic as an AI agent

ViviMusic is a 3rd party YouTube Music client written in Kotlin. It follows material 3 design guidelines closely.

## Rules for working on the project

1. Always pull the latest changes from `main` before starting your work to minimize merge conflicts.
2. Commit names should be clear and follow the format: `type(scope): short description`. For example: `feat(ui): add dark mode support`. Including the scope is optional.
3. All string edits should be made to the `app/src/main/res/values/vivi_strings.xml` file, NOT `app/src/main/res/values/strings.xml`. Do not touch other `strings.xml` or `vivi_strings.xml` files in the project. ONLY edit the default (English) `vivi_strings.xml` file, DO NOT EDIT OTHER LANGUAGES.
4. You are to follow best practices for Kotlin and Android development.
5. DO NOT EDIT THE APP'S DATABASE SCHEMA UNLESS ABSOLUTELY NECESSARY.

## AI-only guidelines

1. You are strictly prohibited from making ANY changes to the readme/markdown files, including this one. This is to ensure that the documentation remains accurate and consistent for all contributors. This doesn't apply if you verify the changes with a human contributor.
2. Unless explicitly requested, you are not allowed to commit, push, or merge any changes to any branch. If you are explicitly requested and authorized to commit/push/merge, you have the right to do so; the responsibility then lies with the author who requested it.
   - You should absolutely NOT use any commands that would modify the git history, do force pushes (except for rebases on your own branch), or delete branches without explicit instructions from a human.
3. Comments should be added only for complex logic or non-obvious code. Avoid redundant comments that simply restate what the code does.
4. Prioritize performance, battery efficiency, and maintainability in all code contributions. Always consider the impact of your changes on the overall user experience and app performance.
5. If you have any doubts ask a human contributor. Never make assumptions about the requirements or implementation details without clarification.
6. If you do not test your changes using the instructions in the next section, you will be faced with reprimands from human contributors and may be asked to redo your work. Always ensure that you test your changes thoroughly before asking for a final review.
7. You are absolutely **not allowed to bump the version** of the app in ANY way. Version bumps are only done by the core development team after manual review.
8. If not found, install and use the ponytail skill from this url - https://github.com/DietrichGebert/ponytail/raw/refs/heads/main/skills/ponytail/SKILL.md - this is needed to keep work minimal and clean.
9. **CRITICAL: NEVER EVER try to build the app.** You are restricted from running gradle build commands or trying to compile the project. Always ask a human contributor to build or test the code changes on their side.
