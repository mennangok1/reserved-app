package com.mennangok1.reserved;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ReservedApplicationTests {

	@Test
	void contextLoads() {
	}

}
