package com.fantastic.springai.dto;

import java.util.List;

public record ConversationRequest(List<MessageDto> messages, String question) {
}
