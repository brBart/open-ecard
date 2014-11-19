/****************************************************************************
 * Copyright (C) 2012-2014 ecsec GmbH.
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

package org.openecard.sal.protocol.eac;

import iso.std.iso_iec._24727.tech.schema.ConnectionHandleType;
import iso.std.iso_iec._24727.tech.schema.DIDAuthenticate;
import iso.std.iso_iec._24727.tech.schema.DIDAuthenticateResponse;
import iso.std.iso_iec._24727.tech.schema.DIDAuthenticationDataType;
import iso.std.iso_iec._24727.tech.schema.DIDStructureType;
import iso.std.iso_iec._24727.tech.schema.GetIFDCapabilities;
import iso.std.iso_iec._24727.tech.schema.GetIFDCapabilitiesResponse;
import iso.std.iso_iec._24727.tech.schema.InputAPDUInfoType;
import iso.std.iso_iec._24727.tech.schema.SlotCapabilityType;
import iso.std.iso_iec._24727.tech.schema.Transmit;
import iso.std.iso_iec._24727.tech.schema.TransmitResponse;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.smartcardio.ResponseAPDU;
import oasis.names.tc.dss._1_0.core.schema.Result;
import org.openecard.addon.sal.FunctionType;
import org.openecard.addon.sal.ProtocolStep;
import org.openecard.binding.tctoken.TR03112Keys;
import org.openecard.bouncycastle.crypto.tls.Certificate;
import org.openecard.common.DynamicContext;
import org.openecard.common.ECardConstants;
import org.openecard.common.I18n;
import org.openecard.common.WSHelper;
import org.openecard.common.anytype.AuthDataMap;
import org.openecard.common.ifd.PACECapabilities;
import org.openecard.common.interfaces.Dispatcher;
import org.openecard.common.sal.state.CardStateEntry;
import org.openecard.common.util.Pair;
import org.openecard.common.util.Promise;
import org.openecard.common.util.TR03112Utils;
import org.openecard.crypto.common.asn1.cvc.CHAT;
import org.openecard.crypto.common.asn1.cvc.CHATVerifier;
import org.openecard.crypto.common.asn1.cvc.CardVerifiableCertificate;
import org.openecard.crypto.common.asn1.cvc.CardVerifiableCertificateChain;
import org.openecard.crypto.common.asn1.cvc.CardVerifiableCertificateVerifier;
import org.openecard.crypto.common.asn1.cvc.CertificateDescription;
import org.openecard.crypto.common.asn1.eac.AuthenticatedAuxiliaryData;
import org.openecard.crypto.common.asn1.eac.SecurityInfos;
import org.openecard.gui.ResultStatus;
import org.openecard.gui.UserConsent;
import org.openecard.gui.UserConsentNavigator;
import org.openecard.gui.definition.UserConsentDescription;
import org.openecard.gui.executor.ExecutionEngine;
import org.openecard.sal.protocol.eac.anytype.EAC1InputType;
import org.openecard.sal.protocol.eac.anytype.EAC1OutputType;
import org.openecard.sal.protocol.eac.anytype.PACEMarkerType;
import org.openecard.sal.protocol.eac.anytype.PACEOutputType;
import org.openecard.sal.protocol.eac.anytype.PasswordID;
import org.openecard.sal.protocol.eac.gui.CHATStep;
import org.openecard.sal.protocol.eac.gui.CVCStep;
import org.openecard.sal.protocol.eac.gui.CVCStepAction;
import org.openecard.sal.protocol.eac.gui.ErrorStep;
import org.openecard.sal.protocol.eac.gui.PINStep;
import org.openecard.sal.protocol.eac.gui.ProcessingStep;
import org.openecard.sal.protocol.eac.gui.ProcessingStepAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implements PACE protocol step according to BSI TR-03112-7.
 *
 * @see "BSI-TR-03112, version 1.1.2., part 7, section 4.6.5."
 * @author Tobias Wich
 * @author Moritz Horsch
 * @author Dirk Petrautzki
 */
