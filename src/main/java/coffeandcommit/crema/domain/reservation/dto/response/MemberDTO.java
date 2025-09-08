package coffeandcommit.crema.domain.reservation.dto.response;

import coffeandcommit.crema.domain.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MemberDTO {

    private String id;
    private String profileImageUrl;
    private String nickname;
    private String description;
    private MemberJobFieldResponseDTO jobField;
    private List<MemberChatTopicResponseDTO> chatTopics;

    public static MemberDTO from(Member member) {
        return MemberDTO.builder()
                .id(member.getId())
                .profileImageUrl(member.getProfileImageUrl())
                .nickname(member.getNickname())
                .description(member.getDescription())
                .jobField(
                        member.getJobField() != null
                                ? MemberJobFieldResponseDTO.from(member.getJobField())
                                : null
                )
                .chatTopics(
                        member.getChatTopics().stream()
                                .map(MemberChatTopicResponseDTO::from)
                                .collect(Collectors.toList())
                )
                .build();
    }
}
