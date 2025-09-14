import random
import time
from locust import HttpUser, task, between
import urllib.parse

class MemberLoadTestUser(HttpUser):
    wait_time = between(0.3, 1.0)  # Member API는 더 복잡하므로 간격을 늘림

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

        account_data = create_response.json()
        self.nickname = account_data["result"]["nickname"]

        # 2. 로그인
        login_data = {"nickname": self.nickname}
        login_response = self.client.post(
            "/api/test/auth/login",
            json=login_data,
            headers={"Content-Type": "application/json"}
        )

        if login_response.status_code == 200:
            result = login_response.json()["result"]
            self.token = result["accessToken"]
            self.member_id = result["memberId"]
            self.headers = {
                "Authorization": f"Bearer {self.token}",
                "Content-Type": "application/json"
            }
            print(f"Login successful for user: {self.nickname}")
        else:
            print(f"Login failed: {login_response.status_code}")

    @task(3)
    def get_my_profile(self):
        """내 정보 조회 (가장 많이 호출)"""
        with self.client.get("/api/member/me", headers=self.headers, catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Status: {response.status_code}")

    @task(2)
    def get_member_by_id(self):
        """타인 정보 조회 (ID로)"""
        with self.client.get(f"/api/member/id/{self.member_id}", headers=self.headers, catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Status: {response.status_code}")

    @task(2)
    def get_member_by_nickname(self):
        """타인 정보 조회 (닉네임으로)"""
        with self.client.get(f"/api/member/nickname/{self.nickname}", headers=self.headers, catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Status: {response.status_code}")

    @task(2)
    def check_nickname(self):
        """닉네임 중복 확인"""
        random_nickname = f"test_user_{random.randint(1, 9999)}"
        with self.client.get(f"/api/member/check/nickname/{random_nickname}", headers=self.headers, catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Status: {response.status_code}")

    @task(1)
    def update_profile(self):
        """프로필 업데이트"""
        profile_data = self.generate_profile_data()
        update_nickname = urllib.parse.quote(profile_data["nickname"])
        update_description = urllib.parse.quote(profile_data["description"])
        update_email = urllib.parse.quote(profile_data["email"])

        url = f"/api/member/me/profile/info?nickname={update_nickname}&description={update_description}&email={update_email}"

        with self.client.put(url, headers=self.headers, catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Status: {response.status_code}")

    @task(1)
    def update_chat_topics(self):
        """관심 커피챗 주제 설정"""
        topics_data = self.generate_chat_topics_data()

        with self.client.put(
            "/api/member/coffee-chat/interests/topics",
            json=topics_data,
            headers=self.headers,
            catch_response=True
        ) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Status: {response.status_code}")

    @task(2)
    def get_chat_topics(self):
        """관심 커피챗 주제 조회"""
        with self.client.get("/api/member/coffee-chat/interests/topics", headers=self.headers, catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Status: {response.status_code}")

    @task(1)
    def update_job_fields(self):
        """직무 분야 설정"""
        job_field_data = {"jobName": "IT 개발/데이터"}

        with self.client.put(
            "/api/member/coffee-chat/interests/fields",
            json=job_field_data,
            headers=self.headers,
            catch_response=True
        ) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Status: {response.status_code}")

    @task(2)
    def get_job_fields(self):
        """직무 분야 조회"""
        with self.client.get("/api/member/coffee-chat/interests/fields", headers=self.headers, catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Status: {response.status_code}")

    @task(1)
    def get_coffee_chat_reservations(self):
        """커피챗 예약 조회 (랜덤 상태)"""
        reservation_types = ['pending', 'confirmed', 'completed', 'cancelled', 'all']
        reservation_type = random.choice(reservation_types)

        with self.client.get(f"/api/member/coffee-chat/reservations/{reservation_type}", headers=self.headers, catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Status: {response.status_code}")

    def generate_profile_data(self):
        """프로필 업데이트용 데이터 생성"""
        timestamp = int(time.time())
        return {
            "nickname": f"LoadTest_{timestamp}_{random.randint(1, 1000)}",
            "description": "Locust 부하테스트로 생성된 프로필 설명입니다.",
            "email": f"loadtest.{timestamp}@crema-test.com"
        }

    def generate_chat_topics_data(self):
        """채팅 주제 데이터 생성"""
        all_topics = [
            "RESUME", "COVER_LETTER", "PORTFOLIO", "INTERVIEW",
            "PRACTICAL_WORK", "ORGANIZATION_CULTURE", "WORK_LIFE_BALANCE",
            "RELATIONSHIP", "PASS_EXPERIENCE", "INDUSTRY_TREND",
            "CAREER_CHANGE", "JOB_CHANGE"
        ]

        # 1-3개의 랜덤 주제 선택
        topic_count = random.randint(1, 3)
        selected_topics = random.sample(all_topics, topic_count)

        return {"topicNames": selected_topics}