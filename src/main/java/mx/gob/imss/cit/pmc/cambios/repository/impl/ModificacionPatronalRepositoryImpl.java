package mx.gob.imss.cit.pmc.cambios.repository.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import mx.gob.imss.cit.mspmccommons.dto.ModificacionPatronalDTO;
import mx.gob.imss.cit.pmc.cambios.repository.ModificacionPatronalRepository;

@Repository
public class ModificacionPatronalRepositoryImpl implements ModificacionPatronalRepository {

	@Autowired
	private MongoOperations mongoOperations;

	@Override
	public Optional<ModificacionPatronalDTO> findOneByCve(String cveIdModifPatron) {
		Query query = new Query(Criteria.where("cveIdModifPatron").is(Integer.valueOf(cveIdModifPatron)));
		ModificacionPatronalDTO d = this.mongoOperations.findOne(query, ModificacionPatronalDTO.class);

		return Optional.ofNullable(d);
	}

	@Override
	public Optional<List<ModificacionPatronalDTO>> findAll() {
		List<ModificacionPatronalDTO> d = this.mongoOperations.findAll(ModificacionPatronalDTO.class);

		return Optional.ofNullable(d);
	}
}
