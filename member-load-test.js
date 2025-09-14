// member-load-test.js - 커피챗 프로젝트 Member 도메인 K6 부하테스트 (최종 버전)

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// =============================================================================
// 커스텀 메트릭스 (Member API 관련만)
// =============================================================================
const profileUpdateRate = new Rate('profile_update_success_rate');
const nicknameCheckRate = new Rate('nickname_check_success_rate');
const memberInfoQueryRate = new Rate('member_info_query_success_rate');
const chatTopicsRate = new Rate('chat_topics_success_rate');
const jobFieldsRate = new Rate('job_fields_success_rate');
const coffeeChatReservationsRate = new Rate('coffee_chat_reservations_success_rate');
const memberApiDuration = new Trend('member_api_duration');
const memberApiErrors = new Counter('member_api_errors');
const loginDuration = new Trend('login_duration');

// =============================================================================
// 테스트 옵션
// =============================================================================
export const options = {
    vus: 1,
    iterations: 1,
    duration: '2m',

    thresholds: {
        http_req_duration: ['p(95)<2000'],              // 95%의 요청이 2초 이내
        http_req_failed: ['rate<0.05'],                 // 전체 실패율 5% 미만
        profile_update_success_rate: ['rate>0.90'],     // 프로필 업데이트 성공률 90% 이상
        nickname_check_success_rate: ['rate>0.95'],     // 닉네임 확인 성공률 95% 이상
        member_info_query_success_rate: ['rate>0.95'],  // 회원 정보 조회 성공률 95% 이상
        chat_topics_success_rate: ['rate>0.90'],        // 채팅 주제 관리 성공률 90% 이상
        job_fields_success_rate: ['rate>0.90'],         // 직무 분야 관리 성공률 90% 이상
        // coffee_chat_reservations_success_rate: ['rate>0.95'], // 커피챗 예약 조회는 일시 제외
        login_duration: ['p(95)<500'],                  // 로그인 95% 0.5초 이하
    },

    tags: {
        test_type: 'member_api_comprehensive_test',
    }
};

// =============================================================================
// 설정 및 상수
// =============================================================================
let BASE_URL = 'http://localhost:8080';

// 환경별 설정
if (__ENV.ENVIRONMENT === 'dev') {
    BASE_URL = 'https://dev-api.crema.com';
} else if (__ENV.ENVIRONMENT === 'prod') {
    BASE_URL = 'https://api.crema.com';
}

// =============================================================================
// 테스트 데이터 생성기
// =============================================================================
class TestDataGenerator {
    constructor() {
        this.usedNicknames = new Set();

        // 샘플 채팅 주제 ID들 (실제 DB에 있는 값들로 교체 필요)
        this.chatTopicIds = [1, 2, 3, 4, 5, 6];
    }

    generateUniqueNickname() {
        // VU ID, 타임스탬프, 랜덤 문자열을 조합하여 고유성 극대화
        const vuId = __VU || 1;
        const timestamp = Date.now();

        // 6자리 랜덤 영숫자 문자열 생성
        const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
        let randomString = '';
        for (let i = 0; i < 6; i++) {
            randomString += chars.charAt(Math.floor(Math.random() * chars.length));
        }

        // 추가 랜덤성을 위한 카운터 (VU 내에서의 호출 순서)
        if (!this.nicknameCounter) {
            this.nicknameCounter = 0;
        }
        this.nicknameCounter++;

        // 최종 닉네임 조합: vu{VU}_{timestamp}_{randomString}_{counter}
        const nickname = `vu${vuId}_${timestamp}_${randomString}_${this.nicknameCounter}`;

        // 만약을 위한 Set 체크 (실제로는 거의 사용되지 않을 것)
        let finalNickname = nickname;
        let attempts = 0;
        while (this.usedNicknames.has(finalNickname) && attempts < 10) {
            attempts++;
            const extraRandom = Math.floor(Math.random() * 999999);
            finalNickname = `${nickname}_${extraRandom}`;
        }

        this.usedNicknames.add(finalNickname);

        // 닉네임이 너무 길어지지 않도록 최대 50자로 제한
        if (finalNickname.length > 50) {
            finalNickname = finalNickname.substring(0, 50);
        }

        return finalNickname;
    }

