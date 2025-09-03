package coffeandcommit.crema.global.common.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

/**
* 애플리케이션 시작 시(local/test) 모든 JPA 엔티티의 테이블 “존재 여부”를 검증하고,
* 누락된 테이블이 있으면 빠르게 실패하여 환경 설정(ddl-auto/update 또는 마이그레이션) 누락을 드러내는 컴포넌트입니다.
*
* 실제 테이블 “생성”은 local/test 프로파일의 ddl-auto(update) 또는 Flyway/Liquibase에 위임해야 합니다.
* 각 엔티티에 대해 경량 쿼리를 실행해 테이블 존재 여부를 확인합니다(없으면 예외 발생)
*/
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInitializer {

    private final EntityManager entityManager;

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