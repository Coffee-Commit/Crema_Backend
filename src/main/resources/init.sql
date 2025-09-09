-- ===========================
-- Crema 프로젝트 초기 데이터 삽입 SQL
-- ===========================

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE notification;
TRUNCATE TABLE candidate;
TRUNCATE TABLE meeting_room;
TRUNCATE TABLE hash_tag;
TRUNCATE TABLE experience_detail;
TRUNCATE TABLE experience_group;
TRUNCATE TABLE review_experience;
TRUNCATE TABLE review;
TRUNCATE TABLE time_unit;
TRUNCATE TABLE reservation;
TRUNCATE TABLE survey_file;
TRUNCATE TABLE survey;
TRUNCATE TABLE time_slot;
TRUNCATE TABLE guide_schedule;
TRUNCATE TABLE guide_chat_topic;
TRUNCATE TABLE guide_job_field;
TRUNCATE TABLE guide;
TRUNCATE TABLE member_chat_topic;
TRUNCATE TABLE chat_topic;
TRUNCATE TABLE member_job_field;
TRUNCATE TABLE member;

-- SET FOREIGN_KEY_CHECKS = 1; -- 데이터 입력 후 재활성화 (파일 말미)

-- Member: phone_number 컬럼은 존재하지 않음. provider/provider_id는 존재.
INSERT INTO member (id, nickname, role, point, profile_image_url, description, provider, provider_id, is_deleted, created_at) VALUES
  ('MBR00001', '홍길동', 'ROOKIE', 100, 'https://cdn.example.com/profile1.png', '백엔드 지망생입니다.', 'GOOGLE', 'google_123', false, NOW()),
  ('MBR00002', '김선배', 'GUIDE', 200, 'https://cdn.example.com/profile2.png', '현직 백엔드 개발자입니다.', 'KAKAO', 'kakao_456', false, NOW()),
  ('MBR00003', '프론트가이드', 'GUIDE', 150, 'https://cdn.example.com/profile3.png', '프론트엔드 개발자입니다.', 'GOOGLE', 'google_789', false, NOW());

-- ===========================
-- JobNameType ENUM 값에 맞게 입력 (예: IT_DEVELOPMENT_DATA 등)
INSERT INTO member_job_field (id, member_id, job_name) VALUES
  (1, 'MBR00001', 'IT_DEVELOPMENT_DATA'),
  (2, 'MBR00002', 'RESEARCH_RND'),
  (3, 'MBR00003', 'IT_DEVELOPMENT_DATA');

-- ===========================
-- TopicNameType ENUM 값 사용 (CAREER 는 없음 → JOB_CHANGE 사용)
INSERT INTO chat_topic (id, topic_name) VALUES
  (1, 'INTERVIEW'),
  (2, 'RESUME'),
  (3, 'JOB_CHANGE');

-- ===========================
-- 컬럼명: chat_topic_id (topic_id 아님)
INSERT INTO member_chat_topic (id, member_id, chat_topic_id) VALUES
  (1, 'MBR00001', 1),
  (2, 'MBR00001', 2),
  (3, 'MBR00002', 3);

-- ===========================
-- 필수: title, (선택) certification_image_url, job_position; working_period는 선택
INSERT INTO guide (id, member_id, chat_description, is_opened, title, certification_image_url, approved_date, working_start, working_end, job_position, company_name, is_company_name_public, is_current)
VALUES (1, 'MBR00002', '커피챗으로 면접 준비 도와드립니다.', true, '백엔드 면접/이직 상담', 'https://cdn.example.com/cert1.png', NOW(), '2020-01-01', NULL, 'Backend Engineer', '네이버', true, true),
       (2, 'MBR00003', '프론트엔드 커리어/포트폴리오 상담합니다.', true, '프론트엔드 커리어 상담', 'https://cdn.example.com/cert2.png', NOW(), '2019-03-01', NULL, 'Frontend Engineer', '카카오', true, true);