public class PACEStep implements ProtocolStep<DIDAuthenticate, DIDAuthenticateResponse> {

    private static final Logger logger = LoggerFactory.getLogger(PACEStep.class.getName());

    private static final I18n lang = I18n.getTranslation("eac");
    private static final I18n langPace = I18n.getTranslation("pace");

    // GUI translation constants
    private static final String TITLE = "eac_user_consent_title";

    private final Dispatcher dispatcher;
    private final UserConsent gui;

    /**
     * Creates a new PACE protocol step.
     *
     * @param dispatcher Dispatcher
     * @param gui GUI
     */
    public PACEStep(Dispatcher dispatcher, UserConsent gui) {
	this.dispatcher = dispatcher;
	this.gui = gui;
    }

    @Override
    public FunctionType getFunctionType() {
	return FunctionType.DIDAuthenticate;
    }

    @Override
    public DIDAuthenticateResponse perform(DIDAuthenticate request, Map<String, Object> internalData) {
	// get context to save values in
	DynamicContext dynCtx = DynamicContext.getInstance(TR03112Keys.INSTANCE_KEY);

	DIDAuthenticate didAuthenticate = request;
	DIDAuthenticateResponse response = new DIDAuthenticateResponse();
	byte[] slotHandle = didAuthenticate.getConnectionHandle().getSlotHandle();
	dynCtx.put(EACProtocol.SLOT_HANDLE, slotHandle);
	dynCtx.put(EACProtocol.DISPATCHER, dispatcher);

	try {
	    EAC1InputType eac1Input = new EAC1InputType(didAuthenticate.getAuthenticationProtocolData());
	    EAC1OutputType eac1Output = eac1Input.getOutputType();

	    AuthenticatedAuxiliaryData aad = new AuthenticatedAuxiliaryData(eac1Input.getAuthenticatedAuxiliaryData());
	    byte pinID = PasswordID.valueOf(didAuthenticate.getDIDName()).getByte();
	    final String passwordType = PasswordID.parse(pinID).getString();

	    // determine PACE capabilities of the terminal
	    CardStateEntry cardState = (CardStateEntry) internalData.get(EACConstants.IDATA_CARD_STATE_ENTRY);
	    boolean nativePace = genericPACESupport(cardState.handleCopy());
	    dynCtx.put(EACProtocol.IS_NATIVE_PACE, nativePace);

	    // Certificate chain
	    CardVerifiableCertificateChain certChain = new CardVerifiableCertificateChain(eac1Input.getCertificates());
	    byte[] rawCertificateDescription = eac1Input.getCertificateDescription();
	    CertificateDescription certDescription = CertificateDescription.getInstance(rawCertificateDescription);

	    // put CertificateDescription into DynamicContext which is needed for later checks
	    dynCtx.put(TR03112Keys.ESERVICE_CERTIFICATE_DESC, certDescription);

	    // according to BSI-INSTANCE_KEY-7 we MUST perform some checks immediately after receiving the eService cert
	    Result activationChecksResult = performChecks(certDescription, dynCtx);
	    if (! ECardConstants.Major.OK.equals(activationChecksResult.getResultMajor())) {
		response.setResult(activationChecksResult);
		dynCtx.put(EACProtocol.AUTHENTICATION_FAILED, true);
		return response;
	    }

	    CHAT requiredCHAT = new CHAT(eac1Input.getRequiredCHAT());
	    CHAT optionalCHAT = new CHAT(eac1Input.getOptionalCHAT());

	    // get the PACEMarker
	    PACEMarkerType paceMarker = getPaceMarker(cardState, passwordType);
	    dynCtx.put(EACProtocol.PACE_MARKER, paceMarker);

	    // Verify that the certificate description matches the terminal certificate
	    CardVerifiableCertificate taCert = certChain.getTerminalCertificate();
	    CardVerifiableCertificateVerifier.verify(taCert, certDescription);
	    // Verify that the required CHAT matches the terminal certificate's CHAT
	    CHAT taCHAT = taCert.getCHAT();
	    CHATVerifier.verfiy(taCHAT, requiredCHAT);
	    // remove overlapping values from optional chat
	    optionalCHAT.restrictAccessRights(taCHAT);


	    // Prepare data in DIDAuthenticate for GUI
	    final EACData eacData = new EACData();
	    eacData.didRequest = didAuthenticate;
	    eacData.certificate = certChain.getTerminalCertificate();
	    eacData.certificateDescription = certDescription;
	    eacData.rawCertificateDescription = rawCertificateDescription;
	    eacData.transactionInfo = eac1Input.getTransactionInfo();
	    eacData.requiredCHAT = requiredCHAT;
	    eacData.optionalCHAT = optionalCHAT;
	    eacData.selectedCHAT = requiredCHAT;
	    eacData.aad = aad;
	    eacData.pinID = pinID;
	    eacData.passwordType = passwordType;
	    dynCtx.put(EACProtocol.EAC_DATA, eacData);

	    // get initial pin status
	    InputAPDUInfoType input = new InputAPDUInfoType();
	    input.setInputAPDU(new byte[] {(byte) 0x00, (byte) 0x22, (byte) 0xC1, (byte) 0xA4, (byte) 0x0F, (byte) 0x80,
		(byte) 0x0A, (byte) 0x04, (byte) 0x00, (byte) 0x7F, (byte) 0x00, (byte) 0x07, (byte) 0x02, (byte) 0x02,
		(byte) 0x04, (byte) 0x02, (byte) 0x02, (byte) 0x83, (byte) 0x01, (byte) 0x03});
	    input.getAcceptableStatusCode().add(new byte[] {(byte) 0x90, (byte) 0x00}); // pin activated 3 tries left
	    input.getAcceptableStatusCode().add(new byte[] {(byte) 0x63, (byte) 0xC2}); // pin activated 2 tries left
	    input.getAcceptableStatusCode().add(new byte[] {(byte) 0x63, (byte) 0xC1}); // pin suspended 1 try left CAN
											// needs to be entered
	    input.getAcceptableStatusCode().add(new byte[] {(byte) 0x63, (byte) 0xC0}); // pin blocked 0 tries left
	    input.getAcceptableStatusCode().add(new byte[] {(byte) 0x62, (byte) 0x83}); // pin deaktivated

	    Transmit transmit = new Transmit();
	    transmit.setSlotHandle(slotHandle);
	    transmit.getInputAPDUInfo().add(input);

	    TransmitResponse pinCheckResponse = (TransmitResponse) dispatcher.deliver(transmit);
	    byte[] output = pinCheckResponse.getOutputAPDU().get(0);
	    ResponseAPDU outputApdu = new ResponseAPDU(output);
	    byte[] status = {(byte) outputApdu.getSW1(), (byte) outputApdu.getSW2()};
	    dynCtx.put(EACProtocol.PIN_STATUS_BYTES, status);

	    boolean pinUsable = ! Arrays.equals(status, new byte[]{(byte) 0x63, (byte) 0xC0});

	    // define GUI depending on the PIN status
	    final UserConsentDescription uc = new UserConsentDescription(lang.translationForKey(TITLE));
	    if (pinUsable) {
		// create GUI and init executor
		CVCStep cvcStep = new CVCStep(eacData);
		CVCStepAction cvcStepAction = new CVCStepAction(cvcStep);
		cvcStep.setAction(cvcStepAction);
		uc.getSteps().add(cvcStep);
		uc.getSteps().add(CHATStep.createDummy());
		uc.getSteps().add(PINStep.createDummy(passwordType));
		ProcessingStep procStep = new ProcessingStep();
		ProcessingStepAction procStepAction = new ProcessingStepAction(procStep);
		procStep.setAction(procStepAction);
		uc.getSteps().add(procStep);
	    } else {
		String pin = langPace.translationForKey("pin");
		String puk = langPace.translationForKey("puk");
		String title = langPace.translationForKey("step_error_title_blocked", pin);
		String errorMsg = langPace.translationForKey("step_error_pin_blocked", pin, pin, puk, pin);
		ErrorStep eStep = new ErrorStep(title, errorMsg);
		uc.getSteps().add(eStep);

		dynCtx.put(EACProtocol.PACE_SUCCESSFUL, false);
	    }

	    Thread guiThread = new Thread(new Runnable() {
		@Override
		public void run() {
		    // get context here because it is thread local
		    DynamicContext dynCtx = DynamicContext.getInstance(TR03112Keys.INSTANCE_KEY);

		    UserConsentNavigator navigator = gui.obtainNavigator(uc);
		    dynCtx.put(TR03112Keys.OPEN_USER_CONSENT_NAVIGATOR, navigator);
		    ExecutionEngine exec = new ExecutionEngine(navigator);
		    ResultStatus guiResult = exec.process();

		    dynCtx.put(EACProtocol.GUI_RESULT, guiResult);

		    if (guiResult == ResultStatus.CANCEL) {
			Promise<Object> pPaceSuccessful = dynCtx.getPromise(EACProtocol.PACE_SUCCESSFUL);
			if (! pPaceSuccessful.isDelivered()) {
			    pPaceSuccessful.deliver(false);
			}
		    }
		}
	    }, "EAC-GUI");
	    guiThread.start();

	    // wait for PACE to finish
	    Promise<Object> pPaceSuccessful = dynCtx.getPromise(EACProtocol.PACE_SUCCESSFUL);
	    boolean paceSuccessful = (boolean) pPaceSuccessful.deref();
	    if (! paceSuccessful) {
		// TODO: differentiate between cancel and pin error
		String protocol = didAuthenticate.getAuthenticationProtocolData().getProtocol();
		cardState.removeProtocol(protocol);
		String msg = "Failure in PACE authentication.";
		Result r = WSHelper.makeResultError(ECardConstants.Minor.SAL.CANCELLATION_BY_USER, msg);
		response.setResult(r);
		dynCtx.put(EACProtocol.AUTHENTICATION_FAILED, true);
		return response;
	    }

	    // get challenge from card
	    TerminalAuthentication ta = new TerminalAuthentication(dispatcher, slotHandle);
	    byte[] challenge = ta.getChallenge();

	    // prepare DIDAuthenticationResponse
	    DIDAuthenticationDataType data = eacData.paceResponse.getAuthenticationProtocolData();
	    AuthDataMap paceOutputMap = new AuthDataMap(data);

	    //int retryCounter = Integer.valueOf(paceOutputMap.getContentAsString(PACEOutputType.RETRY_COUNTER));
	    byte[] efCardAccess = paceOutputMap.getContentAsBytes(PACEOutputType.EF_CARD_ACCESS);
	    byte[] currentCAR = paceOutputMap.getContentAsBytes(PACEOutputType.CURRENT_CAR);
	    byte[] previousCAR = paceOutputMap.getContentAsBytes(PACEOutputType.PREVIOUS_CAR);
	    byte[] idpicc = paceOutputMap.getContentAsBytes(PACEOutputType.ID_PICC);

	    // Store SecurityInfos
	    SecurityInfos securityInfos = SecurityInfos.getInstance(efCardAccess);
	    internalData.put(EACConstants.IDATA_SECURITY_INFOS, securityInfos);
	    // Store additional data
	    internalData.put(EACConstants.IDATA_AUTHENTICATED_AUXILIARY_DATA, aad);
	    internalData.put(EACConstants.IDATA_CERTIFICATES, certChain);
	    internalData.put(EACConstants.IDATA_CURRENT_CAR, currentCAR);
	    internalData.put(EACConstants.IDATA_CHALLENGE, challenge);

	    // Create response
	    //eac1Output.setRetryCounter(retryCounter);
	    eac1Output.setCHAT(eacData.selectedCHAT.toByteArray());
	    eac1Output.setCurrentCAR(currentCAR);
	    eac1Output.setPreviousCAR(previousCAR);
	    eac1Output.setEFCardAccess(efCardAccess);
	    eac1Output.setIDPICC(idpicc);
	    eac1Output.setChallenge(challenge);

	    response.setResult(WSHelper.makeResultOK());
	    response.setAuthenticationProtocolData(eac1Output.getAuthDataType());

	} catch (CertificateException ex) {
	    logger.error(ex.getMessage(), ex);
	    String msg = ex.getMessage();
	    response.setResult(WSHelper.makeResultError(ECardConstants.Minor.SAL.EAC.DOC_VALID_FAILED, msg));
	    dynCtx.put(EACProtocol.AUTHENTICATION_FAILED, true);
	} catch (WSHelper.WSException e) {
	    logger.error(e.getMessage(), e);
	    response.setResult(e.getResult());
	    dynCtx.put(EACProtocol.AUTHENTICATION_FAILED, true);
	} catch (Exception e) {
	    logger.error(e.getMessage(), e);
	    response.setResult(WSHelper.makeResultUnknownError(e.getMessage()));
	    dynCtx.put(EACProtocol.AUTHENTICATION_FAILED, true);
	}

	return response;
    }

