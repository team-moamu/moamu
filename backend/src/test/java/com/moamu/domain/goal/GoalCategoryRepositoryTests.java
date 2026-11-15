package com.moamu.domain.goal;

import com.moamu.domain.goal.entity.GoalCategory;
import com.moamu.domain.goal.entity.GoalTemplate;
import com.moamu.domain.artwork.entity.Artwork;
import com.moamu.domain.artwork.repository.ArtworkRepository;
import com.moamu.domain.goal.repository.GoalCategoryRepository;
import com.moamu.domain.goal.repository.GoalTemplateRepository;
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
                .addAnnotatedClass(GoalTemplate.class)
                .addAnnotatedClass(Artwork.class)
                .buildMetadata().buildSessionFactory();
                var session = sessionFactory.openSession()) {
            var query = session.createQuery(jpql, Long.class);

            assertThat(query.getParameters()).extracting(parameter -> parameter.getName())
                    .containsExactlyInAnyOrder("majorName", "middleName", "minorName");

            String confirmationQuery = GoalCategoryRepository.class
                    .getMethod("findForConfirmation", Long.class, String.class).getAnnotation(Query.class).value();
            assertThat(session.createQuery(confirmationQuery, GoalCategory.class).getParameters()).hasSize(2);

            String restoreQuery = GoalCategoryRepository.class
                    .getMethod("restore", Long.class).getAnnotation(Query.class).value();
            assertThat(session.createMutationQuery(restoreQuery).setParameter("cateId", 1L)).isNotNull();

            String templateQuery = GoalTemplateRepository.class
                    .getMethod("findReusableTemplates", Long.class, String.class, String.class, String.class)
                    .getAnnotation(Query.class).value();
            assertThat(session.createQuery(templateQuery, GoalTemplate.class).getParameters()).hasSize(4);

            String artworkQuery = ArtworkRepository.class.getMethod("findReadyArtworks").getAnnotation(Query.class).value();
            assertThat(session.createQuery(artworkQuery, Artwork.class).getParameters()).isEmpty();
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}
