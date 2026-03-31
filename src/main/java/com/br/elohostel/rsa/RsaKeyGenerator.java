package com.br.elohostel.rsa;

import java.io.*;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RsaKeyGenerator {

    private static final Logger logger = Logger.getLogger(RsaKeyGenerator.class.getName());

    private static final String PUBLIC_KEY_FILE = "public.key";
    private static final String PRIVATE_KEY_FILE = "private.key";

    private final RSAPublicKey publicKey;
    private final RSAPrivateKey privateKey;

    public RsaKeyGenerator() {
        File publicKeyFile = new File(PUBLIC_KEY_FILE);
        File privateKeyFile = new File(PRIVATE_KEY_FILE);

        if (publicKeyFile.exists() && privateKeyFile.exists()) {
            this.publicKey = loadPublicKey(publicKeyFile);
            this.privateKey = loadPrivateKey(privateKeyFile);
            logger.log(Level.INFO, "Public key loaded successfully.");
            logger.log(Level.INFO, "Private key loaded successfully.");
        } else {
            KeyPair keyPair = generateRsaKey();
            this.publicKey = (RSAPublicKey) keyPair.getPublic();
            this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
            saveKeyToFile(PUBLIC_KEY_FILE, this.publicKey.getEncoded());
            saveKeyToFile(PRIVATE_KEY_FILE, this.privateKey.getEncoded());
            logger.log(Level.INFO, "Public key generated successfully.");
            logger.log(Level.INFO, "Private key generated successfully.");
        }
    }

    private KeyPair generateRsaKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "Erro ao gerar par de chaves RSA", e.getMessage());
            throw new RuntimeException("Erro ao gerar par de chaves RSA", e);
        }
    }

    private void saveKeyToFile(String filename, byte[] key) {
        try (FileOutputStream fos = new FileOutputStream(filename)) {
            fos.write(key);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erro ao salvar chave no arquivo", e.getMessage());
            throw new RuntimeException("Erro ao salvar chave no arquivo", e);
        }
    }

    private RSAPublicKey loadPublicKey(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] keyBytes = fis.readAllBytes();
            return (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao carregar chave pública", e.getMessage());
            throw new RuntimeException("Erro ao carregar chave pública", e);
        }
    }

    private RSAPrivateKey loadPrivateKey(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] keyBytes = fis.readAllBytes();
            return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao carregar chave privada", e.getMessage());
            throw new RuntimeException("Erro ao carregar chave privada", e);
        }
    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    public RSAPrivateKey getPrivateKey() {
        return privateKey;
    }
}