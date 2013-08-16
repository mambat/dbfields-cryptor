package org.n3r.sensitive.proxy;

public interface SensitiveCryptor {
    String encrypt(String data);
    String decrypt(String data);
}
