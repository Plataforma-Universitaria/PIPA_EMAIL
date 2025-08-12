package br.ueg.tc.pipa_email.services;

import br.ueg.tc.pipa_integrator.exceptions.files.ErrorCouldNotDeleteFile;
import br.ueg.tc.pipa_integrator.exceptions.files.ErrorFileNotFound;
import br.ueg.tc.pipa_integrator.interfaces.providers.EmailDetails;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import lombok.extern.slf4j.Slf4j;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Objects;

@Service
@Slf4j
public class EmailSenderService {

    private static final String SMTP_HOST = "smtp_host";
    private static final String SMTP_PORT = "smtp_port";
    private static final String EMAIL_USER = "email_user";
    private static final String EMAIL_PASSWORD = "email_password";

    private static boolean sendEmail(EmailDetails emailDetails, Environment environment, boolean withAttachment)
            throws ErrorFileNotFound, ErrorCouldNotDeleteFile {
        try {
            validateFileExists(emailDetails.attachmentFilePath());

            Mailer mailer = buildMailer(environment);

            Email email = withAttachment ? buildEmailWithAttachment(emailDetails, environment)
                    : buildEmailWithoutAttachment(emailDetails, environment);

            Thread emailThread = new Thread(() -> {
                try {
                    mailer.sendMail(email);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    deleteFile(emailDetails.attachmentFilePath());
                }
            });

            emailThread.start();
            return true;
        } catch (ErrorFileNotFound | ErrorCouldNotDeleteFile e) {
            throw e;
        } catch (Exception e) {
            deleteFile(emailDetails.attachmentFilePath());
            return false;
        }
    }

    public static boolean sendEmailWithFileAttachment(EmailDetails emailDetails, Environment environment)
            throws ErrorFileNotFound, ErrorCouldNotDeleteFile {
        return sendEmail(emailDetails, environment, true);
    }

    public static boolean sendEmailWithoutFileAttachment(EmailDetails emailDetails, Environment environment)
            throws ErrorFileNotFound, ErrorCouldNotDeleteFile {
        return sendEmail(emailDetails, environment, false);
    }


    private static Mailer buildMailer(Environment environment) {
        int smtpPortValue = Integer.parseInt(Objects.requireNonNull(environment.getProperty(SMTP_PORT)));

        return MailerBuilder
                .withSMTPServer(
                        environment.getProperty(SMTP_HOST),
                        smtpPortValue, environment.getProperty(EMAIL_USER),
                        environment.getProperty(EMAIL_PASSWORD))
                .withTransportStrategy(TransportStrategy.SMTP_TLS)
                .withSessionTimeout(10 * 1000)
                .buildMailer();
    }

    private static Email buildEmailWithAttachment(EmailDetails emailDetails, Environment environment) {
        DataSource dataSource = new FileDataSource(new File(emailDetails.attachmentFilePath()));

        return EmailBuilder.startingBlank()
                .from("PIPA", Objects.requireNonNull(environment.getProperty(EMAIL_USER)))
                .to(emailDetails.recipientName(), emailDetails.recipientEmail())
                .withSubject(emailDetails.subject())
                .withPlainText(emailDetails.messageBody())
                .withAttachment(emailDetails.attachmentName(), dataSource)
                .buildEmail();
    }

    private static Email buildEmailWithoutAttachment(EmailDetails emailDetails, Environment environment) {

        return EmailBuilder.startingBlank()
                .from("PIPA", Objects.requireNonNull(environment.getProperty(EMAIL_USER)))
                .to(emailDetails.recipientName(), emailDetails.recipientEmail())
                .withSubject(emailDetails.subject())
                .withPlainText(emailDetails.messageBody())
                .buildEmail();
    }

    private static void validateFileExists(String filePath) throws ErrorFileNotFound {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new ErrorFileNotFound(new Object[]{filePath});
        }
    }

    private static void deleteFile(String filePath) throws ErrorCouldNotDeleteFile {
        File file = new File(filePath);
        if (!file.delete()) {
            throw new ErrorCouldNotDeleteFile(new Object[]{filePath});
        }
    }


}
