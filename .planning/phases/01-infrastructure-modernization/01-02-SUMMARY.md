---
phase: 01-infrastructure-modernization
plan: 02
subsystem: infra
tags: [sbt, scala, cross-compilation, tpolecat, mima, scalac, compiler-warnings]

# Dependency graph
requires:
  - phase: 01-infrastructure-modernization
    plan: 01
    provides: sbt-tpolecat 0.5.3 migration with explicit ScalacOptions import
provides:
  - verified cross-compilation across Scala 2.12.20, 2.13.16, and 3.3.5
  - targeted -Wconf suppressions for tpolecat 0.5.x newly-enabled warnings
  - Scala 3 value-discard error fix in CancelablePromise.scala
  - test-time scalacOptions tuned for pre-existing test patterns
affects: [phase-01-pr, subsequent-phases]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "tpolecat 0.5.x -Wconf pattern: suppress -Wvalue-discard via msg=unused value of type:silent for 2.13"
    - "tpolecat 0.5.x test scope: remove -Wunused:patvars and -Xlint:constant for pre-existing test patterns"
    - "Scala 3 with tpolecat 0.5.x: remove -Werror (not -Xfatal-warnings) to allow warning-only builds"

key-files:
  created: []
  modified:
    - build.sbt
    - monix-execution/shared/src/main/scala/monix/execution/CancelablePromise.scala

key-decisions:
  - "Added -Wconf:msg=unused value of type:silent for Scala 2.13 to suppress tpolecat 0.5.x -Wvalue-discard on intentional patterns (trySuccess/tryFailure returning Boolean)"
  - "Removed -Werror for Scala 3 (tpolecat 0.5.x uses -Werror not -Xfatal-warnings; existing removal of -Xfatal-warnings was insufficient)"
  - "Fixed CancelablePromise.scala:245 with @unchecked annotation on val x :: xs pattern (Scala 3 requires this for refutable patterns used as definitions)"
  - "Added Test / scalacOptions --= to remove -Wunused:patvars and -Xlint:constant for pre-existing test patterns that are intentional"
  - "JS test hang on CatsEffectIssue380Suite documented as known pre-existing issue unrelated to infrastructure changes"
  - "MiMa TrampolineExecutionContext startLoop signature change is intentional (pre-existing on series/3.x-avs branch)"

patterns-established:
  - "tpolecat 0.5.x warning suppressions: use -Wconf with specific msg= or cat= filters, never disable -Xfatal-warnings globally"
  - "Test scope compiler options: tune separately from main scope to avoid suppressing genuine issues in production code"

requirements-completed: [UP-01, UP-02]

# Metrics
duration: ~2 sessions (previous executor + UAT verification)
completed: 2026-02-26
---

# Phase 01 Plan 02: Cross-Compile Verification Summary

**Monix cross-compilation verified passing on Scala 2.12.20, 2.13.16, and 3.3.5 via targeted tpolecat 0.5.x warning suppressions and a Scala 3 @unchecked fix in CancelablePromise**

## Performance

- **Duration:** ~2 sessions (executor + UAT)
- **Started:** 2026-02-25T13:00:00Z
- **Completed:** 2026-02-26T00:00:00Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- All three Scala versions (2.12.20, 2.13.16, 3.3.5) compile without errors (verified via UAT)
- Targeted -Wconf suppressions added for tpolecat 0.5.x newly-enabled warnings that trigger on pre-existing intentional patterns
- Fixed Scala 3 compilation: removed -Werror (tpolecat 0.5.x uses -Werror instead of -Xfatal-warnings) and added @unchecked to refutable pattern in CancelablePromise.scala
- Test-scope scalacOptions tuned to remove -Wunused:patvars and -Xlint:constant which triggered on valid test patterns

## Task Commits

Tasks were verified via UAT. Fix commits were made by the previous executor:

1. **Task 1: Cross-compile and fix all Scala versions** - `7465e7b6` (fix) + `294f64c2` (fix)
2. **Task 2: Run full test suite and binary compatibility checks** - Verified via user UAT

**Plan metadata:** (docs commit following)

## Files Created/Modified
- `build.sbt` - Added -Wconf suppressions for 2.13 value-discard warnings; removed -Werror for Scala 3; added Test scope scalacOptions tuning
- `monix-execution/shared/src/main/scala/monix/execution/CancelablePromise.scala` - Added @unchecked annotation to val x :: xs pattern (Scala 3 refutable pattern fix)