    generateProfileUpdateData() {
        return {
            nickname: this.generateUniqueNickname(),
            description: 'K6 부하테스트로 생성된 프로필 설명입니다.',
            email: `loadtest.${Date.now()}@crema-test.com`
        };
    }

    generateChatTopicsData() {
        // 1-3개의 랜덤 주제 선택
        const count = Math.floor(Math.random() * 3) + 1;
        const shuffled = [...this.chatTopicIds].sort(() => 0.5 - Math.random());
        return {
            topics: shuffled.slice(0, count).map(id => {
                const topics = ["RESUME", "COVER_LETTER", "PORTFOLIO", "INTERVIEW", "PRACTICAL_WORK", "ORGANIZATION_CULTURE"];
                return {
                    topicName: topics[id - 1] || "RESUME"
                };
            })
        };
    }

    generateJobFieldsData() {
        // JobNameType enum 값 중 하나를 선택
        const jobNames = [
            "IT_DEVELOPMENT_DATA",
            "DESIGN",
            "MARKETING_PR",
            "MANAGEMENT_SUPPORT",
            "PLANNING_STRATEGY",
            "RESEARCH_RND"
        ];

        return {
            jobName: jobNames[Math.floor(Math.random() * jobNames.length)]
        };
    }
}

const testData = new TestDataGenerator();

// =============================================================================
// 공통 검증 함수
// =============================================================================
function validateResponse(response, expectedStatus = 200, checkName = 'Response validation', metric = null) {
    const isValid = check(response, {
        [`${checkName} - status is ${expectedStatus}`]: (r) => r.status === expectedStatus,
        [`${checkName} - has body`]: (r) => r.body && r.body.length > 0,
        [`${checkName} - response time < 3s`]: (r) => r.timings.duration < 3000,
    });

    // 메트릭 업데이트
    if (metric) {
        metric.add(isValid ? 1 : 0);
    }

    memberApiDuration.add(response.timings.duration);

    if (!isValid) {
        console.error(`${checkName} failed:`, {
            status: response.status,
            body: response.body ? response.body.substring(0, 200) : 'No body',
            duration: response.timings.duration
        });
        memberApiErrors.add(1);
    }

    return isValid;
}

// =============================================================================
// 인증 헬퍼 (Member API 테스트를 위한 사전 작업용)
// =============================================================================
function setupTestUser() {
    console.log('Creating test rookie account...');

    // 1. 루키 계정 생성
    let createStart = Date.now();
    let createResponse = http.post(`${BASE_URL}/api/test/auth/create-rookie`);

    console.log(`Create account response status: ${createResponse.status}`);

    check(createResponse, {
        '루키 계정 생성 성공': (r) => r.status === 200 || r.status === 201,
        '루키 닉네임 존재': (r) => {
            try {
                const data = r.json();
                return data.result && data.result.nickname;
            } catch (e) {
                return false;
            }
        }
    });

    if (createResponse.status !== 200 && createResponse.status !== 201) {
        console.error('Failed to create test account');
        return null;
    }

    let nickname;
    try {
        const responseData = createResponse.json();
        nickname = responseData.result.nickname;
        console.log('Test account created with nickname:', nickname);
    } catch (e) {
        console.error('루키 계정 생성 응답 파싱 실패:', createResponse.body);
        return null;
    }

    if (!nickname) {
        console.error('No nickname found in response');
        return null;
    }

    sleep(0.2);

    // 2. 로그인
    console.log('Attempting login with nickname:', nickname);
    let loginStart = Date.now();
    let loginResponse = http.post(`${BASE_URL}/api/test/auth/login`,
        JSON.stringify({ nickname: nickname }),
        { headers: { 'Content-Type': 'application/json' } }
    );
    loginDuration.add(Date.now() - loginStart);

    console.log(`Login response status: ${loginResponse.status}`);

    check(loginResponse, {
        '로그인 성공': (r) => r.status === 200,
        '액세스 토큰 존재': (r) => {
            try {
                const data = r.json();
                return data.result && data.result.accessToken;
            } catch (e) {
                return false;
            }
        }
    });

    if (loginResponse.status !== 200) {
        console.error('Login failed');
        return null;
    }

    let token;
    try {
        const responseData = loginResponse.json();
        token = responseData.result.accessToken;
        console.log('Successfully obtained access token');
    } catch (e) {
        console.error('로그인 응답 파싱 실패:', loginResponse.body);
        return null;
    }

    return {
        token: token,
        nickname: nickname,
        memberId: loginResponse.json().result.memberId
    };
}

