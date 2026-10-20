package ar.edu.iua.iw3.business;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ar.edu.iua.iw3.business.execption.BadRequestException;
import ar.edu.iua.iw3.business.execption.BusinessException;
import ar.edu.iua.iw3.business.execption.NotFoundException;
import ar.edu.iua.iw3.model.Cliente;
import ar.edu.iua.iw3.model.persistence.ClienteRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Negocio de clientes. Los clientes llegan dentro de la orden que manda SAP (punto 1).
 */
@Service
@Slf4j
public class ClienteBusiness implements IClienteBusiness {

	@Autowired
	private ClienteRepository clienteDAO;

	@Override
	public List<Cliente> list() throws BusinessException {
		try {
			return clienteDAO.findAll();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw BusinessException.builder().ex(e).build();
		}
	}

	@Override
	public Cliente load(String codigoExterno) throws NotFoundException, BusinessException {
		Optional<Cliente> r;
		try {
			r = clienteDAO.findByCodigoExterno(codigoExterno);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw BusinessException.builder().ex(e).build();
		}
		if (r.isEmpty()) {
			throw NotFoundException.builder().message("No se encuentra el cliente codigoExterno=" + codigoExterno).build();
		}
		return r.get();
	}

	@Override
	public Cliente sincronizar(Cliente recibido) throws BadRequestException, BusinessException {
		if (recibido == null || recibido.getCodigoExterno() == null || recibido.getCodigoExterno().isBlank()) {
			throw BadRequestException.builder().message("Falta el cliente o su codigoExterno").build();
		}
		try {
			Optional<Cliente> existente = clienteDAO.findByCodigoExterno(recibido.getCodigoExterno());
			if (existente.isPresent()) {
				// Ya lo conocemos: actualizamos sus datos con lo que mandó el sistema externo
				Cliente actual = existente.get();
				actual.setRazonSocial(recibido.getRazonSocial());
				actual.setContacto(recibido.getContacto());
				return clienteDAO.save(actual);
			}
			// Es nuevo: lo guardamos (el id lo genera la base)
			recibido.setId(null);
			return clienteDAO.save(recibido);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw BusinessException.builder().ex(e).build();
		}
	}
}