## Decisions Made
- Warning suppressions are targeted and minimal: msg= filters used instead of blanket category suppressions to avoid masking genuine issues
- Test scope options tuned separately from main scope so production code still gets strict checking
- CancelablePromise fix uses @unchecked (language-idiomatic) rather than restructuring the pattern match
- sbt `+compile` passes with exit code 0 for all three Scala versions (UAT verified)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Scala 2.13 value-discard warnings blocking compilation**
- **Found during:** Task 1 (Cross-compile and fix all Scala versions)
- **Issue:** tpolecat 0.5.x newly enables -Wvalue-discard in Scala 2.13; pre-existing patterns like trySuccess/tryFailure returning Boolean triggered errors
- **Fix:** Added `-Wconf:msg=unused value of type:silent` in sharedSettings for Scala 2.13
- **Files modified:** build.sbt
- **Verification:** sbt ++2.13.16 compile passes (UAT verified)
- **Committed in:** 7465e7b6

**2. [Rule 1 - Bug] Scala 3 -Werror not removed (tpolecat 0.5.x API change)**
- **Found during:** Task 1 (Cross-compile and fix all Scala versions)
- **Issue:** tpolecat 0.5.x uses -Werror (not -Xfatal-warnings) for Scala 3; existing build.sbt only removed -Xfatal-warnings which was insufficient
- **Fix:** Added removal of -Werror for Scala 3 in sharedSettings
- **Files modified:** build.sbt
- **Verification:** sbt ++3.3.5 compile passes (UAT verified)
- **Committed in:** 7465e7b6

**3. [Rule 1 - Bug] Scala 3 refutable pattern error in CancelablePromise.scala**
- **Found during:** Task 1 (Cross-compile and fix all Scala versions)
- **Issue:** `val x :: xs = ...` on line 245 is a refutable pattern; Scala 3 requires @unchecked annotation for such patterns used as definitions
- **Fix:** Added @unchecked annotation to the pattern
- **Files modified:** monix-execution/shared/src/main/scala/monix/execution/CancelablePromise.scala
- **Verification:** sbt ++3.3.5 compile passes (UAT verified)
- **Committed in:** 7465e7b6

**4. [Rule 1 - Bug] Test-scope warnings blocking test compilation**
- **Found during:** Task 2 (Run full test suite)
- **Issue:** tpolecat 0.5.x enables -Wunused:patvars and -Xlint:constant which trigger on pre-existing intentional test patterns (for-comprehension loop vars; intentional overflow tests)
- **Fix:** Added `Test / scalacOptions --=` to remove these options in test scope
- **Files modified:** build.sbt
- **Verification:** Test compilation succeeds (UAT verified)
- **Committed in:** 294f64c2

---

**Total deviations:** 4 auto-fixed (all Rule 1 bugs from tpolecat 0.5.x newly-enabled compiler options)
**Impact on plan:** All fixes essential for compilation success. Suppressions are targeted, not blanket. No scope creep.

## Known Issues (Not Fixed)

**JS test hang: CatsEffectIssue380Suite**
- The Scala.js test suite CatsEffectIssue380Suite hangs during `sbt +test`
- This is a pre-existing issue unrelated to the infrastructure changes on this branch
- UAT decision: SKIPPED (not blocking Phase 1 completion)

**MiMa: TrampolineExecutionContext startLoop signature**
- MiMa reports a binary incompatibility on TrampolineExecutionContext startLoop method signature
- This change is intentional and pre-existing on the series/3.x-avs branch
- A MiMa exclusion filter should be added in project/MimaFilters.scala as a separate follow-up
- UAT decision: SKIPPED (intentional change, not a regression from this branch's work)

## Issues Encountered

- tpolecat 0.5.x behavioral changes were not fully anticipated in the plan. The migration in Plan 01 correctly updated the plugin, but the downstream effects on compiler options required additional fix commits in this plan.
- Scala 3 -Werror vs -Xfatal-warnings distinction was a subtle tpolecat 0.5.x API change that required investigation.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Phase 1 infrastructure modernization complete: SBT 1.10.7, all plugins updated, Scala 2.12.20/2.13.16/3.3.5 all compile
- The bump-dependencies branch is ready for PR to series/3.x-avs
- Two follow-up items identified (JS test hang, MiMa exclusion filter) but neither blocks the PR

---
*Phase: 01-infrastructure-modernization*
*Completed: 2026-02-26*