-- ===========================
-- GuideJobField 는 Guide 당 1개 (OneToOne, guide_id unique). ENUM 값 맞춤
INSERT INTO guide_job_field (id, guide_id, job_name) VALUES
  (1, 1, 'IT_DEVELOPMENT_DATA'),
  (2, 2, 'IT_DEVELOPMENT_DATA');

-- ===========================
-- 컬럼명: chat_topic_id
INSERT INTO guide_chat_topic (id, guide_id, chat_topic_id) VALUES
  (1, 1, 1),
  (2, 1, 3),
  (3, 2, 1),
  (4, 2, 2);

-- ===========================
-- Guide Schedule
-- ===========================
INSERT INTO guide_schedule (id, guide_id, day_of_week) VALUES
                                                           (1, 1, 'MONDAY'),
                                                           (2, 1, 'WEDNESDAY'),
                                                           (3, 2, 'TUESDAY'),
                                                           (4, 2, 'THURSDAY');

-- ===========================
-- Time Slot
-- ===========================
INSERT INTO time_slot (id, schedule_id, start_time_option, end_time_option) VALUES
                                                                                (1, 1, '09:00:00', '12:00:00'),
                                                                                (2, 2, '14:00:00', '18:00:00'),
                                                                                (3, 3, '10:00:00', '12:00:00'),
                                                                                (4, 4, '19:00:00', '21:00:00');

-- 필수: file_upload_url, preferred_date. updated_at 컬럼은 없고 modified_at 임
INSERT INTO survey (id, file_upload_url, message_to_guide, preferred_date, created_at, modified_at) VALUES
  (1, 'https://cdn.example.com/survey1-root.pdf', '면접 준비 조언 부탁드립니다.', '2025-09-15 15:00:00', NOW(), NOW()),
  (2, 'https://cdn.example.com/survey2-root.pdf', '자소서 피드백 받고 싶습니다.', '2025-09-20 19:00:00', NOW(), NOW()),
  (3, 'https://cdn.example.com/survey3-root.pdf', '프로젝트 코드 리뷰 받고 싶어요.', '2025-09-22 20:00:00', NOW(), NOW()),
  (4, 'https://cdn.example.com/survey4-root.pdf', '커리어 전환 조언 부탁드립니다.', '2025-09-25 21:00:00', NOW(), NOW()),
  (5, 'https://cdn.example.com/survey5-root.pdf', '포트폴리오 피드백 부탁드립니다.', '2025-09-28 10:00:00', NOW(), NOW()),
  (6, 'https://cdn.example.com/survey6-root.pdf', '프론트 면접 상담 부탁드립니다.', '2025-09-29 10:00:00', NOW(), NOW()),
  (7, 'https://cdn.example.com/survey7-root.pdf', '리액트 포트폴리오 문의드립니다.', '2025-09-30 14:00:00', NOW(), NOW());


-- 컬럼명: file_Key (@Column 명시)
INSERT INTO survey_file (id, survey_id, file_Key) VALUES
  (1, 1, 'survey/1/file1.pdf'),
  (2, 2, 'survey/2/file1.pdf'),
  (3, 3, 'survey/3/file1.pdf'),
  (4, 4, 'survey/4/file1.pdf'),
  (5, 5, 'survey/5/file1.pdf'),
  (6, 6, 'survey/6/file1.pdf'),
  (7, 7, 'survey/7/file1.pdf');

-- ===========================
-- 통계 검증을 위해 1번 예약은 COMPLETED 로 설정
INSERT INTO reservation (id, guide_id, member_id, survey_id, matching_time, status, reserved_at) VALUES
  (1, 1, 'MBR00001', 1, '2025-09-15 15:00:00', 'COMPLETED', NOW()),
  (2, 1, 'MBR00001', 2, '2025-09-20 19:00:00', 'PENDING', NOW()),
  (3, 1, 'MBR00001', 3, '2025-09-22 20:00:00', 'COMPLETED', NOW()),
  (4, 1, 'MBR00001', 4, '2025-09-25 21:00:00', 'COMPLETED', NOW()),
  (5, 1, 'MBR00001', 5, '2025-09-28 10:00:00', 'COMPLETED', NOW()),
  (6, 2, 'MBR00001', 6, '2025-09-29 10:00:00', 'PENDING', NOW()),
  (7, 2, 'MBR00001', 7, '2025-09-30 14:00:00', 'PENDING', NOW());

