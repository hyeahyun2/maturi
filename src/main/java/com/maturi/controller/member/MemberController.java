package com.maturi.controller.member;

import com.maturi.dto.member.MemberEditMyPageDTO;
import com.maturi.dto.member.MemberJoinDTO;
import com.maturi.dto.member.MemberLoginDTO;
import com.maturi.entity.member.Member;
import com.maturi.service.article.ArticleService;
import com.maturi.service.member.EmailService;
import com.maturi.service.member.MemberService;
import com.maturi.util.argumentresolver.Login;
import com.maturi.util.constfield.MessageConst;
import com.maturi.util.validator.MemberValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

import static com.maturi.util.constfield.SessionConst.*;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("/members")
public class MemberController {
  final private MemberService memberService;
  final private ArticleService articleService;
  final private MemberValidator memberValidator;
  final private EmailService emailService;

  @InitBinder("memberJoinDTO")
  public void init(WebDataBinder dataBinder) {
    dataBinder.addValidators(memberValidator);
  }
  @GetMapping("/join")
  public String getJoin(Model model){
    model.addAttribute("member",new MemberJoinDTO());
    return "/members/join";
  }

  @PostMapping("/join")
  public String join(
          @Validated @ModelAttribute(name = "member") MemberJoinDTO memberJoinDTO,
          BindingResult bindingResult){
    log.info("memberJoinRequest = {}", memberJoinDTO.toString());
    memberValidator.validate(memberJoinDTO, bindingResult);

    //검증에 실패하면 다시 입력 폼으로
    if (bindingResult.hasErrors()) {
      log.info("errors={} ", bindingResult);
      return "/members/join";
    }

    memberService.join(memberJoinDTO);
    return "redirect:/members/login";
  }

  @GetMapping("/login")
  public String loginPage(@RequestParam(defaultValue = "/") String redirectURL,
                          Model model){

    model.addAttribute("redirectURL",redirectURL);
    model.addAttribute("member",new MemberLoginDTO());
    return "/members/login";
  }

  @PostMapping("/login")
  public String login(
          @ModelAttribute(name = "member") MemberLoginDTO memberLoginDTO,
          BindingResult bindingResult,
          @RequestParam(defaultValue = "/") String redirectURL,
          HttpServletRequest request,
          RedirectAttributes redirectAttributes,
          Model model){

    //검증에 실패하면 다시 입력 폼으로
    if (bindingResult.hasErrors()) {
      log.info("errors={} ", bindingResult);
      model.addAttribute(MessageConst.ERROR_MESSAGE, MessageConst.LOGIN_FAIL);
      return "/members/login";
    }

    //정상 로직
    Member findMember = memberService.login(memberLoginDTO);
    if(findMember == null){
      model.addAttribute(MessageConst.ERROR_MESSAGE, MessageConst.LOGIN_FAIL);

      return "/members/login";
    }
    else if(memberService.isBanMember(findMember.getId())){ // 밴 멤버
      model.addAttribute(MessageConst.ERROR_MESSAGE, MessageConst.IS_BAN_MEMBER);

      return "/members/login";
    } else { // 정상 멤버
      HttpSession session = request.getSession();
      session.setAttribute(MEMBER_ID,findMember.getId());

      log.info("redirectURL = {}",redirectURL);
      return "redirect:" + redirectURL;
    }
  }

  @PostMapping("/logout")
  public String logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Member LoginMember = memberService.getMemberById((Long) request.getSession().getAttribute(MEMBER_ID));
    request.getSession().invalidate();

