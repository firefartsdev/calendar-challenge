package com.doodle.calendar_challenge.application.exception;

public class ParticipantNotAvailableException extends RuntimeException {

    public ParticipantNotAvailableException(String participant) {
        super("Participant '" + participant + "' has no free time slot covering the requested period");
    }
}
