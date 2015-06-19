package com.rosterloh.moodring.events;

/**
 * @author Richard Osterloh <richard.osterloh.com>
 * @version 1
 * @since 15/12/2014
 */
public class MessageEvent {
    public final String message;

    public MessageEvent(String message) {
        this.message = message;
    }
}
