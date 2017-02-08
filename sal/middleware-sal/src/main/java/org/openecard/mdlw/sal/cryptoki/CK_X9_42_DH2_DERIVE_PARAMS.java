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
public class CK_X9_42_DH2_DERIVE_PARAMS extends Structure {
	/** C type : CK_X9_42_DH_KDF_TYPE */
	public long kdf;
	public long getKdf() {
		return kdf;
	}
	public void setKdf(long kdf) {
		this.kdf = kdf;
	}
	/** C type : CK_ULONG */
	public long ulOtherInfoLen;
	public long getUlOtherInfoLen() {
		return ulOtherInfoLen;
	}
	public void setUlOtherInfoLen(long ulOtherInfoLen) {
		this.ulOtherInfoLen = ulOtherInfoLen;
	}
	/** C type : CK_BYTE_PTR */
	public Pointer pOtherInfo;
	public Pointer getPOtherInfo() {
		return pOtherInfo;
	}
	public void setPOtherInfo(Pointer pOtherInfo) {
		this.pOtherInfo = pOtherInfo;
	}
	/** C type : CK_ULONG */
	public long ulPublicDataLen;
	public long getUlPublicDataLen() {
		return ulPublicDataLen;
	}
	public void setUlPublicDataLen(long ulPublicDataLen) {
		this.ulPublicDataLen = ulPublicDataLen;
	}
	/** C type : CK_BYTE_PTR */
	public Pointer pPublicData;
	public Pointer getPPublicData() {
		return pPublicData;
	}
	public void setPPublicData(Pointer pPublicData) {
		this.pPublicData = pPublicData;
	}
	/** C type : CK_ULONG */
	public long ulPrivateDataLen;
	public long getUlPrivateDataLen() {
		return ulPrivateDataLen;
	}
	public void setUlPrivateDataLen(long ulPrivateDataLen) {
		this.ulPrivateDataLen = ulPrivateDataLen;
	}
	/** C type : CK_OBJECT_HANDLE */
	public long hPrivateData;
	public long getHPrivateData() {
		return hPrivateData;
	}
	public void setHPrivateData(long hPrivateData) {
		this.hPrivateData = hPrivateData;
	}
	/** C type : CK_ULONG */
	public long ulPublicDataLen2;
	public long getUlPublicDataLen2() {
		return ulPublicDataLen2;
	}
	public void setUlPublicDataLen2(long ulPublicDataLen2) {
		this.ulPublicDataLen2 = ulPublicDataLen2;
	}
	/** C type : CK_BYTE_PTR */
	public Pointer pPublicData2;
	public Pointer getPPublicData2() {
		return pPublicData2;
	}
	public void setPPublicData2(Pointer pPublicData2) {
		this.pPublicData2 = pPublicData2;
	}
	public CK_X9_42_DH2_DERIVE_PARAMS() {
		super();
	}
	 protected List<String> getFieldOrder() {
		return Arrays.asList("kdf", "ulOtherInfoLen", "pOtherInfo", "ulPublicDataLen", "pPublicData", "ulPrivateDataLen", "hPrivateData", "ulPublicDataLen2", "pPublicData2");
	}
	/**
	 * @param kdf C type : CK_X9_42_DH_KDF_TYPE<br>
	 * @param ulOtherInfoLen C type : CK_ULONG<br>
	 * @param pOtherInfo C type : CK_BYTE_PTR<br>
	 * @param ulPublicDataLen C type : CK_ULONG<br>
	 * @param pPublicData C type : CK_BYTE_PTR<br>
	 * @param ulPrivateDataLen C type : CK_ULONG<br>
	 * @param hPrivateData C type : CK_OBJECT_HANDLE<br>
	 * @param ulPublicDataLen2 C type : CK_ULONG<br>
	 * @param pPublicData2 C type : CK_BYTE_PTR
	 */
	public CK_X9_42_DH2_DERIVE_PARAMS(long kdf, long ulOtherInfoLen, Pointer pOtherInfo, long ulPublicDataLen, Pointer pPublicData, long ulPrivateDataLen, long hPrivateData, long ulPublicDataLen2, Pointer pPublicData2) {
		super();
		this.kdf = kdf;
		this.ulOtherInfoLen = ulOtherInfoLen;
		this.pOtherInfo = pOtherInfo;
		this.ulPublicDataLen = ulPublicDataLen;
		this.pPublicData = pPublicData;
		this.ulPrivateDataLen = ulPrivateDataLen;
		this.hPrivateData = hPrivateData;
		this.ulPublicDataLen2 = ulPublicDataLen2;
		this.pPublicData2 = pPublicData2;
	}
	public CK_X9_42_DH2_DERIVE_PARAMS(Pointer peer) {
		super(peer);
	}
	public static class ByReference extends CK_X9_42_DH2_DERIVE_PARAMS implements Structure.ByReference {
		
	};
	public static class ByValue extends CK_X9_42_DH2_DERIVE_PARAMS implements Structure.ByValue {
		
	};
}
