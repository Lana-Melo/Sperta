import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class EncryptAttestationPath {
    private static final String PBE_ALGORITHM = "PBEWithHmacSHA256AndAES_128";
    private static final byte[] PBE_SALT = { (byte) 0x12, (byte) 0x3f, (byte) 0xa1, (byte) 0x33, (byte) 0x81, (byte) 0x3f, (byte) 0xff, (byte) 0x11, };
    private static final int PBE_ITERATIONS = 20;

    private EncryptAttestationPath() {}

    /**
     * This program receives a path to a file and the password to generate a PBE key
     * It encrypts the file of given path
     * Before encripting it, adds the length of the parameters of the cipher key
     * And after the length of the parameters, adds the parameters themselves
     * and only then, the encrypted content:
     * 
     * 
     * paramlen:params:content
     * 
     * PS: The ':' are merely for visual reference, they are not written onto the file
     * 
     * @param args
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                "Usage: java EncryptAttestationPath <path-to-file-you-want-to-encrypt> <pbe-password>"
            );
        }

        String pathToEncrypt = args[0];
        String password = args[1];

        try {
            SecretKey key = setPBKey(password);
            Cipher cipher = getEncryptionCipher(key);

            byte[] clearContent = Files.readAllBytes(Path.of(pathToEncrypt));
            byte[] encryptedContent = cipher.doFinal(clearContent);
            byte[] params = cipher.getParameters().getEncoded();

            try (DataOutputStream dos = new DataOutputStream(Files.newOutputStream(Path.of(pathToEncrypt)))) {
                dos.writeInt(params.length);
                dos.write(params);
                dos.write(encryptedContent);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt file: " + pathToEncrypt, e);
        }
    }

    private static Cipher getEncryptionCipher(SecretKey key) {
        Cipher c = getCipherPBEWithHmacSHA256AndAES128Instance();
        try {
            c.init(Cipher.ENCRYPT_MODE, key);
        } catch (InvalidKeyException e) {
            throw new AssertionError("Couldn't initialize encryption cipher", e);
        }
        return c;
    }

    private static Cipher getCipherPBEWithHmacSHA256AndAES128Instance() {
        try {
            return Cipher.getInstance(PBE_ALGORITHM);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException e) {
            throw new AssertionError("Couldn't get cipher instance for required algorithm", e);
        }
    }

    public static SecretKey setPBKey(String password) {
        PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), PBE_SALT, PBE_ITERATIONS); // pass, salt, iterations
        SecretKeyFactory kf = newPBEWithHmacSHA256AndAES128Factory();
        return generatePBESecretKey(kf, keySpec);
    }

    private static SecretKey generatePBESecretKey(SecretKeyFactory kf, PBEKeySpec keySpec) {
        try {
            return kf.generateSecret(keySpec);
        } catch (InvalidKeySpecException e) {
            throw new AssertionError("Couldn't generate the SecretKey", e);
        }
    }

    private static SecretKeyFactory newPBEWithHmacSHA256AndAES128Factory() {
        try {
            return SecretKeyFactory.getInstance(PBE_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("Missing required algorithm: " + PBE_ALGORITHM, e);
        }
    }
}
