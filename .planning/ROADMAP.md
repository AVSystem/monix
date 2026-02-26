# Roadmap: Monix 2026 Maintenance

## Overview

Modernize Monix's build infrastructure, resolve accumulated technical debt, and ensure forward compatibility with JDK 25. Sequential phases: stabilize the build first, then address code quality, then verify future JDK support.

## Phases

- [x] **Phase 1: Infrastructure Modernization** - Update SBT, Scala versions, and build plugins to latest stable releases
- [ ] **Phase 2: Task Stack Traces** - Address stack trace management technical debt in monix-eval
- [ ] **Phase 3: Observable Doctests** - Implement missing doctests in monix-reactive Observable
- [ ] **Phase 4: JDK 25 Compatibility** - Verify and fix JDK 25 compatibility issues

## Phase Details

### Phase 1: Infrastructure Modernization
**Goal**: Update the build system and Scala compilers to their latest stable versions
**Depends on**: Nothing (first phase)
**Requirements**: [UP-01, UP-02]
**Success Criteria** (what must be TRUE):
  1. SBT updated to 1.10.7+ and build loads without errors
  2. All build plugins updated to latest compatible versions
  3. Scala 2.12.20, 2.13.16, and 3.3.5 compile successfully
  4. `sbt +test` passes across all Scala versions
  5. Binary compatibility checks pass (`sbt +mimaReportBinaryIssues`)
**Plans:** 2 plans

Plans:
- [x] 01-01-PLAN.md — Complete sbt-tpolecat migration to org.typelevel 0.5.3
- [x] 01-02-PLAN.md — Cross-version build verification (compile, test, MiMa)

### Phase 2: Task Stack Traces
**Goal**: Address stack trace management in `Task.start` and `Task.startAndForget`
**Depends on**: Phase 1
**Requirements**: [DEBT-01]
**Success Criteria** (what must be TRUE):
  1. `Task.start` stack trace behavior is documented and intentional
  2. Test case in `TaskTracingSuite` verifies trace continuity/separation
  3. All existing tests pass with any changes
**Plans**: TBD

Plans:
- [ ] 02-01: Investigate and resolve Task.start stack trace management

### Phase 3: Observable Doctests
**Goal**: Implement missing doctests in `Observable`
**Depends on**: Phase 1
**Requirements**: [DEBT-02]
**Success Criteria** (what must be TRUE):
  1. All `// TODO: to implement!` markers in Observable.scala are resolved
  2. Doctest examples compile and produce correct output
  3. `sbt monix-reactive/doctest` passes
**Plans**: TBD

Plans:
- [ ] 03-01: Implement Observable doctest examples

### Phase 4: JDK 25 Compatibility
**Goal**: Ensure Monix is ready for JDK 25
**Depends on**: Phase 1
**Requirements**: [JDK-01]
**Success Criteria** (what must be TRUE):
  1. No usage of internal JDK APIs that are restricted in JDK 25
  2. JCTools and low-level dependencies updated to JDK 25-compatible versions
  3. CI matrix includes JDK 25 (if available)
  4. Test suite passes on JDK 25
**Plans**: TBD

Plans:
- [ ] 04-01: Analyze JDK 25 risks and update dependencies
- [ ] 04-02: Update CI for JDK 25 testing

## Progress

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Infrastructure Modernization | 2/2 | Complete | 2026-02-26 |
| 2. Task Stack Traces | 0/1 | Not started | - |
| 3. Observable Doctests | 0/1 | Not started | - |
| 4. JDK 25 Compatibility | 0/2 | Not started | - |
