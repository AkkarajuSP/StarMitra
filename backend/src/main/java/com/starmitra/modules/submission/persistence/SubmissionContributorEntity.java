package com.starmitra.modules.submission.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** DB-02: live ref to M07 member/role + immutable snapshot captured at finalize. */
@Entity
@Table(name = "submission_contributors")
public class SubmissionContributorEntity {

    @Id
    private UUID id;

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(name = "member_ref", nullable = false)
    private UUID memberRef;                        // REF → project_members (user_id)

    @Column(name = "role_ref")
    private UUID roleRef;                          // REF → project_contribution_roles

    @Column(name = "snapshot_member_display", length = 200)
    private String snapshotMemberDisplay;

    @Column(name = "snapshot_role_name", length = 80)
    private String snapshotRoleName;

    @Column(name = "captured_at")
    private OffsetDateTime capturedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected SubmissionContributorEntity() {}

    public SubmissionContributorEntity(UUID submissionId, UUID memberRef, UUID roleRef) {
        this.id = UUID.randomUUID();
        this.submissionId = submissionId;
        this.memberRef = memberRef;
        this.roleRef = roleRef;
    }

    /** DB-02 snapshot at finalization — attribution survives later M07 changes. */
    public void captureSnapshot(String memberDisplay, String roleName) {
        this.snapshotMemberDisplay = memberDisplay;
        this.snapshotRoleName = roleName;
        this.capturedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getMemberRef() { return memberRef; }
    public UUID getRoleRef() { return roleRef; }
    public String getSnapshotRoleName() { return snapshotRoleName; }
    public OffsetDateTime getCapturedAt() { return capturedAt; }
}
