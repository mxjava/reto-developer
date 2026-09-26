package com.challenge.transaction.gateway.infrastructure.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AesCryptoService {
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final int AES_256_KEY_BYTES = 32;
    private final byte[] key;

    public AesCryptoService(@Value("${app.aes.key-base64}") String encodedKey) {
        final byte[] decodedKey;
        try {
            decodedKey = Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("APP_AES_KEY_BASE64 no contiene Base64 valido", ex);
        }
        if (decodedKey.length != AES_256_KEY_BYTES) {
            throw new IllegalStateException(
                    "APP_AES_KEY_BASE64 debe representar exactamente 32 bytes aleatorios para AES-256");
        }
        // Se conserva una copia privada para que el material criptografico no dependa del arreglo
        // recibido.
        this.key = Arrays.copyOf(decodedKey, decodedKey.length);
    }

    public String decrypt(String encodedPayload) {
        try {
            byte[] payload = Base64.getDecoder().decode(encodedPayload);
            // GCM necesita IV de 96 bits y, al menos, el tag de autenticación de 128 bits.
            if (payload.length <= IV_LENGTH + 16) {
                throw new IllegalArgumentException("Payload AES invalido");
            }
            byte[] iv = Arrays.copyOfRange(payload, 0, IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(payload, IV_LENGTH, payload.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (GeneralSecurityException ex) {
            // No incluir ciphertext, llave, IV ni detalles del proveedor criptográfico en
            // logs/respuestas.
            throw new IllegalArgumentException("Secreto cifrado invalido");
        }
    }
}
