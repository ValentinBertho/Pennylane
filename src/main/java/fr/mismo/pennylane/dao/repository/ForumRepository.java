package fr.mismo.pennylane.dao.repository;

import fr.mismo.pennylane.dao.entity.ForumEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumRepository extends CrudRepository<ForumEntity, Integer> {

    @Query("FROM ForumEntity f WHERE f.noIncident = :noIncident")
    List<ForumEntity> findByNoIncident(@Param("noIncident") String noIncident);

}
