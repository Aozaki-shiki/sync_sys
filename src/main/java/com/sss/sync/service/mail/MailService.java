package com.sss.sync.service.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

  private final JavaMailSender mailSender;
  private final MailProperties props;

  public void sendText(String to, String subject, String text) {
    if (!props.isEnabled()) return;

    SimpleMailMessage msg = new SimpleMailMessage();
    msg.setFrom(props.getFrom());
    msg.setTo(to);
    msg.setSubject(subject);
    msg.setText(text);
    mailSender.send(msg);
  }
}