    private PACEMarkerType getPaceMarker(CardStateEntry cardState, String pinType) {
	// TODO: replace with DIDGet call
	byte[] applicationIdentifier = cardState.getCurrentCardApplication().getApplicationIdentifier();
	DIDStructureType didStructure = cardState.getDIDStructure(pinType, applicationIdentifier);
	iso.std.iso_iec._24727.tech.schema.PACEMarkerType didMarker;
	didMarker = (iso.std.iso_iec._24727.tech.schema.PACEMarkerType) didStructure.getDIDMarker();
	return new PACEMarkerType(didMarker);
    }

    private boolean convertToBoolean(Object o) {
	if (o instanceof Boolean) {
	    return ((Boolean) o);
	} else {
	    return false;
	}
    }

    /**
     * Perform all checks as described in BSI TR-03112-7 3.4.4.
     *
     * @param certDescription CertificateDescription of the eService Certificate
     * @param dynCtx Dynamic Context
     * @return a {@link Result} set according to the results of the checks
     */
    private Result performChecks(CertificateDescription certDescription, DynamicContext dynCtx) {
	Object objectActivation = dynCtx.get(TR03112Keys.OBJECT_ACTIVATION);
	Object tokenChecks = dynCtx.get(TR03112Keys.TCTOKEN_CHECKS);
	boolean checkPassed;
	// omit these checks if explicitly disabled
	if (convertToBoolean(tokenChecks)) {
	    checkPassed = checkEserviceCertificate(certDescription, dynCtx);
	    if (! checkPassed) {
		String msg = "Hash of eService certificate is NOT contained in the CertificateDescription.";
		// TODO check for the correct minor type
		Result r = WSHelper.makeResultError(ECardConstants.Minor.App.UNKNOWN_ERROR, msg);
		return r;
	    }

	    // only perform the following checks if new activation is used
	    if (! convertToBoolean(objectActivation)) {
		checkPassed = checkTCTokenServerCertificates(certDescription, dynCtx);
		if (! checkPassed) {
		    String msg = "Hash of the TCToken server certificate is NOT contained in the CertificateDescription.";
		    // TODO check for the correct minor type
		    Result r = WSHelper.makeResultError(ECardConstants.Minor.App.UNKNOWN_ERROR, msg);
		    return r;
		}

		checkPassed = checkTCTokenAndSubjectURL(certDescription, dynCtx);
		if (! checkPassed) {
		    String msg = "TCToken does not come from the server to which the authorization certificate was issued.";
		    // TODO check for the correct minor type
		    Result r = WSHelper.makeResultError(ECardConstants.Minor.App.UNKNOWN_ERROR, msg);
		    return r;
		}
	    } else {
		logger.warn("Checks according to BSI TR03112 3.4.4 (TCToken specific) skipped.");
	    }
	} else {
	    logger.warn("Checks according to BSI TR03112 3.4.4 skipped.");
	}

	// all checks passed
	return WSHelper.makeResultOK();
    }

