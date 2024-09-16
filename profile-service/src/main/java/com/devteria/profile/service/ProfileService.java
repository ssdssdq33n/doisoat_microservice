package com.devteria.profile.service;

import java.util.*;

import com.devteria.event.dto.NotificationEvent;
import com.devteria.profile.dto.identity.*;
import com.devteria.profile.dto.request.PasswordRequest;
import com.devteria.profile.dto.request.RoleAddRequest;
import com.devteria.profile.dto.request.RoleRequest;
import com.devteria.profile.dto.response.RoleIdReponse;
import com.devteria.profile.dto.response.RoleReponse;
import com.devteria.profile.entity.Profile;
import com.devteria.profile.entity.Role;
import com.devteria.profile.exception.AppException;
import com.devteria.profile.exception.ErrorCode;
import com.devteria.profile.mapper.RoleMapper;
import com.devteria.profile.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.devteria.profile.dto.request.RegistrationRequest;
import com.devteria.profile.dto.response.ProfileResponse;
import com.devteria.profile.exception.ErrorNormalizer;
import com.devteria.profile.mapper.ProfileMapper;
import com.devteria.profile.repository.IdentityClient;
import com.devteria.profile.repository.ProfileRepository;

import feign.FeignException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProfileService {
    ProfileRepository profileRepository;
    ProfileMapper profileMapper;
    RoleRepository roleRepository;
    RoleMapper roleMapper;
    IdentityClient identityClient;
    ErrorNormalizer errorNormalizer;
    KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${idp.client-id}")
    @NonFinal
    String clientId;

    @Value("${idp.client-secret}")
    @NonFinal
    String clientSecret;

    @PreAuthorize("hasRole('ADMIN')") // nếu sử dụng hasAuthority thì phải đúng tên role thì mới map , ví dụ: ROLE_ADMIN
    public List<ProfileResponse> getAllProfiles() {
        var profiles = profileRepository.findAll();
        return profiles.stream().map(profileMapper::toProfileResponse).toList();
    }

    @PreAuthorize("hasRole('ADMIN')") // nếu sử dụng hasAuthority thì phải đúng tên role thì mới map , ví dụ: ROLE_ADMIN
    public List<RoleReponse> getAllRoles() {
        var roles = roleRepository.findAll();
        return roles.stream().map(roleMapper::toRoleReponse).toList();
    }

    public ProfileResponse getMyProfile() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        var profile = profileRepository.findByUserId(userId).orElseThrow(
                () -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return profileMapper.toProfileResponse(profile);
    }

    public ProfileResponse register(RegistrationRequest request) {
        try {
            // Create account in KeyCloak
            // Exchange client Token
            var token = identityClient.exchangeToken(TokenExchangeParam.builder()
                    .grant_type("client_credentials")
                    .client_id(clientId)
                    .client_secret(clientSecret)
                    .scope("openid")
                    .build());

            log.info("TokenInfo {}", token);
            // Create user with client Token and given info

            // Get userId of keyCloak account
            var creationResponse = identityClient.createUser(
                    "Bearer " + token.getAccessToken(),
                    UserCreationParam.builder()
                            .username(request.getUsername())
                            .firstName(request.getFirstName())
                            .lastName(request.getLastName())
                            .email(request.getEmail())
                            .enabled(true)
                            .emailVerified(false)
                            .credentials(List.of(Credential.builder()
                                    .type("password")
                                    .temporary(false)
                                    .value(request.getPassword())
                                    .build()))
                            .build());

            String userId = extractUserId(creationResponse);
            log.info("UserId {}", userId);

            NotificationEvent notificationEvent = NotificationEvent.builder()
                    .headerTab("Tài khoản đăng nhập")
                    .name(request.getFirstName()+" "+request.getLastName())
                    .password(request.getPassword())
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .build();

            // Publish message to kafka
            kafkaTemplate.send("notification-delivery", notificationEvent);

            var profile = profileMapper.toProfile(request);
            profile.setUserId(userId);

            profile = profileRepository.save(profile);

            return profileMapper.toProfileResponse(profile);
        } catch (FeignException exception) {
            throw errorNormalizer.handleKeyCloakException(exception);
        }
    }

    public TokenExchangeUserResponse loginUser(String username,String password){
        try {
            TokenExchangeUserResponse response = identityClient.loginUser(TokenExchangeUser.builder()
                    .grant_type("password")
                    .client_id(clientId)
                    .client_secret(clientSecret)
                    .scope("openid")
                    .username(username)
                    .password(password)
                    .build());
            log.info("response user :",response);
            return response;
        } catch (FeignException exception) {
            throw errorNormalizer.handleKeyCloakException(exception);
        }
    }

    public ValidResponse introspect(IntrospectRequest request){
        try {
            IntrospectResponse response = identityClient.introspect(IntrospectParam.builder()

                    .client_id(clientId)
                    .client_secret(clientSecret)
                    .token(request.getToken())
                    .build());
            log.info("response :",response);
            ValidResponse validResponse=new ValidResponse(response.isActive());
            return validResponse;
        } catch (FeignException exception) {
            throw errorNormalizer.handleKeyCloakException(exception);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    public RoleReponse createRole(RoleRequest request) {
        try {
            // Create account in KeyCloak
            // Exchange client Token
            TokenExchangeUserResponse response = identityClient.loginUser(TokenExchangeUser.builder()
                    .grant_type("password")
                    .client_id(clientId)
                    .client_secret(clientSecret)
                    .scope("openid")
                    .username("admin")
                    .password("admin")
                    .build());

            var creationRole = identityClient.createRole(
                    "Bearer " + response.getAccessToken(),
                    RoleRequest.builder()
                            .name(request.getName())
                            .description(request.getName())
                            .build());


            var role = roleMapper.toRole(request);


            role = roleRepository.save(role);

            return roleMapper.toRoleReponse(role);
        } catch (FeignException exception) {
            throw errorNormalizer.handleKeyCloakException(exception);
        }
    }


    @PreAuthorize("hasRole('ADMIN')")
    public RoleReponse addRoletoRole(RoleAddRequest request) {
        try {
            // Create account in KeyCloak
            // Exchange client Token
            TokenExchangeUserResponse response = identityClient.loginUser(TokenExchangeUser.builder()
                    .grant_type("password")
                    .client_id(clientId)
                    .client_secret(clientSecret)
                    .scope("openid")
                    .username("admin")
                    .password("admin")
                    .build());

            var roleReponseParent = identityClient.getIdRole("Bearer " + response.getAccessToken(), request.getRoleParent());
            List<RoleIdReponse> listChild=new ArrayList<>();
            Set<String> listName=new HashSet<>();
            for(String id : request.getRoleChilds()){
                var roleReponseChild = identityClient.getIdRole("Bearer " + response.getAccessToken(), id);
                listChild.add(roleReponseChild);
                listName.add(roleReponseChild.getName());
            }
            var addRole=identityClient.addRoletoRole("Bearer " + response.getAccessToken(),roleReponseParent.getId(),listChild);


            Optional<Role> role = roleRepository.findByNameEquals(request.getRoleParent());
            if(role.isEmpty()) throw new AppException(ErrorCode.USER_NOT_EXISTED);

            role.get().setPermissions(listName);


            Role res = roleRepository.save(role.get());

            return roleMapper.toRoleReponse(res);
        } catch (FeignException exception) {
            throw errorNormalizer.handleKeyCloakException(exception);
        }
    }

    @PreAuthorize("hasRole('USER')")
    public String resetPassword(String username, String password) {
        try {
            // Create account in KeyCloak
            // Exchange client Token
            TokenExchangeUserResponse response = identityClient.loginUser(TokenExchangeUser.builder()
                    .grant_type("password")
                    .client_id(clientId)
                    .client_secret(clientSecret)
                    .scope("openid")
                    .username("admin")
                    .password("admin")
                    .build());

            Optional<Profile> profile=profileRepository.findByUsernameEquals(username);
            if(profile.isEmpty()) throw new AppException(ErrorCode.USER_NOT_EXISTED);
            PasswordRequest request=new PasswordRequest("password",false,password);
            var res = identityClient.resetPassword("Bearer " + response.getAccessToken(),profile.get().getUserId(),request);

            return "Change Password Success";
        } catch (FeignException exception) {
            throw errorNormalizer.handleKeyCloakException(exception);
        }
    }

    @PreAuthorize("hasRole('USER')")
    public ProfileResponse addRoleToUser(String userId, List<String> listRole) {
        try {
            // Create account in KeyCloak
            // Exchange client Token
            TokenExchangeUserResponse response = identityClient.loginUser(TokenExchangeUser.builder()
                    .grant_type("password")
                    .client_id(clientId)
                    .client_secret(clientSecret)
                    .scope("openid")
                    .username("admin")
                    .password("admin")
                    .build());

            Optional<Profile> profile=profileRepository.findByUserId(userId);
            if(profile.isEmpty()) throw new AppException(ErrorCode.USER_NOT_EXISTED);
            List<RoleIdReponse> roleIdReponses=new ArrayList<>();
            Set<String> roles=new HashSet<>();
            for(String id : listRole){
                var roleReponseChild = identityClient.getIdRole("Bearer " + response.getAccessToken(), id);
                roles.add(roleReponseChild.getName());
                roleIdReponses.add(roleReponseChild);
            }
            var res = identityClient.addRoleToUser("Bearer " + response.getAccessToken(),userId,roleIdReponses);

            profile.get().setRoles(roles);
            profileRepository.save(profile.get());

            return profileMapper.toProfileResponse(profile.get());
        } catch (FeignException exception) {
            throw errorNormalizer.handleKeyCloakException(exception);
        }
    }

    private String extractUserId(ResponseEntity<?> response) {
        String location = response.getHeaders().get("Location").getFirst();
        String[] splitedStr = location.split("/");
        return splitedStr[splitedStr.length - 1];
    }
}
