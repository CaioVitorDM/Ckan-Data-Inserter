package ufrn.imd.br.ckandatainserter.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ufrn.imd.br.ckandatainserter.model.TaxeData;

@Repository
public interface TaxeDataRepository extends JpaRepository<TaxeData, Long> {
    Page<TaxeData> findAll(Pageable pageable);
}
