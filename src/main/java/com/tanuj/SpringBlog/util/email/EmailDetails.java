package com.tanuj.SpringBlog.util.email;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailDetails {
    
    private String recepient;
    private String msgBody;
    private String subject;

}
