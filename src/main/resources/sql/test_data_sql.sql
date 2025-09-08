-- Survey 테이블 정리 및 초기화
DELETE FROM survey WHERE id BETWEEN 1 AND 10;
ALTER TABLE survey AUTO_INCREMENT = 1;

-- Survey 데이터 재삽입 (ID 없이, AUTO_INCREMENT 사용)
INSERT INTO survey (message_to_guide, preferred_date, created_at, modified_at) VALUES
                                                                                   ('백엔드 개발 면접 준비를 위한 조언을 구하고 싶습니다.', '2024-12-15 14:00:00', NOW(), NOW()),
                                                                                   ('마케팅 전략 수립에 대해 배우고 싶습니다.', '2024-12-16 10:30:00', NOW(), NOW()),
                                                                                   ('포트폴리오 리뷰를 받고 싶습니다.', '2024-12-17 16:00:00', NOW(), NOW()),
                                                                                   ('커리어 전환에 대한 조언이 필요합니다.', '2024-12-18 13:00:00', NOW(), NOW()),
                                                                                   ('실무 경험담을 듣고 싶습니다.', '2024-12-19 15:30:00', NOW(), NOW()),
                                                                                   ('UX/UI 디자인 트렌드에 대해 알고 싶습니다.', '2024-12-20 14:00:00', NOW(), NOW());

-- 생성된 Survey ID 확인
SELECT id FROM survey ORDER BY id;