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

    @Value("${smtp.host}")
    private String SMTP_HOST;

    @Value("${smtp.port}")
    private String SMTP_PORT;

    @Value("${smtp.user}")
    private String EMAIL_USER;

    @Value("${smtp.password}")
    private String EMAIL_PASSWORD;

    private boolean sendEmail(EmailDetails emailDetails, boolean withAttachment)
            throws ErrorFileNotFound, ErrorCouldNotDeleteFile {
        try {
            validateFileExists(emailDetails.attachmentFilePath());

            Mailer mailer = buildMailer();

            Email email = withAttachment ? buildEmailWithAttachment(emailDetails)
                    : buildEmailWithoutAttachment(emailDetails);

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

    public boolean sendEmailWithFileAttachment(EmailDetails emailDetails)
            throws ErrorFileNotFound, ErrorCouldNotDeleteFile {
        return sendEmail(emailDetails, true);
    }

    public boolean sendEmailWithoutFileAttachment(EmailDetails emailDetails, Environment environment)
            throws ErrorFileNotFound, ErrorCouldNotDeleteFile {
        return sendEmail(emailDetails, false);
    }


    private Mailer buildMailer() {
        int smtpPortValue = Integer.parseInt(Objects.requireNonNull(SMTP_PORT));

        return MailerBuilder
                .withSMTPServer(
                        SMTP_HOST,
                        smtpPortValue, EMAIL_USER,
                        EMAIL_PASSWORD)
                .withTransportStrategy(TransportStrategy.SMTP_TLS)
                .withSessionTimeout(10 * 1000)
                .buildMailer();
    }

    private Email buildEmailWithAttachment(EmailDetails emailDetails) {
        DataSource dataSource = new FileDataSource(new File(emailDetails.attachmentFilePath()));

        return EmailBuilder.startingBlank()
                .from("PIPA", Objects.requireNonNull(EMAIL_USER))
                .to(emailDetails.recipientName(), emailDetails.recipientEmail())
                .withSubject(emailDetails.subject())
                .withPlainText(emailDetails.messageBody())
                .withAttachment(emailDetails.attachmentName(), dataSource)
                .buildEmail();
    }

    private Email buildEmailWithoutAttachment(EmailDetails emailDetails) {

        return EmailBuilder.startingBlank()
                .from("PIPA", Objects.requireNonNull(EMAIL_USER))
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