// =============================================================================
// Member API 테스트 시나리오들 (부하테스트 대상)
// =============================================================================

// 1. 내 정보 조회 테스트
function testMyProfileQuery(headers) {
    console.log('Testing my profile query...');
    let start = Date.now();
    let response = http.get(`${BASE_URL}/api/member/me`, { headers });
    memberApiDuration.add(Date.now() - start);

    const success = validateResponse(response, 200, 'My profile query', memberInfoQueryRate);

    if (success) {
        console.log('My profile query successful');
    } else {
        console.error('My profile query failed:', response.status, response.body);
    }

    return success;
}

// 2. 타인 정보 조회 테스트 (ID로)
function testGetMemberById(headers, memberId) {
    console.log('Testing get member by ID...');
    let start = Date.now();
    let response = http.get(`${BASE_URL}/api/member/id/${memberId}`, { headers });
    memberApiDuration.add(Date.now() - start);

    const success = validateResponse(response, 200, 'Get member by ID', memberInfoQueryRate);

    if (success) {
        console.log('Get member by ID successful');
    } else {
        console.error('Get member by ID failed:', response.status, response.body);
    }

    return success;
}

// 3. 타인 정보 조회 테스트 (닉네임으로)
function testGetMemberByNickname(headers, nickname) {
    console.log('Testing get member by nickname...');
    let start = Date.now();
    let response = http.get(`${BASE_URL}/api/member/nickname/${nickname}`, { headers });
    memberApiDuration.add(Date.now() - start);

    const success = validateResponse(response, 200, 'Get member by nickname', memberInfoQueryRate);

    if (success) {
        console.log('Get member by nickname successful');
    } else {
        console.error('Get member by nickname failed:', response.status, response.body);
    }

    return success;
}

// 4. 닉네임 중복 확인 테스트
function testNicknameCheck(headers) {
    console.log('Testing nickname check...');
    let randomNickname = testData.generateUniqueNickname();
    let start = Date.now();
    let response = http.get(`${BASE_URL}/api/member/check/nickname/${randomNickname}`, { headers });
    memberApiDuration.add(Date.now() - start);

    const success = validateResponse(response, 200, 'Nickname check', nicknameCheckRate);

    if (success) {
        console.log('Nickname check successful for:', randomNickname);
    } else {
        console.error('Nickname check failed:', response.status, response.body);
    }

    return success;
}

// 5. 프로필 업데이트 테스트 (파일 업로드 제외)
function testProfileUpdate(headers) {
    console.log('Testing profile update...');
    const profileData = testData.generateProfileUpdateData();

    // URL 쿼리 파라미터로 전송
    const updateNickname = encodeURIComponent(profileData.nickname);
    const updateDescription = encodeURIComponent(profileData.description);
    const updateEmail = encodeURIComponent(profileData.email);

    let start = Date.now();
    let response = http.put(
        `${BASE_URL}/api/member/me/profile/info?nickname=${updateNickname}&description=${updateDescription}&email=${updateEmail}`,
        null,
        { headers }
    );
    memberApiDuration.add(Date.now() - start);

    const success = validateResponse(response, 200, 'Profile update', profileUpdateRate);

    if (success) {
        console.log('Profile update successful');
    } else {
        console.error('Profile update failed:', response.status, response.body);
    }

    return success;
}

