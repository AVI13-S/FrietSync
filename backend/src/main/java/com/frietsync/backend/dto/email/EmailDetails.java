package com.frietsync.backend.dto.email;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailDetails {

    private String recipient;
    private String subject;
    private String msgBody;
    private String actionUrl;
    private String actionLabel;
}
