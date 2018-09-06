/****************************************************************************
 * Copyright (C) 2017 ecsec GmbH.
 * All rights reserved.
 * Contact: ecsec GmbH (info@ecsec.de)
 *
 * This file is part of the Open eCard App.
 *
 * GNU General Public License Usage
 * This file may be used under the terms of the GNU General Public
 * License version 3.0 as published by the Free Software Foundation
 * and appearing in the file LICENSE.GPL included in the packaging of
 * this file. Please review the following information to ensure the
 * GNU General Public License version 3.0 requirements will be met:
 * http://www.gnu.org/copyleft/gpl.html.
 *
 * Other Usage
 * Alternatively, this file may be used in accordance with the terms
 * and conditions contained in a signed written agreement between
 * you and ecsec GmbH.
 *
 ***************************************************************************/

package org.openecard.android.activation;


/**
 *
 * @author Mike Prechtl
 */
public enum ActivationResultCode {

    OK(200), REDIRECT(301), CLIENT_ERROR(400), INTERRUPTED(400), INTERNAL_ERROR(500), DEPENDING_HOST_UNREACHABLE(503),
    RESOURCE_UNAVAILABLE(503);

    private final int statusCode;

    private ActivationResultCode(int statusCode) {
	this.statusCode = statusCode;
    }

    public int getHttpStatusCode() {
	return statusCode;
    }

}
