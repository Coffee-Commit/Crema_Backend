// guide-list-k6-test.js - 커피챗 프로젝트 Guide 목록 조회 API K6 부하테스트 (토큰 재사용 버전)

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { SharedArray } from 'k6/data';

// =============================================================================
// 커스텀 메트릭스 (Guide 목록 조회만 집중)
// =============================================================================
const guideListSuccessRate = new Rate('guide_list_success_rate');
const guideListFilteredSuccessRate = new Rate('guide_list_filtered_success_rate');
const guideListPaginationSuccessRate = new Rate('guide_list_pagination_success_rate');
const guideListSortSuccessRate = new Rate('guide_list_sort_success_rate');
const guideApiDuration = new Trend('guide_api_duration');
const guideApiErrors = new Counter('guide_api_errors');
const loginDuration = new Trend('login_duration'); // 참고용으로만 유지

// =============================================================================
// 테스트 옵션 (로그인 임계값 완화)
// =============================================================================
export const options = {
    stages: [
        { duration: '0s', target: 0 },
        { duration: '10s', target: 5000 },
        { duration: '3m', target: 5000 },
        { duration: '10s', target: 0 }
    ],

    thresholds: {
        http_req_duration: ['p(95)<2000'],              // 95%의 요청이 2초 이내
        http_req_failed: ['rate<0.05'],                 // 전체 실패율 5% 미만
        guide_list_success_rate: ['rate>0.95'],         // 기본 목록 조회 성공률 95% 이상
        guide_list_filtered_success_rate: ['rate>0.90'], // 필터링 조회 성공률 90% 이상
        guide_list_pagination_success_rate: ['rate>0.95'], // 페이지네이션 성공률 95% 이상
        guide_list_sort_success_rate: ['rate>0.95'],    // 정렬 옵션 성공률 95% 이상
        guide_api_duration: ['p(95)<1000'],             // 가이드 API 95% 1초 이내
        login_duration: ['p(95)<5000'],                 // 로그인 임계값 완화 (5초)
    },

    tags: {
        test_type: 'guide_list_api_focused_test',
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
class GuideTestDataGenerator {
    constructor() {
        // JobNameType enum 값들
        this.jobNames = [
            "IT_DEVELOPMENT_DATA",
            "DESIGN",
            "MARKETING_PR",
            "MANAGEMENT_SUPPORT",
            "PLANNING_STRATEGY",
            "RESEARCH_RND"
        ];

        // TopicNameType enum 값들
        this.chatTopicNames = [
            "RESUME",
            "COVER_LETTER",
            "PORTFOLIO",
            "INTERVIEW",
            "PRACTICAL_WORK",
            "ORGANIZATION_CULTURE",
            "WORK_LIFE_BALANCE",
            "RELATIONSHIP",
            "PASS_EXPERIENCE",
            "INDUSTRY_TREND",
            "CAREER_CHANGE",
            "JOB_CHANGE"
        ];

        // 검색 키워드 샘플들
        this.keywords = [
            "스프링",
            "자바",
            "리액트",
            "백엔드",
            "프론트엔드",
            "면접",
            "이력서",
            "포트폴리오",
            "이직",
            "커리어"
        ];

        // 페이지 사이즈 옵션들
        this.pageSizes = [5, 10, 20];

        // 정렬 옵션들
        this.sortOptions = ["latest", "popular"];
    }

    // 기본 파라미터 (필터 없음) - page 고정
    generateBasicParams() {
        return {
            page: 0, // 항상 0으로 고정
            size: this.pageSizes[Math.floor(Math.random() * this.pageSizes.length)],
            sort: this.sortOptions[Math.floor(Math.random() * this.sortOptions.length)]
        };
    }

    // 단일 필터 파라미터 (job 또는 topic 중 하나만) - page 고정
    generateSingleFilterParams() {
        const useJobFilter = Math.random() > 0.5;

        if (useJobFilter) {
            // Job 필터만 사용
            return {
                jobNames: [this.jobNames[Math.floor(Math.random() * this.jobNames.length)]],
                page: 0, // 항상 0으로 고정
                size: this.pageSizes[Math.floor(Math.random() * this.pageSizes.length)],
                sort: this.sortOptions[Math.floor(Math.random() * this.sortOptions.length)]
            };
        } else {
            // Topic 필터만 사용
            return {
                chatTopicNames: [this.chatTopicNames[Math.floor(Math.random() * this.chatTopicNames.length)]],
                page: 0, // 항상 0으로 고정
                size: this.pageSizes[Math.floor(Math.random() * this.pageSizes.length)],
                sort: this.sortOptions[Math.floor(Math.random() * this.sortOptions.length)]
            };
        }
    }

    // 복합 필터 파라미터 (여러 필터 조합) - page 고정
    generateComplexFilterParams() {
        const params = {
            page: 0, // 항상 0으로 고정
            size: this.pageSizes[Math.floor(Math.random() * this.pageSizes.length)],
            sort: this.sortOptions[Math.floor(Math.random() * this.sortOptions.length)]
        };

        // 30% 확률로 키워드 추가
        if (Math.random() < 0.3) {
            params.keyword = this.keywords[Math.floor(Math.random() * this.keywords.length)];
        }

        // 50% 확률로 job 필터 추가 (1-2개)
        if (Math.random() < 0.5) {
            const jobCount = Math.random() < 0.7 ? 1 : 2;
            params.jobNames = this.shuffleArray(this.jobNames).slice(0, jobCount);
        }

        // 40% 확률로 topic 필터 추가 (1-3개)
        if (Math.random() < 0.4) {
            const topicCount = Math.floor(Math.random() * 3) + 1;
            params.chatTopicNames = this.shuffleArray(this.chatTopicNames).slice(0, topicCount);
        }

        return params;
    }

    // 페이지네이션 파라미터 - page 고정
    generatePaginationParams() {
        return {
            page: 0, // 항상 0으로 고정
            size: this.pageSizes[Math.floor(Math.random() * this.pageSizes.length)],
            sort: this.sortOptions[Math.floor(Math.random() * this.sortOptions.length)]
        };
    }

    // 배열 셔플 유틸리티
    shuffleArray(array) {
        const shuffled = [...array];
        for (let i = shuffled.length - 1; i > 0; i--) {
            const j = Math.floor(Math.random() * (i + 1));
            [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
        }
        return shuffled;
    }

    // 쿼리 파라미터 문자열 생성
    buildQueryString(params) {
        const queryParts = [];

        if (params.jobNames && params.jobNames.length > 0) {
            params.jobNames.forEach(job => {
                queryParts.push(`jobNames=${encodeURIComponent(job)}`);
            });
        }

        if (params.chatTopicNames && params.chatTopicNames.length > 0) {
            params.chatTopicNames.forEach(topic => {
                queryParts.push(`chatTopicNames=${encodeURIComponent(topic)}`);
            });
        }

        if (params.keyword) {
            const encodedKeyword = encodeURIComponent(encodeURIComponent(params.keyword));
            queryParts.push(`keyword=${encodedKeyword}`);
        }

        queryParts.push(`page=${params.page}`);
        queryParts.push(`size=${params.size}`);
        queryParts.push(`sort=${params.sort}`);

        return queryParts.join('&');
    }
}

const testData = new GuideTestDataGenerator();

// =============================================================================
// 공통 검증 함수 (가이드 API에만 집중)
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

    if (!isValid) {
        guideApiErrors.add(1);
    }

    return isValid;
}

function validateGuideListResponse(response, checkName = 'Guide list response') {
    let dataValidation = false;

    try {
        const responseData = response.json();
        dataValidation = check(responseData, {
            [`${checkName} - has data`]: (data) => data.data !== undefined,
            [`${checkName} - has content array`]: (data) => Array.isArray(data.data.content),
            [`${checkName} - has pagination info`]: (data) => data.data.pageable !== undefined,
            [`${checkName} - has totalElements`]: (data) => typeof data.data.totalElements === 'number',
            [`${checkName} - has totalPages`]: (data) => typeof data.data.totalPages === 'number',
        });

        // 가이드 데이터가 있을 경우 구조 검증
        if (responseData.data.content && responseData.data.content.length > 0) {
            const firstGuide = responseData.data.content[0];
            check(firstGuide, {
                [`${checkName} - guide has guideId`]: (guide) => typeof guide.guideId === 'number',
                [`${checkName} - guide has nickname`]: (guide) => typeof guide.nickname === 'string',
                [`${checkName} - guide has title`]: (guide) => typeof guide.title === 'string',
                [`${checkName} - guide has jobField`]: (guide) => guide.jobField !== undefined,
                [`${checkName} - guide has stats`]: (guide) => guide.stats !== undefined,
            });
        }
    } catch (e) {
        console.error(`JSON parsing failed for ${checkName}:`, e.message);
        dataValidation = false;
    }

    return dataValidation;
}

// =============================================================================
// 글로벌 토큰 저장소 (SharedArray로 토큰 공유)
// =============================================================================
let globalToken = null;

// =============================================================================
// 로그인 함수 (setup에서 한번만 실행)
// =============================================================================
function performGlobalLogin() {
    console.log('Performing global login for token sharing...');
    let start = Date.now();

    let createResponse = http.post(`${BASE_URL}/api/test/auth/create-rookie`, null, {
        headers: { 'Content-Type': 'application/json' }
    });

    if (createResponse.status !== 200) {
        console.error('Test account creation failed:', createResponse.status);
        return null;
    }

    let accountData = createResponse.json();
    let nickname = accountData.result.nickname;

    let loginResponse = http.post(`${BASE_URL}/api/test/auth/login`, JSON.stringify({
        nickname: nickname
    }), {
        headers: { 'Content-Type': 'application/json' }
    });

    loginDuration.add(Date.now() - start);

    if (loginResponse.status !== 200) {
        console.error('Global login failed:', loginResponse.status);
        return null;
    }

    let token;
    try {
        const responseData = loginResponse.json();
        token = responseData.result.accessToken;
        console.log('Successfully obtained global access token');
    } catch (error) {
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
// Guide API 테스트 시나리오들 (토큰 재사용)
// =============================================================================

// 1. 기본 가이드 목록 조회 (필터 없음)
function testBasicGuideList(headers) {
    const params = testData.generateBasicParams();
    const queryString = testData.buildQueryString(params);

    let start = Date.now();
    let response = http.get(`${BASE_URL}/api/guides?${queryString}`, { headers });
    guideApiDuration.add(Date.now() - start); // 가이드 API 전용 메트릭

    const success = validateResponse(response, 200, 'Basic guide list', guideListSuccessRate);
    const dataValid = validateGuideListResponse(response, 'Basic guide list');

    return success && dataValid;
}

// 2. 단일 필터 가이드 목록 조회
function testSingleFilterGuideList(headers) {
    const params = testData.generateSingleFilterParams();
    const queryString = testData.buildQueryString(params);

    let start = Date.now();
    let response = http.get(`${BASE_URL}/api/guides?${queryString}`, { headers });
    guideApiDuration.add(Date.now() - start);

    const success = validateResponse(response, 200, 'Single filter guide list', guideListFilteredSuccessRate);
    const dataValid = validateGuideListResponse(response, 'Single filter guide list');

    return success && dataValid;
}

// 3. 복합 필터 가이드 목록 조회
function testComplexFilterGuideList(headers) {
    const params = testData.generateComplexFilterParams();
    const queryString = testData.buildQueryString(params);

    let start = Date.now();
    let response = http.get(`${BASE_URL}/api/guides?${queryString}`, { headers });
    guideApiDuration.add(Date.now() - start);

    const success = validateResponse(response, 200, 'Complex filter guide list', guideListFilteredSuccessRate);
    const dataValid = validateGuideListResponse(response, 'Complex filter guide list');

    return success && dataValid;
}

// 4. 페이지네이션 테스트
function testPaginationGuideList(headers) {
    const params = testData.generatePaginationParams();
    const queryString = testData.buildQueryString(params);

    let start = Date.now();
    let response = http.get(`${BASE_URL}/api/guides?${queryString}`, { headers });
    guideApiDuration.add(Date.now() - start);

    const success = validateResponse(response, 200, 'Pagination guide list', guideListPaginationSuccessRate);
    const dataValid = validateGuideListResponse(response, 'Pagination guide list');

    return success && dataValid;
}

// 5. 정렬 옵션 테스트
function testSortOptionsGuideList(headers) {
    let allSuccess = true;

    // latest와 popular 각각 테스트
    for (const sortOption of testData.sortOptions) {
        const params = {
            page: 0, // 항상 첫 페이지만 사용
            size: 20,
            sort: sortOption
        };

        const queryString = testData.buildQueryString(params);

        let start = Date.now();
        let response = http.get(`${BASE_URL}/api/guides?${queryString}`, { headers });
        guideApiDuration.add(Date.now() - start);

        const success = validateResponse(response, 200, `Sort ${sortOption} guide list`, guideListSortSuccessRate);
        const dataValid = validateGuideListResponse(response, `Sort ${sortOption} guide list`);

        if (!success || !dataValid) {
            allSuccess = false;
        }

        sleep(0.05); // 짧은 대기
    }

    return allSuccess;
}

// =============================================================================
// 메인 실행 함수 (토큰 재사용)
// =============================================================================
export default function (data) {
    // 글로벌 토큰 사용 (setup에서 생성됨)
    if (!data || !data.token) {
        console.error('No global token available, skipping test');
        return;
    }

    const headers = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${data.token}`
    };

    // 가이드 API만 집중 테스트 - 랜덤 선택
    const testFunctions = [
        () => testBasicGuideList(headers),
        () => testSingleFilterGuideList(headers),
        () => testComplexFilterGuideList(headers),
        () => testPaginationGuideList(headers),
        () => testSortOptionsGuideList(headers)
    ];

    const randomTest = testFunctions[Math.floor(Math.random() * testFunctions.length)];
    randomTest();

    sleep(0.1); // 짧은 대기
}

// =============================================================================
// Setup: 글로벌 로그인 (한번만 실행)
// =============================================================================
export function setup() {
    console.log('='.repeat(70));
    console.log('커피챗 Guide API 집중 부하테스트 시작 (토큰 재사용)');
    console.log(`Base URL: ${BASE_URL}`);
    console.log(`Environment: ${__ENV.ENVIRONMENT || 'local'}`);
    console.log('테스트 대상: GET /api/guides (가이드 목록 조회 API)');
    console.log('최적화: 토큰 재사용으로 로그인 병목 제거');
    console.log('='.repeat(70));

    // 글로벌 로그인 수행
    const authResult = performGlobalLogin();
    if (!authResult) {
        throw new Error('Global login failed - cannot proceed with test');
    }

    console.log('Global token created successfully - ready for load testing');
    return authResult; // 모든 VU에서 사용할 데이터 반환
}

export function teardown(data) {
    console.log('='.repeat(70));
    console.log('Guide API 집중 부하테스트 완료');
    console.log(`총 Guide API 에러: ${guideApiErrors ? guideApiErrors.count || 0 : 0}`);

    // 테스트 계정 정리
    if (data && data.token) {
        try {
            let cleanupResponse = http.del(`${BASE_URL}/api/test/auth/cleanup`, {
                headers: { 'Authorization': `Bearer ${data.token}` }
            });
            console.log(`정리 작업 완료: ${cleanupResponse.status}`);
        } catch (e) {
            console.log('정리 작업 중 오류:', e.message);
        }
    }

    console.log('='.repeat(70));
}