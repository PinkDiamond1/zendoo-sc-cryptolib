package com.horizen.schnorrnative;

import com.horizen.librustsidechains.Library;

public class SchnorrSignature
{

  public static int SIGNATURE_LENGTH = 192;

  private long signaturePointer;

  static {
    Library.load();
  }

  private SchnorrSignature(long signaturePointer) {
    if (signaturePointer == 0)
      throw new IllegalArgumentException("Signature pointer must be not null.");
    this.signaturePointer = signaturePointer;
  }

  public SchnorrSignature() {
    this.signaturePointer = 0;
  }

  private static native byte[] nativeSerializeSignature(long signaturePointer);

  private static native SchnorrSignature nativeDeserializeSignature(byte[] signatureBytes, boolean checkSignature);

  private static native void nativefreeSignature(long signaturePointer);

  public static SchnorrSignature deserialize(byte[] signatureBytes, boolean checkSignature) {
    if (signatureBytes.length != SIGNATURE_LENGTH)
      throw new IllegalArgumentException(String.format("Incorrect signature length, %d expected, %d found", SIGNATURE_LENGTH, signatureBytes.length));

    return nativeDeserializeSignature(signatureBytes, checkSignature);
  }

  public byte[] serializeSignature() {
    return nativeSerializeSignature(this.signaturePointer);
  }

  private native boolean nativeIsValidSignature(); // jni call to Rust impl

  public boolean isValidSignature() {
    if (signaturePointer == 0)
      throw new IllegalArgumentException("Schnorr signature was freed.");

    return nativeIsValidSignature();
  }


  public void freeSignature() {
    if (signaturePointer != 0) {
      nativefreeSignature(this.signaturePointer);
      signaturePointer = 0;
    }
  }
}