    private boolean checkTCTokenAndSubjectURL(CertificateDescription certDescription, DynamicContext dynCtx) {
	Object o = dynCtx.get(TR03112Keys.TCTOKEN_URL);
	if (o instanceof URL) {
	    URL tcTokenURL = (URL) o;
	    try {
		URL subjectURL = new URL(certDescription.getSubjectURL());
		return TR03112Utils.checkSameOriginPolicy(tcTokenURL, subjectURL);
	    } catch (MalformedURLException e) {
		logger.error("SubjectURL in CertificateDescription is not a well formed URL.");
		return false;
	    }
	} else {
	    logger.error("No TC Token URL set in Dynamic Context.");
	    return false;
	}
    }

    private boolean checkEserviceCertificate(CertificateDescription certDescription, DynamicContext dynCtx) {
	Object o = dynCtx.get(TR03112Keys.ESERVICE_CERTIFICATE);
	if (o instanceof Certificate) {
	    Certificate certificate = (Certificate) o;
	    return TR03112Utils.isInCommCertificates(certificate, certDescription.getCommCertificates());
	} else {
	    logger.error("No eService TLS Certificate set in Dynamic Context.");
	    return false;
	}
    }

    private boolean checkTCTokenServerCertificates(CertificateDescription certDescription, DynamicContext dynCtx) {
	Object o = dynCtx.get(TR03112Keys.TCTOKEN_SERVER_CERTIFICATES);
	if (o instanceof List) {
	    List<?> certificates = (List<?>) o;
	    for (Object cert : certificates) {
		if (cert instanceof Pair) {
		    Pair<?, ?> p = (Pair<?, ?>) cert;
		    if (p.p2 instanceof Certificate) {
			Certificate bcCert = (Certificate) p.p2;
			if (! TR03112Utils.isInCommCertificates(bcCert, certDescription.getCommCertificates())) {
			    return false;
			}
		    }
		}
	    }
	    return true;
	} else {
	    logger.error("No TC Token server certificates set in Dynamic Context.");
	    return false;
	}
    }

