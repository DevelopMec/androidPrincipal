package com.biometricscan.principal.models;

public class LoginResponse {
    private String id;
    private String issued_at;
    private String instance_url;
    private String signature;
    private String access_token;
    private String token_type;
    private String scope;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIssued_at() {
        return issued_at;
    }

    public void setIssued_at(String issued_at) {
        this.issued_at = issued_at;
    }

    public String getInstance_url() {
        return instance_url;
    }

    public void setInstance_url(String instance_url) {
        this.instance_url = instance_url;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getAccess_token() {
        return access_token;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public String getToken_type() {
        return token_type;
    }

    public void setToken_type(String token_type) {
        this.token_type = token_type;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
}
