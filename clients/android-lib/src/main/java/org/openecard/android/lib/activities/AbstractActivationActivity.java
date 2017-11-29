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

package org.openecard.android.lib.activities;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import static android.content.Context.BIND_AUTO_CREATE;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import org.openecard.addon.bind.AuxDataKeys;
import org.openecard.addon.bind.BindingResult;
import org.openecard.android.lib.ServiceContext;
import org.openecard.android.lib.async.tasks.BindingTaskResponse;
import org.openecard.android.lib.async.tasks.BindingTaskResult;
import org.openecard.android.lib.ex.ApduExtLengthNotSupported;
import org.openecard.android.lib.ex.BindingTaskStillRunning;
import org.openecard.android.lib.ex.ContextNotInitialized;
import org.openecard.android.lib.intent.binding.IntentBinding;
import org.openecard.android.lib.utils.NfcUtils;
import org.openecard.common.event.EventObject;
import org.openecard.common.event.EventType;
import org.openecard.common.interfaces.EventCallback;
import org.openecard.gui.android.eac.EacGuiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class provides the basic functionality as specified in the technical guideline and the
 * initialisation of the Eac UI interface service. By extending the class, the UI can be added
 * (see BindingActivity in <a href="https://github.com/ecsec/open-ecard-android">ecsec/open-ecard-android</a>).
 *
 * @author Mike Prechtl
 */
