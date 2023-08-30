package com.inn.cafe.com.inn.cafe.serviceImpl;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.neo4j.Neo4jProperties.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import com.inn.cafe.com.inn.cafe.JWT.CustomerUsersDetailsService;
import com.inn.cafe.com.inn.cafe.JWT.JwtUtil;
import com.inn.cafe.com.inn.cafe.POJO.User;
import com.inn.cafe.com.inn.cafe.dao.UserDao;
import com.inn.cafe.com.inn.cafe.service.UserService;
import com.inn.cafe.com.inn.cafe.utils.CafeUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service

public class UserServiceImpl implements UserService {
    @Autowired
    UserDao userDao;

    @Autowired
    JwtUtil jwtUtil;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    CustomerUsersDetailsService customerUsersDetailsService;

    @Override
    public ResponseEntity<String> signUp(Map<String, String> requestMap) {
        try {
        log.info("inside signup {}", requestMap);
        if (validateSignUpMap(requestMap)) {
            User user = userDao.findByEmailId(requestMap.get("email"));
            if (Objects.isNull(user)) {
                user = getUserFromMap(requestMap);
                userDao.save(user);
                return CafeUtils.getResponseEntity("Successfuly registerd", HttpStatus.OK);
            }
            else{
                return CafeUtils.getResponseEntity("User already exists", HttpStatus.BAD_REQUEST);
            }
        } else {
            return CafeUtils.getResponseEntity("Invalid data", HttpStatus.BAD_REQUEST);
        }    
        } catch (Exception e) {
           e.printStackTrace();
        }
        return CafeUtils.getResponseEntity("Something went wrong", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private boolean validateSignUpMap(Map<String, String> requestMap) {
        if (requestMap.containsKey("name") && requestMap.containsKey("email") && requestMap.containsKey("password")
                && requestMap.containsKey("contactNumber")) {
            return true;
        }
        return false;
    }

    private User getUserFromMap(Map<String, String> requesMap) {
        User user = new User();
        user.setEmail(requesMap.get("email"));
        user.setContactNumber(requesMap.get("contactNumber"));
        user.setPassword(requesMap.get("password"));
        user.setName(requesMap.get("name"));
        user.setStatus("false");
        user.setRole("user");
        return user;

    }

    @Override
    public ResponseEntity<List<User>> getUsers() {
        List<User> users= userDao.findAll();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<User> getUser(int id) {
        Optional<User> userOptional = userDao.findById(id);
    
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            return new ResponseEntity<>(user, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public ResponseEntity<String> deleteUser(int id) {
        Optional<User> userOptional = userDao.findById(id);
        if(userOptional.isPresent()){
            userDao.deleteById(id);
            return CafeUtils.getResponseEntity("Successfully deleted user with id: "+id, HttpStatus.OK);
        } else {
            return CafeUtils.getResponseEntity("User with this id does not exist", HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public ResponseEntity<String> putUser(int id, Map<String, String> requestMap) {
        try {
        Optional<User> userOptional = userDao.findById(id);
        if(userOptional.isPresent()){
            User user= userOptional.get();
            if(requestMap.containsKey("email")){
                user.setEmail(requestMap.get("email"));
            }
            if(requestMap.containsKey("name")){
                user.setName(requestMap.get("name"));
            }
            if(requestMap.containsKey("password")){
                user.setPassword(requestMap.get("password"));
            }
            if(requestMap.containsKey("contactNumber")){
                user.setContactNumber(requestMap.get("contactNumber"));
            }
            userDao.save(user);
            return CafeUtils.getResponseEntity("Successfully updated user with id: "+id, HttpStatus.OK);
        } else {
            return CafeUtils.getResponseEntity("User with this id does not exist", HttpStatus.NOT_FOUND);
        }
            
        } catch (Exception e) {
            e.printStackTrace();
            
        }
        return CafeUtils.getResponseEntity("Something is wrong", HttpStatus.INTERNAL_SERVER_ERROR );
    }

    @Override
    public ResponseEntity<String> logIn(Map<String, String> requestMap) {
        try {
            org.springframework.security.core.Authentication authentication= authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(requestMap.get("email"), requestMap.get("password"))
            );

            if(authentication.isAuthenticated()){
                if(customerUsersDetailsService.getUserDetail().getStatus().equalsIgnoreCase("true")){
                    String token= jwtUtil.generateToken(customerUsersDetailsService.getUserDetail().getEmail(),
                    customerUsersDetailsService.getUserDetail().getRole());
                    return CafeUtils.getResponseEntity("{\"token\":\""+token+"\"}", HttpStatus.OK);
                }
                else{
                    return CafeUtils.getResponseEntity("Waiting for admin approval", HttpStatus.FORBIDDEN);
                }

            }
        } catch (Exception e) {
            log.error("{}", e);
        }
        return CafeUtils.getResponseEntity("smth went wrong", HttpStatus.INTERNAL_SERVER_ERROR );
    }
    

}
