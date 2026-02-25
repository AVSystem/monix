# Requirements: Monix 2026 Maintenance

**Defined:** 2026-02-25
**Core Value:** Keep Monix modern, stable, and performant by updating its foundation and addressing accumulated technical debt

## Active Requirements

### Infrastructure Updates

- [ ] **UP-01**: Scala versions updated to latest patches (2.12.20, 2.13.16, 3.3.5)
- [x] **UP-02**: SBT and core build plugins updated to latest stable versions

### JDK Compatibility

- [ ] **JDK-01**: Full compatibility with JDK 25 verified and any issues resolved

### Technical Debt

- [ ] **DEBT-01**: Stack trace management in `monix-eval` Task resolved (Task.start/startAndForget behavior)
- [ ] **DEBT-02**: Missing doctests in `monix-reactive` Observable implemented

## Out of Scope

| Feature | Reason |
|---------|--------|
| Major new feature development | Maintenance milestone, stability focus |
| API breaking changes | Mature library, backward compatibility required |
| Migration to cats-effect 3 | Only if strictly necessary for JDK 25 |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| UP-01 | Phase 1 | Pending |
| UP-02 | Phase 1 | Complete |
| DEBT-01 | Phase 2 | Pending |
| DEBT-02 | Phase 3 | Pending |
| JDK-01 | Phase 4 | Pending |

**Coverage:**
- Active requirements: 5 total
- Mapped to phases: 5
- Unmapped: 0

---
*Requirements defined: 2026-02-25*
*Last updated: 2026-02-25 after creation from PROJECT.md*
