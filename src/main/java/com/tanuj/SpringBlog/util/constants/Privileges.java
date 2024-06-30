package com.tanuj.SpringBlog.util.constants;

public enum Privileges {

    RESET_ANY_USER_PASSWORD(1l, "RESET_ANY_USER_PASSWORD"), ACCESS_ADMIN_PANEL(2l, "ACCESS_ADMIN_PANEL");

    private Long id;
    private String privileges;
    private Privileges(Long id, String privileges){
        this.id = id;
        this.privileges = privileges;
    }

    public Long getId(){
        return id;
    }

    public String getPrivileges(){
        return privileges;
    }
    
}
