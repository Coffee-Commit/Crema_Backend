package coffeandcommit.crema.domain.member.service;

import coffeandcommit.crema.domain.globalTag.entity.ChatTopic;
import coffeandcommit.crema.domain.globalTag.enums.JobNameType;
import coffeandcommit.crema.domain.globalTag.enums.TopicNameType;
import coffeandcommit.crema.domain.globalTag.repository.ChatTopicRepository;
import coffeandcommit.crema.domain.member.dto.request.MemberChatTopicRequest;
import coffeandcommit.crema.domain.member.dto.request.MemberJobFieldRequest;
import coffeandcommit.crema.domain.member.dto.response.MemberChatTopicResponse;
import coffeandcommit.crema.domain.member.dto.response.MemberJobFieldResponse;
import coffeandcommit.crema.domain.member.dto.response.MemberCoffeeChatResponse;
import coffeandcommit.crema.domain.member.entity.Member;
import coffeandcommit.crema.domain.member.entity.MemberChatTopic;
import coffeandcommit.crema.domain.member.entity.MemberJobField;
import coffeandcommit.crema.domain.member.repository.MemberRepository;
import coffeandcommit.crema.domain.reservation.entity.Reservation;
import coffeandcommit.crema.domain.reservation.enums.Status;
import coffeandcommit.crema.domain.reservation.repository.ReservationRepository;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberCoffeeChatService {

    private final MemberRepository memberRepository;
    private final ChatTopicRepository chatTopicRepository;
    private final ReservationRepository reservationRepository;

    // === 관심 주제 관리 ===

    @Transactional
    public List<MemberChatTopicResponse> registerChatTopics(String memberId, MemberChatTopicRequest request) {
        Member member = findActiveMemberById(memberId);

        // 기존 관심 주제 모두 삭제
        member.getChatTopics().clear();

        // 새로운 관심 주제 등록
        List<TopicNameType> topicNames = request.getTopicNames();
        if (topicNames == null || topicNames.isEmpty()) {
            // 아무것도 선택하지 않으면 UNDEFINED로 설정
            topicNames = List.of(TopicNameType.UNDEFINED);
        }

        List<MemberChatTopic> newChatTopics = new ArrayList<>();
        for (TopicNameType topicName : topicNames) {
            ChatTopic chatTopic = findOrCreateChatTopic(topicName);

            MemberChatTopic memberChatTopic = MemberChatTopic.builder()
                    .member(member)
                    .chatTopic(chatTopic)
                    .build();

            newChatTopics.add(memberChatTopic);
        }

        member.getChatTopics().addAll(newChatTopics);
        memberRepository.save(member);

        return newChatTopics.stream()
                .map(MemberChatTopicResponse::from)
                .collect(Collectors.toList());
    }

    public List<MemberChatTopicResponse> getChatTopics(String memberId) {
        Member member = findActiveMemberById(memberId);

        return member.getChatTopics().stream()
                .map(MemberChatTopicResponse::from)
                .collect(Collectors.toList());
    }

    // === 관심 분야 관리 ===

    @Transactional
    public MemberJobFieldResponse registerJobField(String memberId, MemberJobFieldRequest request) {
        Member member = findActiveMemberById(memberId);

        JobNameType jobName = request.getJobName();
        if (jobName == null) {
            jobName = JobNameType.UNDEFINED; // 미선택시 UNDEFINED로 설정
        }

        MemberJobField memberJobField = member.getJobField();
        if (memberJobField != null) {
            // 기존 분야가 있으면 업데이트
            memberJobField = memberJobField.toBuilder()
                    .jobName(jobName)
                    .build();
        } else {
            // 새로운 분야 생성
            memberJobField = MemberJobField.builder()
                    .member(member)
                    .jobName(jobName)
                    .build();
        }

        // Member 엔티티의 jobField 설정
        member = member.toBuilder()
                .jobField(memberJobField)
                .build();

        memberRepository.save(member);

        return MemberJobFieldResponse.from(memberJobField);
    }

    public MemberJobFieldResponse getJobField(String memberId) {
        Member member = findActiveMemberById(memberId);

        MemberJobField jobField = member.getJobField();
        if (jobField == null) {
            // 분야가 설정되지 않았으면 UNDEFINED로 반환
            jobField = MemberJobField.builder()
                    .member(member)
                    .jobName(JobNameType.UNDEFINED)
                    .build();
        }

        return MemberJobFieldResponse.from(jobField);
    }

    // === 커피챗 예약 조회 ===

    public List<MemberCoffeeChatResponse> getPendingReservations(String memberId) {
        return getReservationsByStatus(memberId, Status.PENDING);
    }

    public List<MemberCoffeeChatResponse> getConfirmedReservations(String memberId) {
        return getReservationsByStatus(memberId, Status.CONFIRMED);
    }

    public List<MemberCoffeeChatResponse> getCompletedReservations(String memberId) {
        return getReservationsByStatus(memberId, Status.COMPLETED);
    }

    public List<MemberCoffeeChatResponse> getCancelledReservations(String memberId) {
        return getReservationsByStatus(memberId, Status.CANCELLED);
    }

    public List<MemberCoffeeChatResponse> getAllReservations(String memberId) {
        findActiveMemberById(memberId); // 멤버 존재 확인

        List<Reservation> reservations = reservationRepository.findByMember_Id(memberId);

        return reservations.stream()
                .map(MemberCoffeeChatResponse::from)
                .collect(Collectors.toList());
    }

    // === Private Helper Methods ===

    private Member findActiveMemberById(String memberId) {
        return memberRepository.findByIdAndIsDeletedFalse(memberId)
                .orElseThrow(() -> new BaseException(ErrorStatus.MEMBER_NOT_FOUND));
    }

    private ChatTopic findOrCreateChatTopic(TopicNameType topicName) {
        return chatTopicRepository.findByTopicName(topicName)
                .orElseGet(() -> {
                    ChatTopic newChatTopic = ChatTopic.builder()
                            .topicName(topicName)
                            .build();
                    return chatTopicRepository.save(newChatTopic);
                });
    }

    private List<MemberCoffeeChatResponse> getReservationsByStatus(String memberId, Status status) {
        findActiveMemberById(memberId); // 멤버 존재 확인

        List<Reservation> reservations = reservationRepository.findByMember_IdAndStatus(memberId, status);

        return reservations.stream()
                .map(MemberCoffeeChatResponse::from)
                .collect(Collectors.toList());
    }
}