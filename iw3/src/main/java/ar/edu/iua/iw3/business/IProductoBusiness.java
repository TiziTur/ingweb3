package ar.edu.iua.iw3.business;

import java.util.List;

import ar.edu.iua.iw3.business.execption.BadRequestException;
import ar.edu.iua.iw3.business.execption.BusinessException;
import ar.edu.iua.iw3.business.execption.NotFoundException;
import ar.edu.iua.iw3.model.Producto;

public interface IProductoBusiness {
	public List<Producto> list() throws BusinessException;

	public Producto load(String codigoExterno) throws NotFoundException, BusinessException;

	// Si existe un producto con ese código externo lo actualiza, si no lo crea.
	public Producto sincronizar(Producto producto) throws BadRequestException, BusinessException;
}
