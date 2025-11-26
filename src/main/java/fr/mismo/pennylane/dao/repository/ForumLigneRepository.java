package fr.mismo.pennylane.dao.repository;

import fr.mismo.pennylane.dao.entity.ForumLigneEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ForumLigneRepository extends CrudRepository<ForumLigneEntity, Integer> {

}
