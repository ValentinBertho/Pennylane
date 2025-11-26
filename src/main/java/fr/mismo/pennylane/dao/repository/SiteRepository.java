package fr.mismo.pennylane.dao.repository;

import fr.mismo.pennylane.dao.entity.SiteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, String> {
    List<SiteEntity> findAllByPennylaneActifTrue();
    List<SiteEntity> findAllByPennylaneAchatTrue();

}
