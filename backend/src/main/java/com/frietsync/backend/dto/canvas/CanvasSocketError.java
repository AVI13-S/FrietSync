package com.frietsync.backend.dto.canvas;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CanvasSocketError {

    private int status;
    private String message;
    private String destination;
}