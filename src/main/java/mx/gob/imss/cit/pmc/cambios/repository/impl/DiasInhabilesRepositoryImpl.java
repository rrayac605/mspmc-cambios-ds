package mx.gob.imss.cit.pmc.cambios.repository.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.stereotype.Repository;

import mx.gob.imss.cit.mspmccommons.dto.FechaInhabilDTO;
import mx.gob.imss.cit.pmc.cambios.repository.DiasInhabilesRepository;

@Repository
public class DiasInhabilesRepositoryImpl implements DiasInhabilesRepository {

	@Autowired
	private MongoOperations mongoOperations;

	@Override
	public List<FechaInhabilDTO> findAll() {
		return this.mongoOperations.findAll(FechaInhabilDTO.class);
	}

}
