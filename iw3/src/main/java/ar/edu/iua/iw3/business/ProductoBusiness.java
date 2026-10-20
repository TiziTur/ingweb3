package ar.edu.iua.iw3.business;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ar.edu.iua.iw3.business.execption.BadRequestException;
import ar.edu.iua.iw3.business.execption.BusinessException;
import ar.edu.iua.iw3.business.execption.NotFoundException;
import ar.edu.iua.iw3.model.Producto;
import ar.edu.iua.iw3.model.persistence.ProductoRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Negocio de productos. Los productos llegan dentro de la orden que manda SAP (punto 1).
 */
@Service
@Slf4j
public class ProductoBusiness implements IProductoBusiness {

	@Autowired
	private ProductoRepository productoDAO;

	@Override
	public List<Producto> list() throws BusinessException {
		try {
			return productoDAO.findAll();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw BusinessException.builder().ex(e).build();
		}
	}

	@Override
	public Producto load(String codigoExterno) throws NotFoundException, BusinessException {
		Optional<Producto> r;
		try {
			r = productoDAO.findByCodigoExterno(codigoExterno);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw BusinessException.builder().ex(e).build();
		}
		if (r.isEmpty()) {
			throw NotFoundException.builder().message("No se encuentra el producto codigoExterno=" + codigoExterno).build();
		}
		return r.get();
	}

	@Override
	public Producto sincronizar(Producto recibido) throws BadRequestException, BusinessException {
		if (recibido == null || recibido.getCodigoExterno() == null || recibido.getCodigoExterno().isBlank()) {
			throw BadRequestException.builder().message("Falta el producto o su codigoExterno").build();
		}
		try {
			Optional<Producto> existente = productoDAO.findByCodigoExterno(recibido.getCodigoExterno());
			if (existente.isPresent()) {
				// Ya lo conocemos: actualizamos sus datos con lo que mandó el sistema externo
				Producto actual = existente.get();
				actual.setNombre(recibido.getNombre());
				actual.setDescripcion(recibido.getDescripcion());
				return productoDAO.save(actual);
			}
			// Es nuevo: lo guardamos (el id lo genera la base)
			recibido.setId(null);
			return productoDAO.save(recibido);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw BusinessException.builder().ex(e).build();
		}
	}
}
