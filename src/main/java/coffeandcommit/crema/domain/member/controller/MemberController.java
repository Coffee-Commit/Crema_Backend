package coffeandcommit.crema.domain.member.controller;

import coffeandcommit.crema.domain.member.dto.request.MemberCreateRequest;
import coffeandcommit.crema.domain.member.dto.response.MemberResponse;
import coffeandcommit.crema.domain.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    public ResponseEntity<MemberResponse> createMember(@Valid @RequestBody MemberCreateRequest request) {
        try {
            MemberResponse member = memberService.createMember(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(member);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<MemberResponse> getMember(@PathVariable String id) {
        try {
            MemberResponse member = memberService.getMemberById(id);
            return ResponseEntity.ok(member);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<MemberResponse> getMemberByUsername(@PathVariable String username) {
        try {
            MemberResponse member = memberService.getMemberByUsername(username);
            return ResponseEntity.ok(member);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/profile")
    public ResponseEntity<MemberResponse> updateProfile(
            @PathVariable String id,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String introduction,
            @RequestParam(required = false) String profileImageUrl) {

        try {
            MemberResponse member = memberService.updateMemberProfile(id, nickname, introduction, profileImageUrl);
            return ResponseEntity.ok(member);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMember(@PathVariable String id) {
        memberService.deleteMember(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check/username/{username}")
    public ResponseEntity<Boolean> checkUsernameAvailability(@PathVariable String username) {
        boolean available = memberService.isUsernameAvailable(username);
        return ResponseEntity.ok(available);
    }

    @GetMapping("/check/nickname/{nickname}")
    public ResponseEntity<Boolean> checkNicknameAvailability(@PathVariable String nickname) {
        boolean available = memberService.isNicknameAvailable(nickname);
        return ResponseEntity.ok(available);
    }
}