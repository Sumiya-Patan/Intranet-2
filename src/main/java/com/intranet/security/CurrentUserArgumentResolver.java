package com.intranet.security;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
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

        Authentication auth = (Authentication) webRequest.getUserPrincipal();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt)) {
            return null;
        }

         // Normalize roles: remove spaces, replace with underscores, uppercase
        

        Jwt jwt = (Jwt) auth.getPrincipal();
        List<String> normalizedRoles = jwt.getClaimAsStringList("roles").stream()
            .map(role -> role.trim().replace(" ", "_").toUpperCase())
            .collect(Collectors.toList());
        return new UserDTO(
            Long.valueOf(jwt.getClaimAsString("user_id")), // Assuming user_id is an Integer
            jwt.getClaimAsString("name"),
            jwt.getClaim("email"),            
            normalizedRoles
        );
    }
}
