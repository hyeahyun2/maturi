package com.maturi.dto.member;

import com.maturi.entity.member.MemberStatus;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
@Data
public class MemberJoinDTO {

    //유효성 검사하는 필드
    @Email
    private String email;
    @NotBlank
    @Pattern(regexp="(?=.*[0-9])(?=.*[a-zA-Z])(?=.*\\W)(?=\\S+$).{8,20}")
    private String passwd;
    @NotBlank
    private String passwdCheck;
    @NotBlank
    private String name;

    //유효성 검사하지 않는 필드
    private String salt;
    private String nickName;
    private MemberStatus status;

}
