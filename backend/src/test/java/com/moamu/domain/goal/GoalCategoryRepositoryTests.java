package com.moamu.domain.goal;

import com.moamu.domain.goal.entity.GoalCategory;
import com.moamu.domain.goal.repository.GoalCategoryRepository;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import static org.assertj.core.api.Assertions.assertThat;

class GoalCategoryRepositoryTests {

    // DB 접속 없이 엔티티 매핑을 기준으로 JPQL 구문과 속성 참조 검증
    @Test
    void validatesCategoryPathQueryAgainstEntityMapping() throws Exception {
        String jpql = GoalCategoryRepository.class
                .getMethod("findActiveMinorId", String.class, String.class, String.class)
                .getAnnotation(Query.class).value();

        var registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
                .applySetting("hibernate.dialect", "org.hibernate.dialect.MySQLDialect")
                .applySetting("hibernate.hbm2ddl.auto", "none")
                .build();

        try (var sessionFactory = new MetadataSources(registry)
                .addAnnotatedClass(GoalCategory.class)
                .buildMetadata().buildSessionFactory();
                var session = sessionFactory.openSession()) {
            var query = session.createQuery(jpql, Long.class);

            assertThat(query.getParameters()).extracting(parameter -> parameter.getName())
                    .containsExactlyInAnyOrder("majorName", "middleName", "minorName");
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}
