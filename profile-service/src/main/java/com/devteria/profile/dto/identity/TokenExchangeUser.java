package com.devteria.profile.dto.identity;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TokenExchangeUser {
    String grant_type;
    String client_id;
    String client_secret;
    String scope;
    String username;
    String password;
}
