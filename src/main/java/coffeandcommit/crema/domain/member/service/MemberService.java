package coffeandcommit.crema.domain.member.service;

import coffeandcommit.crema.domain.member.dto.request.MemberCreateRequest;
import coffeandcommit.crema.domain.member.dto.response.MemberResponse;
import coffeandcommit.crema.domain.member.entity.Member;
import coffeandcommit.crema.domain.member.enums.MemberRole;
import coffeandcommit.crema.domain.member.mapper.MemberMapper;
import coffeandcommit.crema.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;


    public MemberResponse createMember(MemberCreateRequest request) {
        if (memberRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        Member member = request.toEntity(encodedPassword)
                .toBuilder()
                .id(UUID.randomUUID().toString())
                .role(MemberRole.ROOKIE)
                .point(0)
                .build();

        Member savedMember = memberRepository.save(member);
        return MemberMapper.INSTANCE.memberToMemberResponse(savedMember);
    }

    public MemberResponse getMemberById(String id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        return MemberMapper.INSTANCE.memberToMemberResponse(member);
    }

    public MemberResponse getMemberByUsername(String username) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        return MemberMapper.INSTANCE.memberToMemberResponse(member);
    }

    public MemberResponse updateMemberProfile(String id, String nickname, String introduction, String profileImageUrl) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        member.updateProfile(nickname, introduction, profileImageUrl);
        Member savedMember = memberRepository.save(member);
        return MemberMapper.INSTANCE.memberToMemberResponse(savedMember);
    }

    public void deleteMember(String id) {
        memberRepository.deleteById(id);
    }

    public boolean isUsernameAvailable(String username) {
        return !memberRepository.existsByUsername(username);
    }

    public boolean isNicknameAvailable(String nickname) {
        return !memberRepository.existsByNickname(nickname);
    }
}