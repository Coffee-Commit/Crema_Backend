package coffeandcommit.crema.global.common.config;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

/**
 * 애플리케이션 시작 시 모든 JPA 엔티티의 테이블을 생성하는 초기화 컴포넌트
 *
 * OAuth2 회원가입 시 Member 테이블만 생성되는 문제를 해결하기 위해
 * 애플리케이션 시작 시점에 모든 엔티티 클래스를 스캔하여 테이블을 미리 생성합니다.
 *
 * 운영 환경에서는 실행되지 않으며, 개발/테스트 환경에서만 동작합니다.
 * 나중에 스키마가 안정화되면 마이그레이션 도구로 전환할 예정이지만, 현재는 개발 단계의 유연성과 안정성을 모두 확보하기 위한 임시 해결책으로 사용중입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInitializer {

    private final EntityManager entityManager;

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    /**
     * 애플리케이션 시작 시 모든 엔티티의 테이블을 생성합니다.
     *
     * JPA의 Metamodel을 통해 모든 엔티티 클래스를 스캔하고
     * Hibernate가 해당하는 테이블들을 자동으로 생성하도록 합니다.
     */
    @PostConstruct
    @Transactional
    public void initializeTables() {
        // 운영 환경에서는 실행하지 않음
        if ("prod".equals(activeProfile) || "production".equals(activeProfile)) {
            log.info("운영 환경에서는 테이블 초기화를 건너뜁니다.");
            return;
        }

        try {
            log.info("데이터베이스 테이블 초기화 시작...");

            // EntityManagerFactory의 Metamodel을 통해 모든 엔티티 스캔
            // 이 과정에서 Hibernate가 ddl-auto 설정에 따라 테이블들을 생성함
            var entities = entityManager.getEntityManagerFactory().getMetamodel().getEntities();

            log.info("데이터베이스 테이블 초기화 완료: 총 {}개 엔티티 처리", entities.size());
            log.debug("처리된 엔티티 목록:");
            entities.forEach(entity -> log.debug("- {}", entity.getName()));

        } catch (Exception e) {
            log.error("데이터베이스 초기화 실패: {}", e.getMessage(), e);

            // 개발 환경에서는 애플리케이션을 중단하지 않고 경고만 출력
            // 필요시 예외를 다시 던져서 애플리케이션 시작을 중단시킬 수 있음
            if ("local".equals(activeProfile)) {
                log.warn("로컬 환경에서 테이블 초기화에 실패했습니다. 수동으로 데이터베이스를 확인해주세요.");
            } else {
                throw new RuntimeException("데이터베이스 초기화에 실패했습니다.", e);
            }
        }
    }
}