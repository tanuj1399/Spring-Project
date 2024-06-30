package com.tanuj.SpringBlog.Controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import jakarta.validation.Valid;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.tanuj.SpringBlog.models.Account;
import com.tanuj.SpringBlog.services.AccountService;
import com.tanuj.SpringBlog.services.EmailService;
import com.tanuj.SpringBlog.util.AppUtil;
import com.tanuj.SpringBlog.util.email.EmailDetails;

@Controller
public class AccountController {
    
    @Autowired
    private AccountService accountService;

    @Autowired
    private EmailService emailService;

    @Value("${site.domain}")
    private String site_domain;

    @Value("${password.token.reset.timeout.minutes}")
    private int password_token_timeout;

    @GetMapping("/register")
    public String register(Model model){

        Account account = new Account();
        model.addAttribute("account", account);
        return "account_views/register";

    }

    @PostMapping("/register")
    public String register_user(@Valid @ModelAttribute Account account, BindingResult result){
        if(result.hasErrors()){
            return "account_views/register";
        }
        
        accountService.save(account);
        return "redirect:/";
    }

    @GetMapping("/login")
    public String login(Model model){
        return "account_views/login";
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated")
    public String profile(Model model, Principal principal){
        String authUser = "email";
        if(principal!=null){
            authUser = principal.getName();
        }
        Optional<Account> optionalAccount = accountService.findOneByEmail(authUser);
        if(optionalAccount.isPresent()){
            Account account = optionalAccount.get();
            model.addAttribute("account", account);
            model.addAttribute("photo", account.getPhoto());
            return "account_views/profile";
        }
        else{
            return "redirect:/?error";
        }
        
    }

    @PostMapping("/profile")
    @PreAuthorize("isAuthenticated")
    public String post_profile(@Valid @ModelAttribute Account account, BindingResult bindingResult, Principal principal){
        if(bindingResult.hasErrors()){
            return "account_views/profile";
        }
        String authUser = "email";
        if(principal!=null){
            authUser = principal.getName();
        }
        Optional<Account> optionalAccount = accountService.findOneByEmail(authUser);
        if(optionalAccount.isPresent()){
            Account account_by_id = accountService.findById(account.getId()).get();
            account_by_id.setAge(account.getAge());
            account_by_id.setDate_of_birth(account.getDate_of_birth());
            account_by_id.setFirstname(account.getFirstname());
            account_by_id.setLastname(account.getLastname());
            account_by_id.setGender(account.getGender());
            account_by_id.setPassword(account.getPassword());

            accountService.save(account_by_id);
            SecurityContextHolder.clearContext();
            return "redirect:/";
        }
        else{
            return "redirect:/?error";
        }
    }

    @PostMapping("/update_photo")
    @PreAuthorize("isAuthenticated")
    public String update_photo(@RequestParam("file") MultipartFile file,
    RedirectAttributes attributes, Principal principal){
        if(file.isEmpty()){
            attributes.addFlashAttribute("error","No file uploaded");
            return "redirect:/profile";
        }
        else{

            String filename = StringUtils.cleanPath(file.getOriginalFilename());

            try {
                int length = 10;
                boolean useLetters = true;
                boolean useNumbers = true;
                String generatedString = RandomStringUtils.random(length,useLetters,useNumbers);
                String final_photo_name = generatedString + filename;
                String absolute_fileLocation = AppUtil.get_upload_path(final_photo_name);

                Path path = Paths.get(absolute_fileLocation);
                Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
                attributes.addFlashAttribute("message", "Photo uploaded successfully");

                String authUser = "email";
                if(principal!=null){
                     authUser = principal.getName();
                }
                Optional<Account> optionalAccount = accountService.findOneByEmail(authUser);
                if(optionalAccount.isPresent()){
                    Account account = optionalAccount.get();
                    Account account_by_id = accountService.findById(account.getId()).get();
                    String relative_fileLocation = "uploads/" + final_photo_name ;
                    account_by_id.setPhoto(relative_fileLocation);
                    accountService.save(account_by_id);
                }

                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (Exception e) {
                    Thread.currentThread().interrupt();;
                }

                return "redirect:/profile";

            } catch (Exception e) {
                // TODO: handle exception
            }

        }
        return "redirect:/profile?error";
    }

    @GetMapping("/forgot-password")
    public String forgot_password(Model model){
        return "account_views/forgot_password";
    }

    @PostMapping("/reset-password")
    public String reset_password(@RequestParam("email") String _email, RedirectAttributes attributes, Model model){
        Optional<Account> optionalAccount = accountService.findOneByEmail(_email);
        if(optionalAccount.isPresent()){
            Account account = accountService.findById(optionalAccount.get().getId()).get();
            String reset_token = UUID.randomUUID().toString();
            account.setToken(reset_token);
            account.setPassword_reset_token_expiry(LocalDateTime.now().plusMinutes(password_token_timeout));
            accountService.save(account);
            String reset_message = "This is the reset password link: " +site_domain+"change-password?token="+reset_token;
            EmailDetails emailDetails = new EmailDetails(account.getEmail(), reset_message, "Reset password email");
            if(emailService.sendSimpleEmail(emailDetails) == false){
                attributes.addFlashAttribute("error", "Error while sending email");
                return "redirect:/forgot-password";
            }
            attributes.addFlashAttribute("message", "Password reset email sent");
            return "redirect:/login";

        }
        else{
            attributes.addFlashAttribute("error", "No user found with the email");
            return "redirect:/forgot-password";

        }
    }

    @GetMapping("/change-password")
    public String change_password(Model model, @RequestParam("token") String token, RedirectAttributes attributes){
        if(token.equals("")){
            attributes.addFlashAttribute("error", "Invalid Token");
            return "redirect:/forgot-password";
        }
        Optional<Account> optionalAccount = accountService.findByToken(token);
        if(optionalAccount.isPresent()){
            Account account = accountService.findById(optionalAccount.get().getId()).get();
            LocalDateTime now = LocalDateTime.now();
            if(now.isAfter(optionalAccount.get().getPassword_reset_token_expiry())){
                attributes.addFlashAttribute("error", "Token expired");
                return "redirect:/forgot-password";
            }
            model.addAttribute("account", account);
            return "account_views/change_password";
        }

        attributes.addFlashAttribute("error", "Invalid token");
        return "redirect:/forgot-password";
    }

    @PostMapping("/change-password")
    public String post_change_password(@ModelAttribute Account account, RedirectAttributes attributes){
        Account account_by_id = accountService.findById(account.getId()).get();
        account_by_id.setPassword(account.getPassword());
        account_by_id.setToken("");
        accountService.save(account_by_id);
        attributes.addFlashAttribute("message", "Password Updated");
        return "redirect:/login";
    }

}
