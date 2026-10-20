package ar.edu.iua.iw3.business;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ar.edu.iua.iw3.business.execption.BadRequestException;
import ar.edu.iua.iw3.business.execption.BusinessException;
import ar.edu.iua.iw3.business.execption.NotFoundException;
import ar.edu.iua.iw3.model.Chofer;
import ar.edu.iua.iw3.model.persistence.ChoferRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Negocio de choferes. Los choferes llegan dentro de la orden que manda SAP (punto 1).
 */
@Service
@Slf4j
public class ChoferBusiness implements IChoferBusiness {

	@Autowired
	private ChoferRepository choferDAO;

	@Override
	public List<Chofer> list() throws BusinessException {
		try {
			return choferDAO.findAll();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw BusinessException.builder().ex(e).build();
		}
	}

	@Override
	public Chofer load(String codigoExterno) throws NotFoundException, BusinessException {
		Optional<Chofer> r;
		try {
			r = choferDAO.findByCodigoExterno(codigoExterno);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw BusinessException.builder().ex(e).build();
		}
		if (r.isEmpty()) {
			throw NotFoundException.builder().message("No se encuentra el chofer codigoExterno=" + codigoExterno).build();
		}
		return r.get();
	}

	@Override
	public Chofer sincronizar(Chofer recibido) throws BadRequestException, BusinessException {
		if (recibido == null || recibido.getCodigoExterno() == null || recibido.getCodigoExterno().isBlank()) {
			throw BadRequestException.builder().message("Falta el chofer o su codigoExterno").build();
		}
		try {
			Optional<Chofer> existente = choferDAO.findByCodigoExterno(recibido.getCodigoExterno());
			if (existente.isPresent()) {
				// Ya lo conocemos: actualizamos sus datos con lo que mandó el sistema externo
				Chofer actual = existente.get();
				actual.setNombre(recibido.getNombre());
				actual.setApellido(recibido.getApellido());
				actual.setDocumento(recibido.getDocumento());
				return choferDAO.save(actual);
			}
			// Es nuevo: lo guardamos (el id lo genera la base)
			recibido.setId(null);
			return choferDAO.save(recibido);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw BusinessException.builder().ex(e).build();
		}
	}
}
