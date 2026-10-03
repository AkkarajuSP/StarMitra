# Module Implementation Guide

## Layer rules (enforced by `ModuleBoundaryTest`)

```
api/           controllers + DTOs — NO persistence imports, NO entities in signatures
application/   services + cross-module contract views — orchestrates domain+persistence
domain/        business rules — no Spring web/security deps
persistence/   entities + repositories — this module's tables ONLY
```

**A module may never import another module's `persistence` package.** Cross-module need → call the owner's `application` service (contract).

## Adding an endpoint

1. Confirm the operation exists in `05_API-Specifications/openapi.yaml` (operationId, path, schemas — do not deviate; contract test asserts count=133).
2. Request/response DTOs in `api/` (bean-validation annotations).
3. Logic in `application/` service; entities/repos in `persistence/`.
4. Authorization: `SystemRoleGuard.requireRole/requireAdmin`, `SecurityUtils.currentUserId()`, ownership/membership checks; judges → `JudgeScopeService`.
5. Errors: throw `ApiException(ErrorCode.X)` — never return custom bodies.
6. Idempotent commands: `IdempotencyService.execute` or rely on DB UQ + replay lookup.
7. Auditable actions: `AuditService.record(module, action, actor, context, target, reason)`.

## Ownership cheat-sheet

Judge scope = `JudgeScopeService` (M12) · audit = `AuditService` (kernel) · media URLs = M04 `ObjectStorageClient` · `TalentSkill`/`JudgeExpertise`/`ProjectContributionRole` never grant permission · portals (M19/M20) orchestrate only.