    return "redirect:/members/login";
  }

  @PostMapping("/unregister")
  public String unregister(@Login Long memberId,
                           @RequestParam String passwd,
                           RedirectAttributes redirectAttributes,
                           HttpServletRequest request) {
    boolean status = memberService.unregister(memberId, passwd);

    if (!status) { // 회원 탈퇴 실패
      return "redirect:/myPage/detail";
    } else { // 회원 탈퇴 성공
      // 세션 삭제
      request.getSession().invalidate();
      redirectAttributes.addFlashAttribute(MessageConst.SUCCESS_MESSAGE, "unregister");

      return "redirect:/members/login";
    }
  }


  @GetMapping("/{id}")//회원 마이페이지 이동
  public String myPage(@PathVariable Long id, // 해당 마이페이지 유저
                       @Login Long memberId,
                       Model model){

    model.addAttribute("member", articleService.memberInfo(memberId));
    model.addAttribute("myPageMember", memberService.myPageMemberInfo(id));
    return "/members/myPage";
  }

  @GetMapping("/{id}/edit")// myPage/edit 회원 프로필수정 페이지 이동
  public String editMyPage(@Login Long memberId,
                           @PathVariable Long id,
                           RedirectAttributes redirectAttributes,
                           Model model){
    if(!memberId.equals(id)){ // 다른 회원의 프로필수정 페이지 이동 요청 들어왔을 경우
      redirectAttributes.addFlashAttribute(MessageConst.ERROR_MESSAGE, MessageConst.NO_PERMISSION);
      return "redirect:/members/" + id;
    }

    model.addAttribute("member", articleService.memberInfo(memberId));
    model.addAttribute("myPageMember", memberService.myPageMemberInfo(id));
    return "/members/editMyPage";
  }


  @PatchMapping("/{id}/edit")
  public String editMemberProfileInfo(@Login Long memberId,
                                      @PathVariable Long id,
                                      MemberEditMyPageDTO memberEditMyPageDTO,
                                      RedirectAttributes redirectAttributes,
                                      Model model) throws IOException {

    if(memberId.equals(id)){ // 로그인 회원의 프로필 수정 요청일 경우만 시행
      memberService.editMemberProfileInfo(memberId, memberEditMyPageDTO);
    }
    else { // 타회원의 요청
      redirectAttributes.addFlashAttribute(MessageConst.ERROR_MESSAGE, MessageConst.NO_PERMISSION);
    }

    return "redirect:/members/" + id;
  }

  @GetMapping("/{id}/detail")//회원의 상세페이지 이동
  public String myPageDetail(@Login Long memberId,
                             @PathVariable Long id,
                             RedirectAttributes redirectAttributes,
                             Model model){
    if(!memberId.equals(id)){ // 다른 회원의 상세페이지 이동 요청 들어왔을 경우
      redirectAttributes.addFlashAttribute(MessageConst.ERROR_MESSAGE, MessageConst.NO_PERMISSION);
      return "redirect:/members/" + id;
    }

    model.addAttribute("member", articleService.memberInfo(memberId));
    model.addAttribute("myPageMember", memberService.myPageMemberInfo(memberId));
    model.addAttribute("memberDetailInfo", memberService.memberDetailInfo(memberId));

    return "/members/myPageDetail";
  }

  @PostMapping("/newPasswd")
  public String newPasswd(@Login Long memberId,
                          @RequestParam String passwd,
                          RedirectAttributes redirectAttributes){
    Member member = memberService.changePasswd(memberId, passwd);

    redirectAttributes.addFlashAttribute(MessageConst.SUCCESS_MESSAGE, MessageConst.PASSWD_CHANGE);

    return "redirect:/members/" + memberId;
  }

  @GetMapping("/help/passwd") // 비밀번호 찾기
  public String pwInquiry(){
    return "/members/help/pwInquiry";
  }

  @PostMapping("/help/passwd") // 비밀번호 찾기 -> 변경
  public String pwChange(@ModelAttribute(name = "member") MemberLoginDTO memberLoginDTO,
                         RedirectAttributes redirectAttributes,
                         Model model){

    log.info("pwChange start");
    // email에 맞는 회원 찾기
    Member findMember = memberService.getMemberByEmail(memberLoginDTO.getEmail());
    if(findMember == null){
      return "redirect:/members/help/passwd";
    }

    // 해당 회원의 비밀번호 변경
    memberService.changePasswd(findMember.getId(), memberLoginDTO.getPasswd());

    // 메세지 보내기
    redirectAttributes.addFlashAttribute(MessageConst.SUCCESS_MESSAGE, MessageConst.PASSWD_CHANGE);

    return "redirect:/members/login";
  }


  @GetMapping("/help/email")
  public String emailInquiry(){
    return "/members/help/emailInquiry";
  }

  @PostMapping("/help/email")
  public String foundEmailPage(@RequestParam String email,
                               Model model){
    model.addAttribute("email", email);
    return "/members/help/foundEmail";
  }
}
