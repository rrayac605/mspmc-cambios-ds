package mx.gob.imss.cit.pmc.cambios.repository.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import mx.gob.imss.cit.mspmccommons.dto.TipoRiesgoDTO;
import mx.gob.imss.cit.pmc.cambios.repository.TipoRiesgoRepository;

@Repository
public class TipoRiesgoRepositoryImpl implements TipoRiesgoRepository {

	@Autowired
	private MongoOperations mongoOperations;

	@Override
	public Optional<TipoRiesgoDTO> findOneByCve(String cveTipoRiesgo) {
		Query query = new Query(Criteria.where("cveIdTipoRegistro").is(Integer.valueOf(cveTipoRiesgo)));
		TipoRiesgoDTO d = this.mongoOperations.findOne(query, TipoRiesgoDTO.class);

		return Optional.ofNullable(d);
	}

	@Override
	public Optional<List<TipoRiesgoDTO>> findAll() {
		List<TipoRiesgoDTO> d = this.mongoOperations.findAll(TipoRiesgoDTO.class);

		return Optional.ofNullable(d);
	}

}
