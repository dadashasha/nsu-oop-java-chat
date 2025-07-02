package com.nsu.course.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@AllArgsConstructor
@Jacksonized
@Builder
@Getter
public class ChatDto {
    private List<MessageDto> messages;
}
