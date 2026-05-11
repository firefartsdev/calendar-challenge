package com.doodle.calendar_challenge;

import org.springframework.boot.SpringApplication;

public class TestCalendarChallengeApplication {

	public static void main(String[] args) {
		SpringApplication.from(CalendarChallengeApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
