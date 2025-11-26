package fr.mismo.pennylane.dao.repository;

import fr.mismo.pennylane.dao.entity.SynchroEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SynchroRepository extends CrudRepository<SynchroEntity, Integer> {

}
