package com.aquadev.journalservice.client.journal;

import com.aquadev.journalservice.dto.response.JournalHomeworkUploadResponse;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.io.InputStream;

@HttpExchange
public interface JournalHomeworkSubmissionClient {

    @PostExchange(value = "/homework/operations/create", contentType = MediaType.MULTIPART_FORM_DATA_VALUE)
    JournalHomeworkUploadResponse doUploadHomework(@RequestBody MultiValueMap<String, HttpEntity<?>> parts);

    default JournalHomeworkUploadResponse uploadHomework(Long homeworkId, InputStream file, long fileSize, String filename) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("id", homeworkId);
        builder.part("spentTimeHour", "00");
        builder.part("spentTimeMinute", "01");
        builder.part("file", new InputStreamResource(file) {
            @Override
            public long contentLength() {
                return fileSize;
            }

            @Override
            public String getFilename() {
                return filename;
            }
        }).contentType(MediaType.APPLICATION_OCTET_STREAM);
        return doUploadHomework(builder.build());
    }

    default JournalHomeworkUploadResponse uploadHomeworkText(Long homeworkId, String text) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("id", homeworkId);
        builder.part("answerText", text);
        builder.part("spentTimeHour", "00");
        builder.part("spentTimeMinute", "01");
        return doUploadHomework(builder.build());
    }
}
