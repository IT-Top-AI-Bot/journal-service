package com.aquadev.journalservice.client.journal;

import com.aquadev.journalservice.dto.response.JournalCountHomeworkResponse;
import com.aquadev.journalservice.dto.response.JournalHomeworkEvaluationResponse;
import com.aquadev.journalservice.dto.response.JournalHomeworkResponse;
import com.aquadev.journalservice.dto.response.JournalHomeworkUploadResponse;
import com.aquadev.journalservice.dto.response.JournalScheduleResponse;
import com.aquadev.journalservice.dto.response.JournalSpecResponse;
import com.aquadev.journalservice.dto.response.JournalUserResponse;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@HttpExchange
public interface JournalClient {

    @GetExchange("/settings/user-info")
    JournalUserResponse getCurrentUser();

    @GetExchange("/count/homework")
    List<JournalCountHomeworkResponse> getCountHomework(
            @RequestParam(required = false) Integer type,
            @RequestParam(value = "group_id", required = false) Integer groupId
    );

    @GetExchange("/homework/operations/list")
    List<JournalHomeworkResponse> getHomeworks(
            @RequestParam Integer page,
            @RequestParam Integer status,
            @RequestParam Integer type,
            @RequestParam("group_id") Integer groupId,
            @RequestParam(value = "spec_id", required = false) Integer specId
    );

    @GetExchange("/homework/operations/list")
    List<JournalHomeworkResponse> getHomeworks(
            @RequestParam Integer page,
            @RequestParam Integer status,
            @RequestParam Integer type,
            @RequestParam("group_id") Integer groupId,
            @RequestParam("spec_id[]") List<Integer> specIds
    );

    @GetExchange("/homework/evaluation/operations/get")
    Optional<JournalHomeworkEvaluationResponse> getHomeworkEvaluation(@RequestParam("id") Long homeworkId);

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

    @GetExchange("/schedule/operations/get-month")
    List<JournalScheduleResponse> getScheduleByDate(@RequestParam("date-filter") LocalDate date);

    @GetExchange("/settings/group-specs")
    List<JournalSpecResponse> getGroupSpecs();
}
