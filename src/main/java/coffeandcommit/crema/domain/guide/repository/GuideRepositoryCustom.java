package coffeandcommit.crema.domain.guide.repository;

import coffeandcommit.crema.domain.globalTag.enums.JobNameType;
import coffeandcommit.crema.domain.globalTag.enums.TopicNameType;
import coffeandcommit.crema.domain.guide.entity.Guide;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface GuideRepositoryCustom {

    Page<Guide> findBySearchConditions(
            List<JobNameType> jobNames,
            List<TopicNameType> chatTopicNames,
            String keyword,
            Pageable pageable
    );
}

