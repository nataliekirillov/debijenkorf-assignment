package com.debijenkorf.assignment.enums;

public enum S3ErrorEnum {
    NO_SUCH_KEY("NoSuchKey"),
    ;

    public final String value;

    S3ErrorEnum(String value) {
        this.value = value;
    }
}
