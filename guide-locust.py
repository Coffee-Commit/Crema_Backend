import random
import time
from locust import HttpUser, task, between
import urllib.parse

class GuideLoadTestUser(HttpUser):
    wait_time = between(0.1, 0.5)  # 요청 간격 0.1~0.5초

    def on_start(self):
        """테스트 시작 시 로그인 수행"""
        self.login()

    def login(self):
        """테스트 계정 생성 및 로그인"""
        # 1. 루키 계정 생성
        create_response = self.client.post("/api/test/auth/create-rookie")
        if create_response.status_code != 200:
            print(f"Account creation failed: {create_response.status_code}")
            return

        nickname = create_response.json()["result"]["nickname"]

        # 2. 로그인
        login_data = {"nickname": nickname}
        login_response = self.client.post(
            "/api/test/auth/login",
            json=login_data,
            headers={"Content-Type": "application/json"}
        )

        if login_response.status_code == 200:
            self.token = login_response.json()["result"]["accessToken"]
            self.headers = {
                "Authorization": f"Bearer {self.token}",
                "Content-Type": "application/json"
            }
            print(f"Login successful for user: {nickname}")
        else:
            print(f"Login failed: {login_response.status_code}")

    @task(4)
    def basic_guide_list(self):
        """기본 가이드 목록 조회 (가장 많이 호출)"""
        params = self.generate_basic_params()
        with self.client.get(f"/api/guides?{params}", headers=self.headers, catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Status: {response.status_code}")

    @task(2)
    def single_filter_guide_list(self):
        """단일 필터 가이드 목록 조회"""
        params = self.generate_single_filter_params()
        with self.client.get(f"/api/guides?{params}", headers=self.headers, catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Status: {response.status_code}")

    @task(2)
    def complex_filter_guide_list(self):
        """복합 필터 가이드 목록 조회"""
        params = self.generate_complex_filter_params()
        with self.client.get(f"/api/guides?{params}", headers=self.headers, catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Status: {response.status_code}")

    @task(1)
    def pagination_guide_list(self):
        """페이지네이션 테스트 (첫 페이지만)"""
        params = self.generate_pagination_params()
        with self.client.get(f"/api/guides?{params}", headers=self.headers, catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Status: {response.status_code}")

    @task(1)
    def sort_options_guide_list(self):
        """정렬 옵션 테스트"""
        sort_option = random.choice(["latest", "popular"])
        params = f"page=0&size=20&sort={sort_option}"
        with self.client.get(f"/api/guides?{params}", headers=self.headers, catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Status: {response.status_code}")

    def generate_basic_params(self):
        """기본 파라미터 생성 (page=0 고정)"""
        size = random.choice([5, 10, 20])
        sort = random.choice(["latest", "popular"])
        return f"page=0&size={size}&sort={sort}"

    def generate_single_filter_params(self):
        """단일 필터 파라미터 생성"""
        job_names = [
            "IT_DEVELOPMENT_DATA", "DESIGN", "MARKETING_PR",
            "MANAGEMENT_SUPPORT", "PLANNING_STRATEGY", "RESEARCH_RND"
        ]

        topic_names = [
            "RESUME", "COVER_LETTER", "PORTFOLIO", "INTERVIEW",
            "PRACTICAL_WORK", "ORGANIZATION_CULTURE", "WORK_LIFE_BALANCE",
            "RELATIONSHIP", "PASS_EXPERIENCE", "INDUSTRY_TREND",
            "CAREER_CHANGE", "JOB_CHANGE"
        ]

        size = random.choice([5, 10, 20])
        sort = random.choice(["latest", "popular"])
        base_params = f"page=0&size={size}&sort={sort}"

        if random.choice([True, False]):
            # Job 필터
            job = random.choice(job_names)
            return f"{base_params}&jobNames={urllib.parse.quote(job)}"
        else:
            # Topic 필터
            topic = random.choice(topic_names)
            return f"{base_params}&chatTopicNames={urllib.parse.quote(topic)}"

    def generate_complex_filter_params(self):
        """복합 필터 파라미터 생성"""
        job_names = [
            "IT_DEVELOPMENT_DATA", "DESIGN", "MARKETING_PR",
            "MANAGEMENT_SUPPORT", "PLANNING_STRATEGY", "RESEARCH_RND"
        ]

        topic_names = [
            "RESUME", "COVER_LETTER", "PORTFOLIO", "INTERVIEW",
            "PRACTICAL_WORK", "ORGANIZATION_CULTURE", "WORK_LIFE_BALANCE",
            "RELATIONSHIP", "PASS_EXPERIENCE", "INDUSTRY_TREND",
            "CAREER_CHANGE", "JOB_CHANGE"
        ]

        keywords = [
            "스프링", "자바", "리액트", "백엔드", "프론트엔드",
            "면접", "이력서", "포트폴리오", "이직", "커리어"
        ]

        size = random.choice([5, 10, 20])
        sort = random.choice(["latest", "popular"])
        params = [f"page=0", f"size={size}", f"sort={sort}"]

        # 30% 확률로 키워드 추가
        if random.random() < 0.3:
            keyword = random.choice(keywords)
            params.append(f"keyword={urllib.parse.quote(keyword)}")

        # 50% 확률로 job 필터 추가
        if random.random() < 0.5:
            job_count = random.choice([1, 2])
            selected_jobs = random.sample(job_names, job_count)
            for job in selected_jobs:
                params.append(f"jobNames={urllib.parse.quote(job)}")

        # 40% 확률로 topic 필터 추가
        if random.random() < 0.4:
            topic_count = random.randint(1, 3)
            selected_topics = random.sample(topic_names, min(topic_count, len(topic_names)))
            for topic in selected_topics:
                params.append(f"chatTopicNames={urllib.parse.quote(topic)}")

        return "&".join(params)

    def generate_pagination_params(self):
        """페이지네이션 파라미터 생성 (page=0 고정)"""
        size = random.choice([5, 10, 20])
        sort = random.choice(["latest", "popular"])
        return f"page=0&size={size}&sort={sort}"