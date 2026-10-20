package ar.edu.iua.iw3.business;

import java.util.List;

import ar.edu.iua.iw3.business.execption.BadRequestException;
import ar.edu.iua.iw3.business.execption.BusinessException;
import ar.edu.iua.iw3.business.execption.NotFoundException;
import ar.edu.iua.iw3.model.Cliente;

public interface IClienteBusiness {
	public List<Cliente> list() throws BusinessException;

	public Cliente load(String codigoExterno) throws NotFoundException, BusinessException;

	// Si existe un cliente con ese código externo lo actualiza, si no lo crea.
	public Cliente sincronizar(Cliente cliente) throws BadRequestException, BusinessException;
}
