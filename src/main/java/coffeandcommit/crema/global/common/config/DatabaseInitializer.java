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

            boolean hadFailure = false;
            for (EntityType<?> entityType : entities) {
                try {
                    String entityName = entityType.getName();
                    var q = entityManager.createQuery("SELECT 1 FROM " + entityName + " e");
                    // 가벼운 존재 확인: 레코드 1건만 조회(없어도 OK). 테이블이 없으면 예외 발생.
                    q.setMaxResults(1);
                    q.getResultList();
                    log.debug("테이블 확인 완료: {}", entityName);
                    tableCount++;

                } catch (Exception e) {
                    hadFailure = true;
                    log.warn("엔티티 {} 테이블 확인 실패: {}", entityType.getName(), e.getMessage());
                }
            }

            log.info("데이터베이스 테이블 초기화 완료: 총 {}개 엔티티 처리", tableCount);
            if (hadFailure) {throw new IllegalStateException("일부 엔티티 테이블이 없거나 접근할 수 없습니다. (local/test에서 ddl-auto 또는 마이그레이션 적용 필요)");}

        } catch (Exception e) {
            log.error("데이터베이스 초기화 실패: {}", e.getMessage(), e);
            // 예외를 다시 던져서 애플리케이션 시작을 중단시킴
            throw new RuntimeException("데이터베이스 초기화에 실패했습니다.", e);
        }
    }
}