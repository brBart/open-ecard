package org.openecard.mdlw.sal.cryptoki;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;
/**
 * <i>native declaration : pkcs11_v2.40/pkcs11t.h</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> , <a href="http://rococoa.dev.java.net/">Rococoa</a>, or <a href="http://jna.dev.java.net/">JNA</a>.
 */
public class CK_KIP_PARAMS extends Structure {
	/** C type : CK_MECHANISM_PTR */
	public org.openecard.mdlw.sal.cryptoki.CK_MECHANISM.ByReference pMechanism;
	public org.openecard.mdlw.sal.cryptoki.CK_MECHANISM.ByReference getPMechanism() {
		return pMechanism;
	}
	public void setPMechanism(org.openecard.mdlw.sal.cryptoki.CK_MECHANISM.ByReference pMechanism) {
		this.pMechanism = pMechanism;
	}
	/** C type : CK_OBJECT_HANDLE */
	public long hKey;
	public long getHKey() {
		return hKey;
	}
	public void setHKey(long hKey) {
		this.hKey = hKey;
	}
	/** C type : CK_BYTE_PTR */
	public Pointer pSeed;
	public Pointer getPSeed() {
		return pSeed;
	}
	public void setPSeed(Pointer pSeed) {
		this.pSeed = pSeed;
	}
	/** C type : CK_ULONG */
	public long ulSeedLen;
	public long getUlSeedLen() {
		return ulSeedLen;
	}
	public void setUlSeedLen(long ulSeedLen) {
		this.ulSeedLen = ulSeedLen;
	}
	public CK_KIP_PARAMS() {
		super();
	}
	 protected List<String> getFieldOrder() {
		return Arrays.asList("pMechanism", "hKey", "pSeed", "ulSeedLen");
	}
	/**
	 * @param pMechanism C type : CK_MECHANISM_PTR<br>
	 * @param hKey C type : CK_OBJECT_HANDLE<br>
	 * @param pSeed C type : CK_BYTE_PTR<br>
	 * @param ulSeedLen C type : CK_ULONG
	 */
	public CK_KIP_PARAMS(org.openecard.mdlw.sal.cryptoki.CK_MECHANISM.ByReference pMechanism, long hKey, Pointer pSeed, long ulSeedLen) {
		super();
		this.pMechanism = pMechanism;
		this.hKey = hKey;
		this.pSeed = pSeed;
		this.ulSeedLen = ulSeedLen;
	}
	public CK_KIP_PARAMS(Pointer peer) {
		super(peer);
	}
	public static class ByReference extends CK_KIP_PARAMS implements Structure.ByReference {
		
	};
	public static class ByValue extends CK_KIP_PARAMS implements Structure.ByValue {
		
	};
}
