package ch.dboeckli.template.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import lombok.extern.slf4j.Slf4j;
import nl.altindag.log.LogCaptor;
import nl.altindag.log.model.LogEvent;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.zalando.logbook.Logbook;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc
@AutoConfigureObservability
@Slf4j
@ActiveProfiles("local")
public class LoggingWithTracingTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void actuator_info_logbook_logsMessage() {
        try (LogCaptor logCaptor = LogCaptor.forClass(Logbook.class)) {
            String url = "http://localhost:" + port + "/actuator/info";
            restTemplate.getForEntity(url, String.class);

            List<LogEvent> logEvents = logCaptor.getLogEvents();

            assertAll(
                () -> assertNotNull(logEvents),
                () -> assertEquals(2, logEvents.size()),
                () -> assertEquals(Logbook.class.getName(), logEvents.getFirst().getLoggerName()),
                () -> assertThat(logEvents.getFirst().getDiagnosticContext().get("traceId")).isNotBlank().matches("[0-9a-f]{32}"), // in micrometer traceId: 32 Hex
                () -> assertThat(logEvents.getFirst().getDiagnosticContext().get("spanId")).isNotBlank().matches("[0-9a-f]{16}"), // in micrometer spanId: 16 Hex
                () -> assertEquals(Logbook.class.getName(), logEvents.getLast().getLoggerName()),
                () -> assertThat(logEvents.getLast().getDiagnosticContext().get("traceId")).isNotBlank().matches("[0-9a-f]{32}"),
                () -> assertThat(logEvents.getLast().getDiagnosticContext().get("spanId")).isNotBlank().matches("[0-9a-f]{16}"),
                () -> assertEquals(
                    logEvents.getFirst().getDiagnosticContext().get("traceId"),
                    logEvents.getLast().getDiagnosticContext().get("traceId"),
                    "traceId muss für Request/Response identisch sein"
                ),
                () -> assertEquals(
                    logEvents.getFirst().getDiagnosticContext().get("spanId"),
                    logEvents.getLast().getDiagnosticContext().get("spanId"),
                    "spanId muss für Request/Response identisch sein"
                )
            );
        }
    }

    @Test
    void actuator_info_logbook__logsMessage_viaLogbackAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(Logbook.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        String url = "http://localhost:" + port + "/actuator/info";
        restTemplate.getForEntity(url, String.class);
        List<ILoggingEvent> logEvents = listAppender.list;

        assertAll(
            () -> assertNotNull(logEvents),
            () -> assertEquals(2, logEvents.size()),
            () -> assertEquals(Logbook.class.getName(), logEvents.getFirst().getLoggerName()),
            () -> assertThat(logEvents.getFirst().getMDCPropertyMap().get("traceId")).isNotBlank().matches("[0-9a-f]{32}"), // in micrometer traceId: 32 Hex
            () -> assertThat(logEvents.getFirst().getMDCPropertyMap().get("spanId")).isNotBlank().matches("[0-9a-f]{16}"), // in micrometer spanId: 16 Hex
            () -> assertEquals(Logbook.class.getName(), logEvents.getLast().getLoggerName()),
            () -> assertThat(logEvents.getLast().getMDCPropertyMap().get("traceId")).isNotBlank().matches("[0-9a-f]{32}"),
            () -> assertThat(logEvents.getLast().getMDCPropertyMap().get("spanId")).isNotBlank().matches("[0-9a-f]{16}"),
            () -> assertEquals(
                logEvents.getFirst().getMDCPropertyMap().get("traceId"),
                logEvents.getLast().getMDCPropertyMap().get("traceId"),
                "traceId muss für Request/Response identisch sein"
            ),
            () -> assertEquals(
                logEvents.getFirst().getMDCPropertyMap().get("spanId"),
                logEvents.getLast().getMDCPropertyMap().get("spanId"),
                "spanId muss für Request/Response identisch sein"
            )
        );
        logger.detachAppender(listAppender);
        listAppender.stop();
    }
}