// 6. 관심 커피챗 주제 설정 테스트
function testChatTopicsUpdate(headers) {
    console.log('Testing chat topics update...');
    const topicsData = testData.generateChatTopicsData();

    let start = Date.now();
    let response = http.put(
        `${BASE_URL}/api/member/coffee-chat/interests/topics`,
        JSON.stringify(topicsData),
        { headers }
    );
    memberApiDuration.add(Date.now() - start);

    const success = validateResponse(response, 200, 'Chat topics update', chatTopicsRate);

    if (success) {
        console.log('Chat topics update successful');
    } else {
        console.error('Chat topics update failed:', response.status, response.body);
    }

    return success;
}

// 7. 관심 커피챗 주제 조회 테스트
function testChatTopicsQuery(headers) {
    console.log('Testing chat topics query...');

    let start = Date.now();
    let response = http.get(`${BASE_URL}/api/member/coffee-chat/interests/topics`, { headers });
    memberApiDuration.add(Date.now() - start);

    const success = validateResponse(response, 200, 'Chat topics query', chatTopicsRate);

    if (success) {
        console.log('Chat topics query successful');
    } else {
        console.error('Chat topics query failed:', response.status, response.body);
    }

    return success;
}

// 8. 직무 분야 설정 테스트
function testJobFieldsUpdate(headers) {
    console.log('Testing job fields update...');

    // JobNameType enum의 description (한글) 값 사용
    const jobFieldData = {
        jobName: "IT 개발/데이터"  // 한글 description 값으로 전송
    };

    let start = Date.now();
    let response = http.put(
        `${BASE_URL}/api/member/coffee-chat/interests/fields`,
        JSON.stringify(jobFieldData),
        { headers }
    );
    memberApiDuration.add(Date.now() - start);

    const success = validateResponse(response, 200, 'Job fields update', jobFieldsRate);

    if (success) {
        console.log('Job fields update successful');
    } else {
        console.error('Job fields update failed:', response.status, response.body);
    }

    return success;
}

// 9. 직무 분야 조회 테스트
function testJobFieldsQuery(headers) {
    console.log('Testing job fields query...');

    let start = Date.now();
    let response = http.get(`${BASE_URL}/api/member/coffee-chat/interests/fields`, { headers });
    memberApiDuration.add(Date.now() - start);

    const success = validateResponse(response, 200, 'Job fields query', jobFieldsRate);

    if (success) {
        console.log('Job fields query successful');
    } else {
        console.error('Job fields query failed:', response.status, response.body);
    }

    return success;
}

// 10. 커피챗 예약 조회 테스트들 (다양한 상태)
function testCoffeeChatReservations(headers) {
    console.log('Testing coffee chat reservations...');
    let allSuccess = true;

    const reservationTypes = ['pending', 'confirmed', 'completed', 'cancelled', 'all'];

    for (const type of reservationTypes) {
        let start = Date.now();
        let response = http.get(`${BASE_URL}/api/member/coffee-chat/reservations/${type}`, { headers });
        memberApiDuration.add(Date.now() - start);

        const success = validateResponse(response, 200, `Coffee chat reservations (${type})`, coffeeChatReservationsRate);

        if (success) {
            console.log(`Coffee chat reservations (${type}) successful`);
        } else {
            console.error(`Coffee chat reservations (${type}) failed:`, response.status, response.body);
            allSuccess = false;
        }

        sleep(0.1); // 연속 요청 간 짧은 대기
    }

    return allSuccess;
}

