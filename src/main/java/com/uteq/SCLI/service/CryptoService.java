package com.uteq.SCLI.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class CryptoService {

    private static final String ALGORITMO = "AES/GCM/NoPadding";
    private static final int TAMANO_TAG_GCM_BITS = 128;
    private static final int TAMANO_IV_BYTES = 12;

    private final SecretKeySpec claveSecreta;

    public CryptoService(@Value("${app.security.aes-key}") String claveBase64) {
        byte[] claveBytes = Base64.getDecoder().decode(claveBase64);
        if (claveBytes.length != 32) {
            throw new IllegalStateException(
                "AES_SECRET_KEY debe decodificar a exactamente 32 bytes (256 bits). " +
                "Bytes actuales: " + claveBytes.length);
        }
        this.claveSecreta = new SecretKeySpec(claveBytes, "AES");
    }

    /** Cifra un texto plano. Devuelve un texto Base64 que incluye el IV + el contenido cifrado. */
    public String encriptar(String textoPlano) {
        if (textoPlano == null) return null;
        try {
            byte[] iv = new byte[TAMANO_IV_BYTES];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.ENCRYPT_MODE, claveSecreta, new GCMParameterSpec(TAMANO_TAG_GCM_BITS, iv));
            byte[] textoCifrado = cipher.doFinal(textoPlano.getBytes("UTF-8"));

            // El IV se guarda junto con el texto cifrado (no es secreto, solo debe ser único por mensaje)
            byte[] resultado = new byte[iv.length + textoCifrado.length];
            System.arraycopy(iv, 0, resultado, 0, iv.length);
            System.arraycopy(textoCifrado, 0, resultado, iv.length, textoCifrado.length);

            return Base64.getEncoder().encodeToString(resultado);
        } catch (Exception e) {
            throw new RuntimeException("Error al encriptar el dato", e);
        }
    }

    /** Descifra un texto que fue cifrado con encriptar(...). */
    public String desencriptar(String textoCifradoBase64) {
        if (textoCifradoBase64 == null || textoCifradoBase64.isBlank()) return textoCifradoBase64;
        try {
            byte[] datos = Base64.getDecoder().decode(textoCifradoBase64);
            byte[] iv = new byte[TAMANO_IV_BYTES];
            System.arraycopy(datos, 0, iv, 0, TAMANO_IV_BYTES);
            byte[] textoCifrado = new byte[datos.length - TAMANO_IV_BYTES];
            System.arraycopy(datos, TAMANO_IV_BYTES, textoCifrado, 0, textoCifrado.length);

            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.DECRYPT_MODE, claveSecreta, new GCMParameterSpec(TAMANO_TAG_GCM_BITS, iv));
            byte[] textoPlano = cipher.doFinal(textoCifrado);

            return new String(textoPlano, "UTF-8");
        } catch (Exception e) {
            // Si el dato no está cifrado (ej. datos viejos en texto plano), lo devolvemos tal cual
            // en vez de tronar toda la pantalla — mejor UX mientras se migra el dato existente.
            return textoCifradoBase64;
        }
    }
}