public abstract class AbstractActivationActivity extends Activity implements BindingTaskResult {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractActivationActivity.class);

    private Dialog cardRemoveDialog;

    private Context appCtx;
    private ServiceConnection eacServiceCon;

    private boolean eacAlreadyConnected = false;

    private volatile boolean alreadyInitialized = false;
    // if someone returns to the App, but Binding uri was already used.
    private volatile boolean bindingUriAlreadyUsed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);

	// set Open eCard Service Context
	NfcUtils.getInstance().setServiceContext(ServiceContext.getServiceContext());
    }

    @Override
    protected synchronized void onResume() {
	super.onResume();

	// enable dispatch with nfc tag
	NfcUtils.getInstance().enableNFCDispatch(this);
    }

    @Override
    protected synchronized void onPause() {
	super.onPause();

	try {
	    // disable dispatch with nfc tag
	    NfcUtils.getInstance().disableNFCDispatch(this);
	} catch (Exception e) {
	    LOG.info(e.getMessage(), e);
	}
    }

    @Override
    protected void onStart() {
	super.onStart();

	// add callback to this abstract activity when card is removed
	ServiceContext.getServiceContext().getEventDispatcher().add(eventReceiver, EventType.CARD_REMOVED);

	// set up application context and service connection
	appCtx = getApplicationContext();
	eacServiceCon = getServiceConnection();

	// initialize intent binding
	if (! alreadyInitialized) {
	    IntentBinding binding = IntentBinding.getInstance();
	    binding.setBindingResultReceiver(this);
	    ServiceContext.getServiceContext().setEacStarter(this.guiStarter);
	    this.alreadyInitialized = true;
	}

	String bindingUri = getBindingURI(getIntent());
	if (bindingUri != null && ! bindingUriAlreadyUsed) {
	    // start TR procedure according to [BSI-TR-03124-1]
	    HandleRequestAsync task = new HandleRequestAsync();
	    task.execute(bindingUri);
	    bindingUriAlreadyUsed = true;
	} else {
	    finish();
	}
    }

    private class HandleRequestAsync extends AsyncTask<String, Void, Void> {
	@Override
	protected Void doInBackground(String... uri) {
	    IntentBinding binding = IntentBinding.getInstance();
	    try {
		binding.handleRequest(uri[0]);
	    } catch (ContextNotInitialized | BindingTaskStillRunning e) {
		LOG.error(e.getMessage(), e);
	    }
	    return null;
	}
    }

    private final Runnable guiStarter = new Runnable() {
	@Override
	public void run() {
	    if (! eacAlreadyConnected) {
		// start and bind eac gui service
		Intent i = createEacGuiIntent();
		LOG.info("Binding Eac Gui service...");
		appCtx.bindService(i, eacServiceCon, BIND_AUTO_CREATE);
		LOG.info("Starting Eac Gui service...");
		appCtx.startService(i);
		eacAlreadyConnected = true;
	    }
	}
    };

    @Override
    protected void onNewIntent(Intent intent) {
	super.onNewIntent(intent);
	try {
	    // extract nfc tag
	    NfcUtils.getInstance().retrievedNFCTag(intent);
	} catch (ApduExtLengthNotSupported ex) {
	    LOG.error(ex.getMessage());
	}
    }


    @Override
    protected void onStop() {
	super.onStop();

	// remove callback which is set onStart
	ServiceContext.getServiceContext().getEventDispatcher().del(eventReceiver);

	// cancel request if app is closed or minimized
	IntentBinding binding = IntentBinding.getInstance();
	binding.cancelRequest();
	if (eacAlreadyConnected) {
	    // unbind eac gui service
	    Intent i = createEacGuiIntent();
	    eacAlreadyConnected = false;
	    LOG.info("Stop Eac Gui service...");
	    appCtx.stopService(i);
	    LOG.info("Unbinding Eac Gui service...");
	    appCtx.unbindService(eacServiceCon);
	} // else do nothing, because the service hasn't been started yet, maybe because the user canceled the request.
    }

    @Override
    protected void onDestroy() {
	super.onDestroy();
	if (alreadyInitialized) {
	    alreadyInitialized = false;
	}
    }

    private final EventCallback eventReceiver = new EventCallback() {

	@Override
	public void signalEvent(EventType eventType, EventObject eventData) {
	    if (eventType.equals(EventType.CARD_REMOVED)) {
		if (cardRemoveDialog != null && cardRemoveDialog.isShowing()) {
		    cardRemoveDialog.dismiss();
		}
	    } else {
		throw new IllegalStateException("Recognized an unsupported Event: " + eventType.name());
	    }
	}
    };

    @Override
    public void setResultOfBindingTask(BindingTaskResponse response) {
	final BindingResult result = response.getBindingResult();
	switch (result.getResultCode()) {
	    case OK:
		authenticationSuccess(result);
		break;
	    case REDIRECT:
		// show card remove dialog before the redirect occurs
		cardRemoveDialog = showCardRemoveDialog();
		cardRemoveDialog.setCanceledOnTouchOutside(false);
		cardRemoveDialog.setCancelable(false);
		// if card remove dialog is not shown, then show it
		if (! cardRemoveDialog.isShowing()) {
		    cardRemoveDialog.show();
		}
		// redirect to the termination uri when the card remove dialog is closed
		cardRemoveDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
		    @Override
		    public void onDismiss(DialogInterface dialog) {
			redirectToResultLocation(result);
		    }
		});
		break;
	    default:
		authenticationFailure(result);
		break;
	}
    }

    public void redirectToResultLocation(BindingResult result) {
	String location = result.getAuxResultData().get(AuxDataKeys.REDIRECT_LOCATION);
	if (location != null) {
	    // redirct to result location
	    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(location));
	    startActivity(i);
	}
    }

    /**
     * Extracts the binding uri from the intent.
     *
     * @param i the corresponding intent.
     * @return
     */
    protected String getBindingURI(Intent i) {
	Uri data = i.getData();
	return data != null ? data.toString() : null;
    }

    /**
     * Creates an intent to start the Eac Gui Service.
     *
     * @return
     */
    protected Intent createEacGuiIntent() {
	return new Intent(appCtx, EacGuiService.class);
    }

    /**
     * Returns true if the activity is already connected to the Eac Gui Service, otherwise false is returned.
     *
     * @return
     */
    protected boolean isConnectedToEacService() {
	return eacAlreadyConnected;
    }

    /**
     * Returns true if the app is already connected to the Open eCard Service, otherwise false is returned. Before you
     * can connect to the Eac Gui Service, a connection to the Open eCard Service must be established. You have to check
     * if you are connected to the Open eCard Service in the sub class in the onStart()-method. You can handle the check
     * by using this method or on your own, too.
     *
     * @return
     */
    protected boolean isConnectedToOpeneCardService() {
	return ServiceContext.getServiceContext().isInitialized();
    }

    /**
     * Implement this method to provide an instance of a service connection. The service connection is needed to
     * connect to the Eac Gui Service.
     *
     * @return
     */
    public abstract ServiceConnection getServiceConnection();

    /**
     * Implement this method to recognize a successful authentication in the Sub-Activity. You can handle the following
     * steps on your own, for example show that the authentication was successful and then close the Activity.
     *
     * @param result which contains additional information to the authentication.
     */
    public abstract void authenticationSuccess(BindingResult result);

    /**
     * Implement this method to recognize a failed authentication in the Sub-Activity. You can handle the following
     * steps on your own, for example show that the authentication failed and then close the Activity with finish().
     *
     * @param result  which contains additional information to the authentication.
     */
    public abstract void authenticationFailure(BindingResult result);

    /**
     * Implement this method to show the card remove dialog. If the authentication process ends, the card should be
     * removed. To enable this, a card remove dialog is shown to the user. The dialog should contain only a hint for
     * the user. The dialog can not be removed by the user with a button click, only by the app when the card is removed.
     *
     * @return
     */
    public abstract Dialog showCardRemoveDialog();

}
