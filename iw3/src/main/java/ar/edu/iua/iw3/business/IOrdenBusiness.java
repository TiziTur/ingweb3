package ar.edu.iua.iw3.business;

import java.util.List;

import ar.edu.iua.iw3.business.execption.BadRequestException;
import ar.edu.iua.iw3.business.execption.BusinessException;
import ar.edu.iua.iw3.business.execption.ConflictException;
import ar.edu.iua.iw3.business.execption.FoundException;
import ar.edu.iua.iw3.business.execption.NotFoundException;
import ar.edu.iua.iw3.model.CambioEstado;
import ar.edu.iua.iw3.model.DetalleCarga;
import ar.edu.iua.iw3.model.Orden;
import ar.edu.iua.iw3.model.dto.Conciliacion;
import ar.edu.iua.iw3.model.dto.DatoCargaRequest;

public interface IOrdenBusiness {

	// ----- Consultas -----
	public List<Orden> list() throws BusinessException;

	public Orden load(Long numeroOrden) throws NotFoundException, BusinessException;

	public List<DetalleCarga> listDetalles(Long numeroOrden) throws NotFoundException, BusinessException;

	public List<CambioEstado> listCambiosEstado(Long numeroOrden) throws NotFoundException, BusinessException;

	// ----- Punto 1: SAP envía la orden -> estado 1 -----
	public Orden recibirOrden(Orden orden) throws FoundException, BadRequestException, BusinessException;

	// ----- Punto 2: TMS envía la tara -> estado 2, se genera la password -----
	public Orden registrarPesajeInicial(Long numeroOrden, Double tara)
			throws NotFoundException, ConflictException, BadRequestException, BusinessException;

	// ----- Punto 3: el sistema de carga valida la password y recibe el preset -----
	public Orden activarCarga(Long numeroOrden, Integer password)
			throws NotFoundException, ConflictException, BadRequestException, BusinessException;

	// ----- Punto 3: el sistema de carga envía datos en tiempo real -----
	public Orden recibirDatoCarga(Long numeroOrden, DatoCargaRequest dato)
			throws NotFoundException, ConflictException, BadRequestException, BusinessException;

	// ----- Punto 4: se cierra la orden -> estado 3 -----
	public Orden cerrarOrden(Long numeroOrden) throws NotFoundException, ConflictException, BusinessException;

	// ----- Punto 5: TMS envía el pesaje final -> estado 4, se devuelve la conciliación -----
	public Conciliacion registrarPesajeFinal(Long numeroOrden, Double pesajeFinal)
			throws NotFoundException, ConflictException, BadRequestException, BusinessException;

	// ----- Punto 5: conciliación (solo órdenes en estado 4) -----
	public Conciliacion getConciliacion(Long numeroOrden) throws NotFoundException, ConflictException, BusinessException;
}
