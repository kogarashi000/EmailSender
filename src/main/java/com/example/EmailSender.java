package com.example;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class EmailSender {
    /**
     * Логгер для записи информационных сообщений и ошибок работы приложения.
     */
    private static final Logger logger = LoggerFactory.getLogger(EmailSender.class);

    /**
     * SMTP-хост для отправки писем (Mail.ru).
     */
    private static final String SMTP_HOST = "smtp.mail.ru";

    /**
     * Порт SMTP-сервера для SSL-соединения.
     */
    private static final int SMTP_PORT = 465; // Для SSL

    /**
     * Тема электронного письма, отправляемого получателям.
     */
    private static final String SUBJECT = "информационное сообщение";

    /**
     * Точка входа в приложение. Выполняет следующие шаги:
     * <ul>
     *     <li>Дешифрует учетные данные отправителя.</li>
     *     <li>Читает контакты из CSV-файла.</li>
     *     <li>Читает шаблон письма из текстового файла.</li>
     *     <li>Настраивает свойства SMTP-сессии и аутентификацию.</li>
     *     <li>Отправляет персонализированные письма каждому контакту с задержкой между отправками.</li>
     * </ul>
     *
     * @param args аргументы командной строки (не используются)
     */
    public static void main(String[] args) {
        try {
            // Дешифрование учетных данных отправителя
            String[] credentials = CredentialsManager.decryptCredentials();
            String senderEmail = credentials[0];
            String senderPassword = credentials[1];
            logger.info("Учетные данные успешно расшифрованы.");

            // Чтение списка контактов из CSV-файла
            List<Contact> contacts = readContacts("src/main/resources/contacts.csv");
            logger.info("Прочитано {} контактов.", contacts.size());

            // Чтение шаблона сообщения
            String template = readMessageTemplate("src/main/resources/message.txt");
            logger.info("Шаблон сообщения успешно прочитан.");

            // Настройка свойств SMTP
            Properties props = new Properties();
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", String.valueOf(SMTP_PORT));
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.ssl.trust", SMTP_HOST);

            // Создание SMTP-сессии с аутентификацией
            Session session = Session.getInstance(props, new Authenticator() {
                /**
                 * Возвращает объект PasswordAuthentication с учетными данными отправителя.
                 *
                 * @return учетные данные (email и пароль приложения)
                 */
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(senderEmail, senderPassword);
                }
            });

            // Отправка писем каждому контакту
            for (Contact contact : contacts) {
                try {
                    // Подстановка имени контакта в шаблон сообщения
                    String personalizedMessage = template.replace("{name}", contact.getName());

                    // Создание MIME-сообщения
                    Message message = new MimeMessage(session);
                    message.setFrom(new InternetAddress(senderEmail));
                    message.setRecipients(
                            Message.RecipientType.TO,
                            InternetAddress.parse(contact.getEmail())
                    );
                    message.setSubject(SUBJECT);
                    message.setText(personalizedMessage);

                    // Отправка сообщения
                    Transport.send(message);
                    logger.info("Письмо успешно отправлено: {}", contact.getEmail());
                    System.out.println("Письмо отправлено: " + contact.getEmail());

                    // Задержка между отправками (1 секунда)
                    Thread.sleep(1000);
                } catch (MessagingException e) {
                    logger.error("Ошибка при отправке письма {}: {}", contact.getEmail(), e.getMessage());
                    System.err.println("Ошибка при отправке письма " + contact.getEmail() + ": " + e.getMessage());
                } catch (InterruptedException e) {
                    logger.error("Задержка прервана: {}", e.getMessage());
                    Thread.currentThread().interrupt();
                }
            }

        } catch (Exception e) {
            logger.error("Критическая ошибка: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Читает список контактов из указанного CSV-файла.
     * Первая строка считается заголовком и пропускается.
     * Каждая последующая строка должна содержать email и имя контакта.
     *
     * @param filePath путь к CSV-файлу с контактами
     * @return список объектов Contact, содержащих email и имя получателя
     * @throws IOException если происходит ошибка при чтении файла
     * @throws CsvValidationException если формат CSV-файла некорректен
     */
    private static List<Contact> readContacts(String filePath) throws IOException, CsvValidationException {
        List<Contact> contacts = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] nextLine;
            // Пропуск заголовка
            reader.readNext();
            while ((nextLine = reader.readNext()) != null) {
                if (nextLine.length >= 2) {
                    String email = nextLine[0].trim();
                    String name = nextLine[1].trim();
                    if (!email.isEmpty() && !name.isEmpty()) {
                        contacts.add(new Contact(email, name));
                    }
                }
            }
        }
        return contacts;
    }

    /**
     * Читает шаблон сообщения из указанного текстового файла.
     * Шаблон может содержать плейсхолдер "{name}", который будет заменен на имя получателя.
     *
     * @param filePath путь к текстовому файлу с шаблоном сообщения
     * @return строка, содержащая содержимое шаблона сообщения
     * @throws IOException если происходит ошибка при чтении файла
     */
    private static String readMessageTemplate(String filePath) throws IOException {
        StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                contentBuilder.append(sCurrentLine).append("\n");
            }
        }
        return contentBuilder.toString();
    }
}
