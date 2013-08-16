package org.n3r.sensitive.proxy;

import org.n3r.core.security.AesCryptor;

public class AesSensitiveCrypter implements SensitiveCryptor{
    private AesCryptor aesCryptor = new AesCryptor("rocket");

    @Override
    public String encrypt(String data) {
        return aesCryptor.encrypt(data);
    }

    @Override
    public String decrypt(String data) {
        return aesCryptor.decrypt(data);
    }
}
