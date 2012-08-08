package apps;
/*

 * GRAIL Real Time Localization System
 * Copyright (C) 2012 Rutgers University and Robert Moore
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *  
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

import org.grailrtls.libworldmodel.solver.SolverWorldConnection;
import org.grailrtls.libworldmodel.solver.protocol.messages.DataTransferMessage.Solution;
import org.grailrtls.libworldmodel.solver.protocol.messages.TypeAnnounceMessage.TypeSpecification;
import org.grailrtls.libworldmodel.types.BooleanConverter;
import org.grailrtls.libworldmodel.types.DataConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * SwitchController.java
 * 
 * purpose: Class that can be used by applications to easily control power switches in the real model.
 * @author Silas Waltzer, 
 * @version 1.0 8/8/2012
 * 
 * 
 */

public class SwitchController {
	/**
	 * Logger for this class.
	 */
	private static final Logger log = LoggerFactory
			.getLogger(SwitchController.class);
	/**
	 * Connection to the world model on the solver side.
	 */
	private final SolverWorldConnection solverWorldModel;
	/** 
	 * Constructor requires 1 argument: Solver-World Connection
	 */
	public SwitchController(final SolverWorldConnection swm) {
		this.solverWorldModel = swm;
		}
	/** Updates an attribute with given data.
	 * 
	 * @param URI uri to update
	 * @param attribute attribute in the uri to update
	 * @param data data value to insert
	 * @return boolean indicating  successful
	 * 
	 */

	public boolean update(final String uri, final String attribute,
			boolean inputData) {
		
	if (uri = null && attribute = null) {
	{
		log.error("Invalid arguments to update attribute: null arguments");
		return false;
	}

		if (!DataConverter.hasConverterForURI(attribute)) {
			log.error("Does not have converter for Datatype");
			return false;
		}

		byte[] data = BooleanConverter.CONVERTER.encode(inputData);

		if (data == null) {
			log.info("No Data to add");
			return false;
		}

		boolean success = this.pushData(uri, attribute, data);
		if (success) {
			System.out.println("Updated \"" + attribute + "\" with \""
					+ inputData + "\".");
		} else {
			log.error("Unable to update world model.");
		}
		return success;
	}

	/**
	 * Pushes data to the world model.
	 * @return boolean indicating success
	 */
	private boolean pushData(final String uri,
			final String attribute, final byte[] data) {
		if (uri == null) {
			log.error("Unable to create/update attribute for a null uri.");
			return false;
		}

		if (attribute == null) {
			log.error("Unable to create/update a null attribute.");
			return false;
		}

		TypeSpecification spec = new TypeSpecification();
		spec.setIsTransient(false);
		spec.setUriName(attribute);
		this.solverWorldModel.addSolutionType(spec);

		Solution soln = new Solution();
		soln.setTime(System.currentTimeMillis());
		soln.setAttributeName(attribute);
		soln.setTargetName(uri);
		soln.setData(data);

		return this.solverWorldModel.sendSolution(soln);
	}

}
