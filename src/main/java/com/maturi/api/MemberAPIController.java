package com.maturi.api;

import com.maturi.dto.member.MemberLoginDTO;
import com.maturi.service.member.EmailService;
import com.maturi.service.member.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/member")
public class MemberAPIController {

  final private MemberService memberService;
  final private EmailService emailService;

  @PostMapping("/emailAuth")
  public ResponseEntity<String> emailAuth(@RequestBody String json,
                          HttpServletRequest request) throws Exception {
    // parse
    JSONParser jsonParser = new JSONParser();
    JSONObject jsonObject = (JSONObject) jsonParser.parse(json);
    String email = (String) jsonObject.get("email");

    /* 이메일 중복 검사 */
    boolean isJoinMember = memberService.emailDuplCheck(email);
    if(isJoinMember){ // 중복된 이메일 존재
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    /* 이메일 인증 메일 보내기 */
    String confirm = emailService.sendSimpleMessage(email);

    HttpSession session = request.getSession();
    session.setAttribute("emailConfirm", confirm);
    log.info("emailConfirm Number = {}", confirm);

    return ResponseEntity.status(HttpStatus.OK).build();
  }


  @PostMapping("/emailConfirm")
  public ResponseEntity<String> emailConfirm(@RequestBody String json,
                             HttpServletRequest request) throws ParseException {
    // parse
    JSONParser jsonParser = new JSONParser();
    JSONObject jsonObject = (JSONObject) jsonParser.parse(json);
    String emailConfirm = (String) jsonObject.get("emailConfirm");

    HttpSession session = request.getSession();
    String sessionEmailConfirm = (String) session.getAttribute("emailConfirm");

    ResponseEntity response = null;
    if(emailConfirm.equals(sessionEmailConfirm)){
      response = ResponseEntity.status(HttpStatus.OK).build();
      session.removeAttribute("emailConfirm");
    } else {
      response = ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    return response;

  }
}