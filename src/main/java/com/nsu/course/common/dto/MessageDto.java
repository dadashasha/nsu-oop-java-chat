package com.nsu.course.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.Date;

@AllArgsConstructor
@Jacksonized
@Builder
@Getter
public class MessageDto {
    String userName;
    String message;
    Date time;
}
