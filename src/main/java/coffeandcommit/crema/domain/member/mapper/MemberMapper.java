package coffeandcommit.crema.domain.member.mapper;

import coffeandcommit.crema.domain.member.dto.response.MemberResponse;
import coffeandcommit.crema.domain.member.dto.response.MemberPublicResponse;
import coffeandcommit.crema.domain.member.entity.Member;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MemberMapper {

    // 본인 조회용 (모든 필드 포함)
    MemberResponse memberToMemberResponse(Member member);

    // 타인 조회용 (공개 정보만)
    MemberPublicResponse memberToMemberPublicResponse(Member member);
}