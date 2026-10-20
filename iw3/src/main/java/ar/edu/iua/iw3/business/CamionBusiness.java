package ar.edu.iua.iw3.business;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ar.edu.iua.iw3.business.execption.BadRequestException;
import ar.edu.iua.iw3.business.execption.BusinessException;
import ar.edu.iua.iw3.business.execption.NotFoundException;
import ar.edu.iua.iw3.model.Camion;
import ar.edu.iua.iw3.model.persistence.CamionRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Negocio de camiones. Los camiones llegan dentro de la orden que manda SAP (punto 1).
 */
@Service
@Slf4j
public class CamionBusiness implements ICamionBusiness {

	@Autowired
	private CamionRepository camionDAO;

	@Override
	public List<Camion> list() throws BusinessException {
		try {
			return camionDAO.findAll();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw BusinessException.builder().ex(e).build();
		}
	}

	@Override
	public Camion load(String codigoExterno) throws NotFoundException, BusinessException {
		Optional<Camion> r;
		try {
			r = camionDAO.findByCodigoExterno(codigoExterno);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw BusinessException.builder().ex(e).build();
		}
		if (r.isEmpty()) {
			throw NotFoundException.builder().message("No se encuentra el camion codigoExterno=" + codigoExterno).build();
		}
		return r.get();
	}

	@Override
	public Camion sincronizar(Camion recibido) throws BadRequestException, BusinessException {
		if (recibido == null || recibido.getCodigoExterno() == null || recibido.getCodigoExterno().isBlank()) {
			throw BadRequestException.builder().message("Falta el camion o su codigoExterno").build();
		}
		try {
			Optional<Camion> existente = camionDAO.findByCodigoExterno(recibido.getCodigoExterno());
			if (existente.isPresent()) {
				// Ya lo conocemos: actualizamos sus datos con lo que mandó el sistema externo
				Camion actual = existente.get();
				actual.setPatente(recibido.getPatente());
				actual.setDescripcion(recibido.getDescripcion());
				// Las cisternas se reemplazan: se vacía la lista vieja y se cargan las nuevas.
				// Como la relación tiene orphanRemoval, las cisternas viejas se borran de la base.
				actual.getCisternas().clear();
				if (recibido.getCisternas() != null) {
					recibido.getCisternas().forEach(c -> c.setId(null));
					actual.getCisternas().addAll(recibido.getCisternas());
				}
				return camionDAO.save(actual);
			}
			// Es nuevo: lo guardamos (el id lo genera la base)
			recibido.setId(null);
			return camionDAO.save(recibido);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw BusinessException.builder().ex(e).build();
		}
	}
}