    /**
     * Check if the selected card reader supports PACE.
     * In that case, the reader is a standard or comfort reader.
     *
     * @param connectionHandle Handle describing the IFD and reader.
     * @return true when card reader supports genericPACE, false otherwise.
     * @throws Exception
     */
    private boolean genericPACESupport(ConnectionHandleType connectionHandle) throws Exception {
	// Request terminal capabilities
	GetIFDCapabilities capabilitiesRequest = new GetIFDCapabilities();
	capabilitiesRequest.setContextHandle(connectionHandle.getContextHandle());
	capabilitiesRequest.setIFDName(connectionHandle.getIFDName());
	GetIFDCapabilitiesResponse capabilitiesResponse = (GetIFDCapabilitiesResponse) dispatcher.deliver(capabilitiesRequest);
	WSHelper.checkResult(capabilitiesResponse);

	if (capabilitiesResponse.getIFDCapabilities() != null) {
	    List<SlotCapabilityType> capabilities = capabilitiesResponse.getIFDCapabilities().getSlotCapability();
	    // Check all capabilities for generic PACE
	    final String genericPACE = PACECapabilities.PACECapability.GenericPACE.getProtocol();
	    for (SlotCapabilityType capability : capabilities) {
		if (capability.getIndex().equals(connectionHandle.getSlotIndex())) {
		    for (String protocol : capability.getProtocol()) {
			if (protocol.equals(genericPACE)) {
			    return true;
			}
		    }
		}
	    }
	}

	// No PACE capability found
	return false;
    }

}
