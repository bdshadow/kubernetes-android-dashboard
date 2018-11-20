package bdshadow.org.kubernetes.android.dashboard.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.security.auth.x500.X500Principal;

import bdshadow.org.kubernetes.android.dashboard.exception.BrokenSecureStoreDataException;
import bdshadow.org.kubernetes.android.dashboard.exception.SecureStoreNotSupportedException;

// https://proandroiddev.com/security-best-practices-symmetric-encryption-with-aes-in-java-7616beaaade9
// https://github.com/temyco/security-workshop-sample
public class EncryptionUtils {

    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String ALIAS = "kubernetes-android-dashboard";
    private static final String X500_PRINCIPAL_NAME = "CN=kubernetes android dashboard, OU=open source, O=org.bdshadow";
    private static final String IV_SEPARATOR = "]";
    private static final String TRANSFORMATION_ASYMMETRIC = "RSA/ECB/PKCS1Padding";
    private static final String TRANSFORMATION_SYMMETRIC = "AES/CBC/PKCS7Padding";
    private static final String SHARED_PREFS_ASYM_KEY = "encryption-key";
    private static final String SHARED_PREFS_FILE_NAME = "encryption-keys";

    private EncryptionUtils() {

    }

    /**
     * @return encrypted symmetric key
     */
    public static String encryptString(String toEncrypt, Context context) throws SecureStoreNotSupportedException, BrokenSecureStoreDataException {
        if (toEncrypt == null) {
            return null;
        }
        SecretKey symmetricKey;
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS_FILE_NAME, Context.MODE_PRIVATE);
        // Creates Cipher with asymmetric transformation and provides wrap and unwrap functions
        try {
            Cipher cipherForWrapping = Cipher.getInstance(TRANSFORMATION_ASYMMETRIC);
            if (!sharedPreferences.contains(SHARED_PREFS_ASYM_KEY)) {
                // Create AES BC provider key
                symmetricKey = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES).generateKey();
                // Create RSA AndroidKeyStore Provider key and save it into keystore
                KeyPair masterKey = createKeys();
                // Wrap AES Secret key with RSA Public key
                cipherForWrapping.init(Cipher.WRAP_MODE, masterKey.getPublic());
                byte[] decodedData = cipherForWrapping.wrap(symmetricKey);
                String encryptedSymmetricKey = Base64.encodeToString(decodedData, Base64.DEFAULT);
                sharedPreferences.edit().putString(SHARED_PREFS_ASYM_KEY, encryptedSymmetricKey).apply();
            } else {
                String encryptedSymmetricKey = sharedPreferences.getString(SHARED_PREFS_ASYM_KEY, "default");
                byte[] encryptedSymmetricKeyData = Base64.decode(encryptedSymmetricKey, Base64.DEFAULT);
                KeyPair masterKey = getSecretKey();
                cipherForWrapping.init(Cipher.UNWRAP_MODE, masterKey.getPrivate());
                symmetricKey = (SecretKey) cipherForWrapping.unwrap(
                        encryptedSymmetricKeyData, KeyProperties.KEY_ALGORITHM_AES, Cipher.SECRET_KEY);
            }
            // Creates Cipher with symmetric transformation and provides encrypt and decrypt functions
            Cipher cipherForEncryption = Cipher.getInstance(TRANSFORMATION_SYMMETRIC);
            cipherForEncryption.init(Cipher.ENCRYPT_MODE, symmetricKey);
            byte[] iv = cipherForEncryption.getIV();
            String ivString = Base64.encodeToString(iv, Base64.DEFAULT);

            byte[] encryptedBytes = cipherForEncryption.doFinal(toEncrypt.getBytes());
            return ivString + IV_SEPARATOR + Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new SecureStoreNotSupportedException(e);
        } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            throw new BrokenSecureStoreDataException(e);
        }
    }

    public static String decryptString(String encrypted, Context context) throws SecureStoreNotSupportedException, BrokenSecureStoreDataException {
        String[] split = encrypted.split(IV_SEPARATOR);
        if (split.length != 2) {
            throw new IllegalArgumentException("Passed data is incorrect. There was no IV specified with it.");
        }
        String ivString = split[0];
        String encodedString = split[1];

        try {
            KeyPair keyPair = getSecretKey();
            // Creates Cipher with symmetric transformation and provides encrypt and decrypt functions
            Cipher cipherForWrapping = Cipher.getInstance(TRANSFORMATION_ASYMMETRIC);
            String encryptionKey = context.getSharedPreferences(
                    SHARED_PREFS_FILE_NAME, Context.MODE_PRIVATE).getString(SHARED_PREFS_ASYM_KEY, "default");
            cipherForWrapping.init(Cipher.UNWRAP_MODE, keyPair.getPrivate());
            byte[] encryptionKeyData = Base64.decode(encryptionKey, Base64.DEFAULT);
            Key symmetricKey = cipherForWrapping.unwrap(encryptionKeyData, KeyProperties.KEY_ALGORITHM_AES, Cipher.SECRET_KEY);

            Cipher cipherForEncryption = Cipher.getInstance(TRANSFORMATION_SYMMETRIC);
            AlgorithmParameterSpec ivSpec = new IvParameterSpec(Base64.decode(ivString, Base64.DEFAULT));
            cipherForEncryption.init(Cipher.DECRYPT_MODE, symmetricKey, ivSpec);
            byte[] encryptedData = cipherForEncryption.doFinal(Base64.decode(encodedString, Base64.DEFAULT));
            return new String(encryptedData);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new SecureStoreNotSupportedException(e);
        } catch (InvalidAlgorithmParameterException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            throw new BrokenSecureStoreDataException(e);
        }
    }

    private static KeyPair getSecretKey() throws SecureStoreNotSupportedException, BrokenSecureStoreDataException {
        try {
            KeyStore ks = KeyStore.getInstance(KEYSTORE);

            // Weird artifact of Java API.  If you don't have an InputStream to load, you still need
            // to call "load", or it'll crash.
            ks.load(null);
            Key key = ks.getKey(ALIAS, null);
            PublicKey publicKey = ks.getCertificate(ALIAS).getPublicKey();

            if (key instanceof PrivateKey && publicKey != null) {
                return new KeyPair(publicKey, (PrivateKey) key);
            }
            throw new BrokenSecureStoreDataException();
        } catch (KeyStoreException | NoSuchAlgorithmException e) {
            throw new SecureStoreNotSupportedException(e);
        } catch (UnrecoverableKeyException | CertificateException | IOException e) {
            throw new BrokenSecureStoreDataException(e);
        }
    }

    /**
     * Creates a public and private key and stores it using the Android Key Store, so that only
     * this application will be able to access the keys.
     */
    private static KeyPair createKeys() throws SecureStoreNotSupportedException {
        try {
            // The KeyPairGeneratorSpec object is how parameters for your key pair are passed
            // to the KeyPairGenerator.
            AlgorithmParameterSpec spec = new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setCertificateSubject(new X500Principal(X500_PRINCIPAL_NAME))
                    .setBlockModes(KeyProperties.BLOCK_MODE_ECB)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                    .build();

            // Initialize a KeyPair generator using the the intended algorithm (in this example, RSA
            // and the KeyStore.  This example uses the AndroidKeyStore.
            final KeyPairGenerator kpGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, KEYSTORE);
            kpGenerator.initialize(spec);

            return kpGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | NoSuchProviderException e) {
            throw new SecureStoreNotSupportedException(e);
        }
    }
}
