package com.frietsync.backend.service.impl.document;

import com.frietsync.backend.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.apache.tika.exception.WriteLimitReachedException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import java.io.IOException;
@Slf4j
@Component
public class DocumentTextExtractor {

    private static final int MAX_EXTRACTED_CHARACTERS = 50_000;

    public String extract(MultipartFile file, String fileName) {
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler(MAX_EXTRACTED_CHARACTERS);
        Metadata metadata = new Metadata();
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);

        try (TikaInputStream input = TikaInputStream.get(file.getInputStream())) {
            try {
                parser.parse(input, handler, metadata);
            } catch (SAXException exception) {
                if (!WriteLimitReachedException.isWriteLimitReached(exception)) {
                    throw exception;
                }
            }
            return handler.toString().strip();
        } catch (IOException | SAXException | TikaException exception) {
            log.warn("Could not extract text from uploaded project document {}", fileName, exception);
            throw new BadRequestException("Could not extract text from the uploaded document");
        }
    }
}
