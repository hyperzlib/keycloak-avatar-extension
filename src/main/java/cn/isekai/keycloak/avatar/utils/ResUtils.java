package cn.isekai.keycloak.avatar.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

public class ResUtils {
    public static Response responseJson(Map<String, Object> entity) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String jsonStr = mapper.writeValueAsString(entity);
            return Response.ok(jsonStr).type(MediaType.APPLICATION_JSON).build();
        } catch (JsonProcessingException e) {
            return Response.ok("{status:0,error:\"Internal Server Error\"}").type(MediaType.APPLICATION_JSON).build();
        }
    }

    public static Response responseBadRequest(boolean isJson) {
        if (isJson) {
            Map<String, Object> res = new HashMap<String, Object>();

            res.put("status", 500);
            res.put("error", "bad request");
            res.put("errormsg", "httpBadRequest");

            return responseJson(res);
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity("400 - Bad request").build();
        }
    }

    public static Response responseInternalServerError(boolean isJson) {
        if (isJson) {
            Map<String, Object> res = new HashMap<String, Object>();

            res.put("status", 500);
            res.put("error", "internal server error");
            res.put("errormsg", "httpInternalServerError");

            return responseJson(res);
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("500 - Internal Server Error").build();
        }
    }

    public static Response responseNotAuthorized(boolean isJson) {
        if (isJson) {
            Map<String, Object> res = new HashMap<String, Object>();

            res.put("status", 403);
            res.put("error", "access denied");
            res.put("errormsg", "accessDenied");

            return responseJson(res);
        } else {
            return Response.status(Response.Status.FORBIDDEN).entity("403 - Access denied").build();
        }
    }

    public static Response responseNotFound(boolean isJson) {
        if (isJson) {
            Map<String, Object> res = new HashMap<String, Object>();

            res.put("status", 404);
            res.put("error", "resource not found");
            res.put("errormsg", "avatarNotFound");

            return responseJson(res);
        } else {
            return Response.status(Response.Status.NOT_FOUND).entity("404 - Resource not found").build();
        }
    }

    public static Response responseRedirectTo(String url) {
        return Response.status(Response.Status.TEMPORARY_REDIRECT).header("Location", url).build();
    }

    public static String bin2hex(byte[] input){
        BigInteger bigInt = new BigInteger(1, input);
        StringBuilder hashText = new StringBuilder(bigInt.toString(16));
        while(hashText.length() < 32){
            hashText.insert(0, "0");
        }
        return hashText.toString();
    }

    public static String md5(String input){
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(input.getBytes(StandardCharsets.UTF_8));
            return bin2hex(md.digest());
        } catch(Exception e){
            return null;
        }
    }
}
