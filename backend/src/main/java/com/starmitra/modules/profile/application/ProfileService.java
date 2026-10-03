package com.starmitra.modules.profile.application;

import com.starmitra.modules.media.application.MediaReferenceContract;
import com.starmitra.modules.moderation.application.ProfileRestrictionContract;
import com.starmitra.modules.profile.persistence.UserProfileEntity;
import com.starmitra.modules.profile.persistence.UserProfileRepository;
import com.starmitra.modules.skill.application.UserSkillReadContract;
import com.starmitra.platform.audit.AuditService;
import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * M02 profile service — presentation domain only.
 *
 * Visibility != authorization: getPublicProfile filters by visibility_state
 * and M18 restriction; non-viewable → NOT_FOUND (enumeration-safe: PRIVATE /
 * non-PUBLIC / restricted / nonexistent are indistinguishable).
 *
 * Optimistic concurrency: user_profiles has no version column — ETag derives
 * from updated_at; If-Match mismatch → CONFLICT_VERSION.
 */
@Service
public class ProfileService {

    private final UserProfileRepository profiles;
    private final UserSkillReadContract skills;
    private final ProfileRestrictionContract restriction;
    private final MediaReferenceContract mediaRefs;
    private final AuditService audit;

    public ProfileService(UserProfileRepository profiles, UserSkillReadContract skills,
                          ProfileRestrictionContract restriction, MediaReferenceContract mediaRefs,
                          AuditService audit) {
        this.profiles = profiles;
        this.skills = skills;
        this.restriction = restriction;
        this.mediaRefs = mediaRefs;
        this.audit = audit;
    }

    /** Contract views — api layer never sees entities. */
    public record ProfileView(UUID userId, String displayName, String bio, String location,
                              UUID avatarMediaId, String visibilityState,
                              List<UserSkillReadContract.SkillView> skills, String etag) {}
    public record PublicView(UUID userId, String displayName, String bio, UUID avatarMediaId,
                             List<UserSkillReadContract.SkillView> skills) {}
    public record UpdateCommand(String displayName, String bio, String location,
                                UUID avatarMediaId, String visibilityState) {}

    /** Own profile — lazily initialized on first access (contract has no POST). */
    @Transactional
    public ProfileView myProfile(UUID userId, String defaultDisplayName) {
        return toView(profileOrInit(userId, defaultDisplayName));
    }

    /** Public profile — visibility + restriction filtered, enumeration-safe. */
    @Transactional(readOnly = true)
    public PublicView publicProfile(UUID viewerId, UUID targetUserId) {
        UserProfileEntity p = profiles.findByUserId(targetUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        boolean own = targetUserId.equals(viewerId);
        if (!own && !isPubliclyViewable(p)) {
            throw new ApiException(ErrorCode.NOT_FOUND);   // same shape for private/restricted/missing
        }
        return new PublicView(p.getUserId(), p.getDisplayName(), p.getBio(),
                p.getAvatarMediaId(), skills.skillsOf(targetUserId));
    }

    /** Own-profile update with ETag concurrency. */
    @Transactional
    public ProfileView updateMyProfile(UUID userId, String defaultDisplayName,
                                       UpdateCommand cmd, String ifMatch) {
        UserProfileEntity p = profileOrInit(userId, defaultDisplayName);
        if (ifMatch != null && !ifMatch.equals(p.etag())) {
            throw new ApiException(ErrorCode.CONFLICT_VERSION, "Profile was modified concurrently");
        }
        if (cmd.avatarMediaId() != null && !mediaRefs.isUsableBy(cmd.avatarMediaId(), userId)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Avatar media not usable");
        }
        boolean visibilityChanged = cmd.visibilityState() != null
                && !cmd.visibilityState().equals(p.getVisibilityState().name());
        p.applyUpdate(cmd.displayName(), cmd.bio(), cmd.location(), cmd.avatarMediaId(),
                cmd.visibilityState() == null ? null : UserProfileEntity.Visibility.valueOf(cmd.visibilityState()));
        p = profiles.saveAndFlush(p);
        audit.record("M02", "PROFILE_UPDATED", userId, "user", "user_profile", p.getId().toString(),
                visibilityChanged ? "visibility changed" : null);
        return toView(p);
    }

    private UserProfileEntity profileOrInit(UUID userId, String defaultDisplayName) {
        return profiles.findByUserId(userId).orElseGet(() -> {
            try {
                UserProfileEntity p = profiles.saveAndFlush(
                        new UserProfileEntity(userId, defaultDisplayName));
                audit.record("M02", "PROFILE_CREATED", userId, "user", "user_profile", p.getId().toString(), null);
                return p;
            } catch (DataIntegrityViolationException dup) {
                // concurrent first access — uq_user_profiles_user converges
                return profiles.findByUserId(userId).orElseThrow();
            }
        });
    }

    /** PUBLIC only for now — FOLLOWERS/COLLABORATION_ONLY need M21/M06 reads (not yet implemented). */
    private boolean isPubliclyViewable(UserProfileEntity p) {
        return p.getVisibilityState() == UserProfileEntity.Visibility.PUBLIC
                && !restriction.isRestricted(p.getUserId());
    }

    private ProfileView toView(UserProfileEntity p) {
        return new ProfileView(p.getUserId(), p.getDisplayName(), p.getBio(), p.getLocation(),
                p.getAvatarMediaId(), p.getVisibilityState().name(), skills.skillsOf(p.getUserId()), p.etag());
    }
}
