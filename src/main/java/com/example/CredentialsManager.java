package com.example;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.*;
import java.security.SecureRandom;
import java.util.Base64;

public class CredentialsManager {
    private static final String KEY_FILE = "secret.key";
    private static final String CREDENTIALS_FILE = "credentials.enc";
    private static final int KEY_SIZE = 128; // Бит
    private static final int T_LEN = 128; // Для GCM

    // Генерация и сохранение секретного ключа
    public static void generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(KEY_SIZE);
        SecretKey secretKey = keyGen.generateKey();
        try (FileOutputStream fos = new FileOutputStream(KEY_FILE)) {
            fos.write(secretKey.getEncoded());
        }
    }

    // Шифрование учетных данных и сохранение в файл
    public static void encryptCredentials(String email, String password) throws Exception {
        SecretKey secretKey = loadKey();

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[12]; // инициализированный вектор
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        GCMParameterSpec spec = new GCMParameterSpec(T_LEN, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

        String credentials = email + ":" + password;
        byte[] encrypted = cipher.doFinal(credentials.getBytes("UTF-8"));

        // Сохранение IV и зашифрованных данных, разделенных двоеточием
        String encodedIV = Base64.getEncoder().encodeToString(iv);
        String encodedEncrypted = Base64.getEncoder().encodeToString(encrypted);
        String combined = encodedIV + ":" + encodedEncrypted;

        try (FileWriter fw = new FileWriter(CREDENTIALS_FILE)) {
            fw.write(combined);
        }
    }

    // Дешифрование учетных данных
    public static String[] decryptCredentials() throws Exception {
        SecretKey secretKey = loadKey();

        BufferedReader reader = new BufferedReader(new FileReader(CREDENTIALS_FILE));
        String line = reader.readLine();
        reader.close();

        String[] parts = line.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Неверный формат файла учетных данных.");
        }

        byte[] iv = Base64.getDecoder().decode(parts[0]);
        byte[] encrypted = Base64.getDecoder().decode(parts[1]);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(T_LEN, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

        byte[] decrypted = cipher.doFinal(encrypted);
        String credentials = new String(decrypted, "UTF-8");

        String[] creds = credentials.split(":");
        if (creds.length != 2) {
            throw new IllegalArgumentException("Неверный формат расшифрованных данных.");
        }

        return creds;
    }

    // Загрузка ключа из файла
    private static SecretKey loadKey() throws Exception {
        File keyFile = new File(KEY_FILE);
        if (!keyFile.exists()) {
            throw new FileNotFoundException("Файл ключа не найден. Сгенерируйте ключ сначала.");
        }
        byte[] keyBytes = new byte[KEY_SIZE / 8];
        try (FileInputStream fis = new FileInputStream(keyFile)) {
            if (fis.read(keyBytes) != keyBytes.length) {
                throw new IOException("Неверный размер ключа.");
            }
        }
        return new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
    }

    // Пример использования
    public static void main(String[] args) {
        try {
            // Генерация ключа (выполняется один раз)
            // generateKey();
            // System.out.println("Ключ успешно сгенерирован.");

            // Шифрование учетных данных (выполняется один раз)
            // encryptCredentials("test.email.05@mail.ru", "aBG8BisVH8M3sb9iRau9");
            // System.out.println("Учетные данные успешно зашифрованы.");

            // Дешифрование учетных данных
            // String[] creds = decryptCredentials();
            // System.out.println("Email: " + creds[0]);
            // System.out.println("Password: " + creds[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
