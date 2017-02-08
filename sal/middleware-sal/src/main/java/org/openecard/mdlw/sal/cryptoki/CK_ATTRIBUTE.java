package org.openecard.mdlw.sal.cryptoki;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;

/**
 * <i>native declaration : pkcs11_v2.40/pkcs11t.h</i><br>
 * This file was autogenerated by
 * <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that
 * <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a
 * few opensource projects.</a>.<br>
 * For help, please visit
 * <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> ,
 * <a href="http://rococoa.dev.java.net/">Rococoa</a>, or
 * <a href="http://jna.dev.java.net/">JNA</a>.
 */
public class CK_ATTRIBUTE extends Structure {
    /** C type : CK_ATTRIBUTE_TYPE */
    public NativeLong type;

    public NativeLong getType() {
        return type;
    }

    public void setType(NativeLong type) {
        this.type = type;
    }

    /** C type : CK_VOID_PTR */
    public Pointer pValue;

    public Pointer getPValue() {
        return pValue;
    }

    public void setPValue(Pointer pValue) {
        this.pValue = pValue;
    }

    /**
     * in bytes<br>
     * C type : CK_ULONG
     */
    public NativeLong ulValueLen;

    public NativeLong getUlValueLen() {
        return ulValueLen;
    }

    public void setUlValueLen(NativeLong ulValueLen) {
        this.ulValueLen = ulValueLen;
    }

    public CK_ATTRIBUTE() {
        super(ALIGN_NONE);

    }

    protected List<String> getFieldOrder() {
        return Arrays.asList("type", "pValue", "ulValueLen");
    }

    /**
     * @param type
     *            C type : CK_ATTRIBUTE_TYPE<br>
     * @param pValue
     *            C type : CK_VOID_PTR<br>
     * @param ulValueLen
     *            in bytes<br>
     *            C type : CK_ULONG
     */
    public CK_ATTRIBUTE(NativeLong type, Pointer pValue, NativeLong ulValueLen) {
        super(ALIGN_NONE);
        this.type = type;
        this.pValue = pValue;
        this.ulValueLen = ulValueLen;

    }

    public CK_ATTRIBUTE(Pointer peer) {
        super(peer);

    }

    public static class ByReference extends CK_ATTRIBUTE implements Structure.ByReference {

    };

    public static class ByValue extends CK_ATTRIBUTE implements Structure.ByValue {

    };

}
