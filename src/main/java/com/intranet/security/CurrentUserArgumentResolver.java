package com.intranet.security;

import java.util.Collection;
import java.util.Collections;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import com.intranet.dto.UserDTO;

import org.springframework.web.context.request.NativeWebRequest;


@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterAnnotation(CurrentUser.class) != null &&
               parameter.getParameterType().equals(UserDTO.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {

        // Authentication auth = (Authentication) webRequest.getUserPrincipal();
        // if (auth == null || !(auth.getPrincipal() instanceof Jwt)) {
        //     return null;
        // }

        // Jwt jwt = (Jwt) auth.getPrincipal();
        // return new UserDTO(
        //     Long.valueOf(jwt.getClaimAsString("user_id")), // Assuming user_id is an Integer
        //     jwt.getClaim("email"),
        //     jwt.getClaimAsString("name"),
        //     jwt.getClaimAsStringList("roles")
        // );

        // create a sample DTO for now
        
        UserDTO dto=new UserDTO();
        dto.setId(5L);
        dto.setName("Ajay Kumar");
        dto.setEmail("test@gmail.com");
        dto.setRoles(Collections.emptyList());
        return dto;
    }
}
