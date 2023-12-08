package mx.gob.imss.cit.pmc.cambios.repository.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import mx.gob.imss.cit.mspmccommons.dto.LaudoDTO;
import mx.gob.imss.cit.pmc.cambios.repository.LaudoRepository;

@Repository
public class LaudoRepositoryImpl implements LaudoRepository {

	@Autowired
	private MongoOperations mongoOperations;

	@Override
	public Optional<LaudoDTO> findOneByCve(String cveLaudo) {
		Query query = new Query(Criteria.where("cveIdLaudo").is(Integer.valueOf(cveLaudo)));
		LaudoDTO d = this.mongoOperations.findOne(query, LaudoDTO.class);

		return Optional.ofNullable(d);
	}

	@Override
	public Optional<List<LaudoDTO>> findAll() {
		List<LaudoDTO> d = this.mongoOperations.findAll(LaudoDTO.class);

		return Optional.ofNullable(d);
	}

}
