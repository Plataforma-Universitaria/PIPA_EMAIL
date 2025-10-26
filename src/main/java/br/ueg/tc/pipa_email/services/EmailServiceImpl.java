package br.ueg.tc.pipa_email.services;

import br.ueg.tc.pipa_integrator.exceptions.files.ErrorCouldNotCreateFile;
import br.ueg.tc.pipa_integrator.exceptions.files.ErrorCouldNotDeleteFile;
import br.ueg.tc.pipa_integrator.exceptions.files.ErrorFileNotFound;
import br.ueg.tc.pipa_integrator.interfaces.providers.EmailDetails;
import br.ueg.tc.pipa_integrator.interfaces.providers.IEmailService;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class EmailServiceImpl implements IEmailService {
    private final EmailSenderService emailSenderService;

    public EmailServiceImpl(EmailSenderService emailSenderService) {
        this.emailSenderService = emailSenderService;
    }
    public String HTMLToPDF(String htmlString, Path folderPath, String filePrefix)
            throws ErrorCouldNotCreateFile {
        System.out.println("HTMLToPDF");
        return HtmlConverter.generate(htmlString, folderPath, filePrefix);
    }

    public boolean sendEmailWithFileAttachment(EmailDetails emailDetails)
            throws ErrorFileNotFound, ErrorCouldNotDeleteFile {
        System.out.println("sendEmailWithFileAttachment");
        return emailSenderService.sendEmailWithFileAttachment(emailDetails);
    }
}