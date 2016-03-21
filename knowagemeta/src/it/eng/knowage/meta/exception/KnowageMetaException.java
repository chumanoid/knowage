/**
 SpagoBI, the Open Source Business Intelligence suite

 Copyright (C) 2012 Engineering Ingegneria Informatica S.p.A. - SpagoBI Competency Center
 This Source Code Form is subject to the terms of the Mozilla Public
 License, v. 2.0. If a copy of the MPL was not distributed with this file,
 You can obtain one at http://mozilla.org/MPL/2.0/.
 
 **/
package it.eng.knowage.meta.exception;

/**
 * @author Antonella Giachino (antonella.giachino@eng.it)
 *
 */
public class KnowageMetaException extends RuntimeException {

	private static final long serialVersionUID = -2775618353105672486L;

	public KnowageMetaException(String msg) {
		super(msg);
	}

	public KnowageMetaException(String msg, Throwable t) {
		super(msg, t);
	}
}
