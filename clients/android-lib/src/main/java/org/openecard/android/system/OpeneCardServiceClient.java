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

package org.openecard.android.system;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import static org.openecard.android.system.ServiceResponseStatusCodes.INTERNAL_ERROR;
import org.openecard.common.util.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Client class for the Android Service initializing the Open eCard Stack.
 * <p>This class provides synchronous methods to initialize the stack. Once the stack is initialized, the context object
 * can be obtained.</p>
 * The class automatically adjusts its inner state when the Open eCard Stack is stopped from the outside.
 *
 * @author Tobias Wich
 * @author Mike Prechtl
 */
public class OpeneCardServiceClient {

    private static final Logger LOG = LoggerFactory.getLogger(OpeneCardServiceClient.class);

    // lock object to make access to the openecard service globally locked
    private static final Object SYNC = new Object();

    private final Context appCtx;
    private Promise<OpeneCardService> oecService;
    private boolean isInitialized;

    public OpeneCardServiceClient(Context appCtx) {
	this.appCtx = appCtx;
	oecService = new Promise<>();
	isInitialized = false;
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
	@Override
	public void onServiceConnected(ComponentName componentName, IBinder service) {
	    LOG.info("Open eCard Service bound.");
	    OpeneCardService s = OpeneCardService.Stub.asInterface(service);
	    oecService.deliver(s);
	    isInitialized = true;
	}

	@Override
	public void onServiceDisconnected(ComponentName componentName) {
	    oecService = new Promise<>();
	    isInitialized = false;
	}
    };

    /**
     * Synchronously start the Open eCard Stack.
     *
     * @return The result of the start function.
     */
    public ServiceResponse startService() {
	synchronized (SYNC) {
	    try {
		if (! isInitialized) {
		    Intent i = createOpeneCardIntent();
		    appCtx.bindService(i, serviceConnection, Context.BIND_AUTO_CREATE);
		}

		// wait until service is connected, then call startService
		OpeneCardService s = oecService.deref();
		assert(s != null);
		return s.startService();
	    } catch (InterruptedException | RemoteException ex) {
		return new ServiceErrorResponse(INTERNAL_ERROR, ex.getMessage());
	    }
	}
    }

    /**
     * Synchronously stop the Open eCard Stack.
     *
     * @return The result of the stop function.
     * @throws IllegalStateException Thrown in case the service is already stopped.
     */
    public ServiceResponse stopService() throws IllegalStateException {
	synchronized (SYNC) {
	    try {
		if (! isInitialized) {
		    throw new IllegalStateException("Trying to stop uninitialized service.");
		}

		// stop then unbind service
		OpeneCardService s = oecService.deref();
		assert(s != null);
		ServiceResponse res = s.stopService();
		unbindService();
		return res;
	    } catch (InterruptedException | RemoteException ex) {
		return new ServiceErrorResponse(INTERNAL_ERROR, ex.getMessage());
	    }
	}
    }

    /**
     * Unbinds the service client.
     * This call removes the binding, so te service can be GCed properly.
     *
     * @see Context#unbindService(ServiceConnection)
     */
    public void unbindService() {
	appCtx.unbindService(serviceConnection);
    }

    /**
     * Returns whether the Open eCard Stack is initialized or not.
     * This value can also change when the service managing the stack is stopped from the outside.
     *
     * @return {@code true} if the stack is initialized, {@code false} otherwise.
     */
    public boolean isInitialized() {
	return isInitialized;
    }

    /**
     * Gets the context object when the Open eCard Stack is intialized.
     * The {@link #isInitialized()} method should return {@code true} before this method can be used.
     *
     * @return The context object.
     * @throws IllegalStateException Thrown in case the stack is not initialized.
     * @see #isInitialized()
     */
    public OpeneCardContext getContext() throws IllegalStateException {
	synchronized (SYNC) {
	    if (isInitialized()) {
		return OpeneCardServiceImpl.getContext();
	    } else {
		throw new IllegalStateException("Requested unitialized Context object.");
	    }
	}
    }

    private Intent createOpeneCardIntent() {
	return new Intent(appCtx, OpeneCardServiceImpl.class);
    }

}
