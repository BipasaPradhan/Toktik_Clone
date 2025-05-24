package com.example.securingweb.whoami;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class WhoamiDTO {
    private boolean loggedIn = false;

    private String username;

    private String name;

    private String role;
}