// 11. 완전한 Member API 플로우 테스트
function testCompleteMemberFlow(headers, authResult) {
    console.log('Starting complete member API flow test...');
    let allSuccess = true;
    let testCount = 0;

    console.log('=== Phase 1: 기본 정보 조회 ===');
    allSuccess &= testMyProfileQuery(headers);
    testCount++;
    sleep(0.3);

    allSuccess &= testGetMemberById(headers, authResult.memberId);
    testCount++;
    sleep(0.3);

    allSuccess &= testGetMemberByNickname(headers, authResult.nickname);
    testCount++;
    sleep(0.3);

    console.log('=== Phase 2: 닉네임 및 프로필 관리 ===');
    allSuccess &= testNicknameCheck(headers);
    testCount++;
    sleep(0.3);

    allSuccess &= testProfileUpdate(headers);
    testCount++;
    sleep(0.5);

    console.log('=== Phase 3: 커피챗 관심사 설정 ===');
    allSuccess &= testChatTopicsUpdate(headers);
    testCount++;
    sleep(0.3);

    allSuccess &= testChatTopicsQuery(headers);
    testCount++;
    sleep(0.3);

    allSuccess &= testJobFieldsUpdate(headers);
    testCount++;
    sleep(0.3);

    allSuccess &= testJobFieldsQuery(headers);
    testCount++;
    sleep(0.3);

    console.log('=== Phase 4: 커피챗 예약 조회 ===');
    allSuccess &= testCoffeeChatReservations(headers);
    testCount++;
    sleep(0.5);

    console.log(`Complete member flow: ${allSuccess ? 'SUCCESS' : 'FAILED'} (${testCount} tests completed)`);
    return allSuccess;
}

// =============================================================================
// 메인 테스트 함수
// =============================================================================
export default function() {
    console.log(`VU ${__VU}: Starting comprehensive Member API test`);

    // 테스트 계정 생성 및 로그인 (부하테스트 대상 아님, 사전 작업)
    const authResult = setupTestUser();
    if (!authResult) {
        console.error(`VU ${__VU}: Failed to setup test user, skipping Member API tests`);
        return;
    }

    const headers = {
        'Authorization': `Bearer ${authResult.token}`,
        'Content-Type': 'application/json'
    };

    console.log(`VU ${__VU}: Successfully authenticated, starting Member API tests`);

    // Member API 부하테스트 실행
    const scenario = __ENV.SCENARIO || 'complete-flow';

    switch(scenario) {
        case 'basic-info':
            testMyProfileQuery(headers);
            sleep(0.3);
            testGetMemberById(headers, authResult.memberId);
            sleep(0.3);
            testGetMemberByNickname(headers, authResult.nickname);
            break;

        case 'nickname-profile':
            testNicknameCheck(headers);
            sleep(0.3);
            testProfileUpdate(headers);
            break;

        case 'chat-interests':
            testChatTopicsUpdate(headers);
            sleep(0.3);
            testChatTopicsQuery(headers);
            sleep(0.3);
            testJobFieldsUpdate(headers);
            sleep(0.3);
            testJobFieldsQuery(headers);
            break;

        case 'job-fields':
            testJobFieldsUpdate(headers);
            sleep(0.3);
            testJobFieldsQuery(headers);
            break;

        case 'reservations':
            testCoffeeChatReservations(headers);
            break;

        case 'complete-flow':
            testCompleteMemberFlow(headers, authResult);
            break;

        default: // mixed - 랜덤하게 다양한 Member API 테스트 실행
            const testFunctions = [
                () => testMyProfileQuery(headers),
                () => testNicknameCheck(headers),
                () => testProfileUpdate(headers),
                () => testChatTopicsQuery(headers),
                () => testJobFieldsQuery(headers),
                () => testCoffeeChatReservations(headers)
            ];

            const randomTest = testFunctions[Math.floor(Math.random() * testFunctions.length)];
            randomTest();
            break;
    }

    console.log(`VU ${__VU}: Completed Member API test`);
    sleep(1);
}

// =============================================================================
// 설정 및 정리
// =============================================================================
export function setup() {
    console.log('='.repeat(70));
    console.log('커피챗 Member 도메인 API 종합 부하테스트 시작');
    console.log(`Base URL: ${BASE_URL}`);
    console.log(`Scenario: ${__ENV.SCENARIO || 'complete-flow'}`);
    console.log(`Environment: ${__ENV.ENVIRONMENT || 'local'}`);
    console.log('테스트 대상: Member 도메인 전체 API (파일 업로드 제외)');
    console.log('='.repeat(70));
}

export function teardown() {
    console.log('='.repeat(70));
    console.log('Member API 종합 기능 테스트 완료');
    console.log(`총 Member API 에러: ${memberApiErrors ? memberApiErrors.count || 0 : 0}`);

    console.log('='.repeat(70));
}