package coffeandcommit.crema.global.common.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

/**
 * 애플리케이션 시작 시 모든 JPA 엔티티의 테이블을 생성하는 초기화 컴포넌트
 *
 * OAuth2 회원가입 시 Member 테이블만 생성되는 문제를 해결하기 위해
 * 애플리케이션 시작 시점에 모든 엔티티 클래스를 스캔하여 테이블을 미리 생성합니다.
 *
 * 각 엔티티에 대해 COUNT 쿼리를 실행하여 Hibernate가 해당 테이블을 강제로 생성하도록 합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInitializer {

    private final EntityManager entityManager;

    /**
     * 애플리케이션 시작 시 모든 엔티티의 테이블을 생성합니다.
     *
     * JPA의 Metamodel을 통해 모든 엔티티 클래스를 스캔하고,
     * 각 엔티티에 대해 COUNT 쿼리를 실행하여 Hibernate가 해당하는 테이블들을 자동으로 생성하도록 합니다.
     */
    @PostConstruct
    @Transactional
    public void initializeTables() {
        try {
            log.info("데이터베이스 테이블 초기화 시작...");

            // EntityManagerFactory의 Metamodel을 통해 모든 엔티티 스캔
            var entities = entityManager.getEntityManagerFactory().getMetamodel().getEntities();

            int tableCount = 0;

            // 각 엔티티에 대해 COUNT 쿼리 실행하여 테이블 생성 강제
            for (EntityType<?> entityType : entities) {
                try {
                    String entityName = entityType.getName();
                    String jpql = "SELECT COUNT(e) FROM " + entityName + " e";

                    // COUNT 쿼리 실행 - 이 과정에서 Hibernate가 테이블이 없으면 생성함
                    Long count = entityManager.createQuery(jpql, Long.class).getSingleResult();

                    log.debug("테이블 초기화 완료: {} (현재 데이터 개수: {})", entityName, count);
                    tableCount++;

                } catch (Exception e) {
                    // 개별 엔티티 초기화 실패 시에도 다른 엔티티는 계속 처리
                    log.warn("엔티티 {} 테이블 초기화 실패: {}", entityType.getName(), e.getMessage());
                }
            }

            log.info("데이터베이스 테이블 초기화 완료: 총 {}개 엔티티 처리", tableCount);

        } catch (Exception e) {
            log.error("데이터베이스 초기화 실패: {}", e.getMessage(), e);
            // 예외를 다시 던져서 애플리케이션 시작을 중단시킴
            throw new RuntimeException("데이터베이스 초기화에 실패했습니다.", e);
        }
    }
}