-- ===========================
-- 컬럼명: time_type (ENUM), price 컬럼 없음 (TimeType 에 포함)
INSERT INTO time_unit (id, reservation_id, time_type) VALUES
  (1, 1, 'MINUTE_30'),
  (2, 2, 'MINUTE_60'),
  (3, 3, 'MINUTE_30'),
  (4, 4, 'MINUTE_60'),
  (5, 5, 'MINUTE_30'),
  (6, 6, 'MINUTE_30'),
  (7, 7, 'MINUTE_60');

-- ===========================
-- 컬럼명: comment, star_review 는 소수(2,1)
INSERT INTO review (id, reservation_id, comment, star_review) VALUES
  (1, 1, '면접 팁이 정말 유익했습니다!', 4.5),
  (2, 3, '프로젝트 코드 리뷰가 큰 도움이 됐어요.', 5.0),
  (3, 4, '커리어 전환 방향성을 잡을 수 있었어요.', 4.0),
  (4, 5, '포트폴리오 개선 포인트를 알게 되었어요.', 3.5),
  (8, 6, '프론트 면접 팁이 유익했어요.', 4.5),
  (9, 7, '포트폴리오 피드백 좋았어요.', 4.0);

-- ===========================
-- Experience Group
-- ===========================
INSERT INTO experience_group (id, guide_id, guide_chat_topic_id, experience_title, experience_content) VALUES
                                                                                                           (1, 1, 1, '면접 경험', '삼성전자 면접 합격 경험'),
                                                                                                           (2, 1, 2, '이직 경험', '스타트업 → 대기업 전환기 이야기'),
                                                                                                           (3, 2, 3, '프론트 면접 경험', '카카오 면접 합격 경험'),
                                                                                                           (4, 2, 4, '포트폴리오 리뷰', '리액트 포트폴리오 개선 사례');

-- ===========================
-- Experience Detail
-- ===========================
INSERT INTO experience_detail (id, guide_id, who, solution, how) VALUES
  (1, 1, '신입 지원자', '코딩테스트 대비', '스터디 그룹 운영'),
  (2, 2, '주니어 프론트엔드', '포트폴리오 개선', '컴포넌트 구조 리팩토링');

-- ===========================
-- 좋아요(is_thumbs_up)도 함께 설정
INSERT INTO review_experience (id, review_id, experience_group_id, is_thumbs_up) VALUES
  (1, 1, 1, true),
  (2, 2, 1, true),
  (3, 3, 2, true),
  (4, 4, 2, false),
  (5, 8, 3, true),
  (6, 9, 4, true);

-- ===========================
-- Hash Tag
-- ===========================
INSERT INTO hash_tag (id, guide_id, hash_tag_name) VALUES
                                                       (1, 1, '#백엔드'),
                                                       (2, 1, '#면접준비'),
                                                       (3, 2, '#프론트엔드'),
                                                       (4, 2, '#포트폴리오');

-- ===========================
-- Meeting Room
-- ===========================
INSERT INTO meeting_room (id, reservation_id, start_time, end_time, room_url) VALUES
    (1, 1, '2025-09-15 15:00:00', '2025-09-15 15:30:00', 'https://meet.example.com/room1');

-- ===========================
-- 컬럼명: time_id (TimeSlot FK)
INSERT INTO candidate (id, reservation_id, time_id, priority, candidate_date) VALUES
  (1, 1, 1, 1, '2025-09-14 10:00:00'),
  (2, 2, 2, 2, '2025-09-19 15:00:00');

-- ===========================
-- 컬럼: type 없음
INSERT INTO notification (id, member_id, reservation_id, message, is_read, created_at) VALUES
  (1, 'MBR00001', 1, '예약이 확정되었습니다.', false, NOW()),
  (2, 'MBR00002', 1, '새로운 후기가 등록되었습니다.', false, NOW());

-- FK 재활성화
SET FOREIGN_KEY_CHECKS = 1;
