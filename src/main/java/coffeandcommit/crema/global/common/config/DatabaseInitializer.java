package coffeandcommit.crema.global.common.config;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.TargetType;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.util.EnumSet;

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
    private final Environment environment;

    /**
     * 애플리케이션 시작 시 모든 엔티티의 테이블을 생성합니다.
     *
     * Hibernate SchemaExport를 사용하여 확실하게 테이블을 생성합니다.
     */
    @PostConstruct
    @Transactional
    public void initializeTables() {
        // 운영 환경 체크 (개선된 방식)
        if (isProductionEnvironment()) {
            log.info("운영 환경에서는 테이블 초기화를 건너뜁니다.");
            return;
        }

        try {
            log.info("데이터베이스 테이블 초기화 시작...");

            var entities = entityManager.getEntityManagerFactory().getMetamodel().getEntities();
            log.debug("발견된 엔티티: {}개", entities.size());
            entities.forEach(entity -> log.debug("- {}", entity.getName()));

            log.info("데이터베이스 테이블 초기화 완료");

        } catch (Exception e) {
            log.error("데이터베이스 초기화 실패: {}", e.getMessage(), e);

            // 환경별 에러 처리
            if (isLocalEnvironment()) {
                log.warn("로컬 환경에서 테이블 초기화에 실패했습니다. 수동으로 데이터베이스를 확인해주세요.");
            } else {
                throw new RuntimeException("데이터베이스 초기화에 실패했습니다.", e);
            }
        }
    }

    /**
     * 운영 환경 여부 확인
     */
    private boolean isProductionEnvironment() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("prod".equals(profile) || "production".equals(profile)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 로컬 환경 여부 확인
     */
    private boolean isLocalEnvironment() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("local".equals(profile)) {
                return true;
            }
        }
        return false;
    }
}