package coffeandcommit.crema.domain.globalTag.dto;
import coffeandcommit.crema.domain.globalTag.enums.TopicNameType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopicDTO {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private TopicNameType topicName;
    private String description; // 프론트 표시용 설명 문자열

    public static TopicDTO from(TopicNameType topicName) {
        TopicNameType type = (topicName != null) ? topicName : TopicNameType.UNDEFINED;
        return TopicDTO.builder()
                .topicName(type)
                .description(type.getDescription())
                .build();
    }
}
