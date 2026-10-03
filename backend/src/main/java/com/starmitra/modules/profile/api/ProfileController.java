package com.starmitra.modules.profile.api;

import com.starmitra.modules.identity.application.AuthService;
import com.starmitra.modules.profile.application.ProfileService;
import com.starmitra.modules.skill.application.UserSkillReadContract;
import com.starmitra.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * /api/v1/profiles — getMyProfile, updateMyProfile (If-Match/ETag),
 * getPublicProfile. Own-profile authz via security-context userId.
 */
@RestController
@RequestMapping("/api/v1/profiles")
public class ProfileController {

    private final ProfileService profiles;
    private final AuthService auth;

    public ProfileController(ProfileService profiles, AuthService auth) {
        this.profiles = profiles;
        this.auth = auth;
    }

    @GetMapping("/me")
    public ResponseEntity<ProfileDtos.Profile> getMyProfile() {
        var v = profiles.myProfile(SecurityUtils.currentUserId(), defaultName());
        return ResponseEntity.ok()
                .eTag(v.etag())
                .body(toDto(v));
    }

    @PutMapping("/me")
    public ResponseEntity<ProfileDtos.Profile> updateMyProfile(
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody ProfileDtos.ProfileUpdate body) {
        var cmd = new ProfileService.UpdateCommand(body.displayName(), body.bio(),
                body.location(), body.avatarMediaId(), body.visibilityState());
        var v = profiles.updateMyProfile(SecurityUtils.currentUserId(), defaultName(), cmd, ifMatch);
        return ResponseEntity.ok()
                .eTag(v.etag())
                .body(toDto(v));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ProfileDtos.ProfilePublic> getPublicProfile(@PathVariable UUID userId) {
        var v = profiles.publicProfile(SecurityUtils.currentUserId(), userId);
        return ResponseEntity.ok(new ProfileDtos.ProfilePublic(
                v.userId(), v.displayName(), v.bio(), v.avatarMediaId(), mapSkills(v.skills())));
    }

    private String defaultName() {
        var identity = auth.identityOf(SecurityUtils.currentUserId());
        String email = identity.email();
        return email != null && email.contains("@") ? email.substring(0, email.indexOf('@')) : "user";
    }

    private ProfileDtos.Profile toDto(ProfileService.ProfileView v) {
        return new ProfileDtos.Profile(v.userId(), v.displayName(), v.bio(), v.location(),
                v.avatarMediaId(), v.visibilityState(), mapSkills(v.skills()));
    }

    private List<ProfileDtos.SkillView> mapSkills(List<UserSkillReadContract.SkillView> skills) {
        return skills.stream().map(s -> new ProfileDtos.SkillView(s.skillId(), s.name(), s.proficiency())).toList();
    }
}
