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
public class CK_WTLS_KEY_MAT_OUT extends Structure {
	/** C type : CK_OBJECT_HANDLE */
	public long hMacSecret;
	public long getHMacSecret() {
		return hMacSecret;
	}
	public void setHMacSecret(long hMacSecret) {
		this.hMacSecret = hMacSecret;
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
	public Pointer pIV;
	public Pointer getPIV() {
		return pIV;
	}
	public void setPIV(Pointer pIV) {
		this.pIV = pIV;
	}
	public CK_WTLS_KEY_MAT_OUT() {
		super();
	}
	 protected List<String> getFieldOrder() {
		return Arrays.asList("hMacSecret", "hKey", "pIV");
	}
	/**
	 * @param hMacSecret C type : CK_OBJECT_HANDLE<br>
	 * @param hKey C type : CK_OBJECT_HANDLE<br>
	 * @param pIV C type : CK_BYTE_PTR
	 */
	public CK_WTLS_KEY_MAT_OUT(long hMacSecret, long hKey, Pointer pIV) {
		super();
		this.hMacSecret = hMacSecret;
		this.hKey = hKey;
		this.pIV = pIV;
	}
	public CK_WTLS_KEY_MAT_OUT(Pointer peer) {
		super(peer);
	}
	public static class ByReference extends CK_WTLS_KEY_MAT_OUT implements Structure.ByReference {
		
	};
	public static class ByValue extends CK_WTLS_KEY_MAT_OUT implements Structure.ByValue {
		
	};
}
