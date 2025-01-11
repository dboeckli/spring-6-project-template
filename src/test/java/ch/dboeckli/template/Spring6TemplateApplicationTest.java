package ch.dboeckli.template;
// TODO: RENAME PACKAGE

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS)
@Slf4j
// TODO: RENAME ME
class Spring6TemplateApplicationTest {

    @Test
    void contextLoads() {
        log.info("Testing Spring 6 Template Application...");
    }

}
