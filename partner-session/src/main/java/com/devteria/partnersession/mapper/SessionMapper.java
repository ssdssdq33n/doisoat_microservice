package com.devteria.partnersession.mapper;

import org.mapstruct.Mapper;

import com.devteria.partnersession.dto.request.SessionRequest;
import com.devteria.partnersession.dto.response.SessionResponse;
import com.devteria.partnersession.model.Session;

@Mapper(componentModel = "spring")
public interface SessionMapper {
    SessionResponse toSessionResponse(Session session);

    Session toSession(SessionRequest request);
}
