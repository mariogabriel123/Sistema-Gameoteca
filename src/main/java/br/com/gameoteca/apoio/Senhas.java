package br.com.gameoteca.apoio;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** PBKDF2 com salt aleatório por senha. Não é criptografia reversível. */
public final class Senhas {
    private static final int ITERACOES = 600_000;
    private static final SecureRandom RANDOM = new SecureRandom();
    private Senhas() { }
    public static String gerar(char[] senha) {
        byte[] salt = new byte[16]; RANDOM.nextBytes(salt);
        return "pbkdf2_sha256$" + ITERACOES + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(derivar(senha, salt, ITERACOES));
    }
    public static boolean conferir(char[] senha, String armazenada) {
        if (armazenada == null) return false;
        try {
            String[] partes = armazenada.split("\\$");
            if (partes.length != 4 || !partes[0].equals("pbkdf2_sha256")) return false;
            int iteracoes = Integer.parseInt(partes[1]);
            if (iteracoes < 100_000 || iteracoes > 2_000_000) return false;
            byte[] salt = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            return MessageDigest.isEqual(esperado, derivar(senha, salt, iteracoes));
        } catch (IllegalArgumentException e) { return false; }
    }
    private static byte[] derivar(char[] senha, byte[] salt, int iteracoes) {
        PBEKeySpec spec = new PBEKeySpec(senha, salt, iteracoes, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível proteger a senha.", e);
        } finally { spec.clearPassword(); }
    }
}
