package io.muzoo.scalable.vms.CommentUtils;

import org.springframework.stereotype.Service;

@Service
public class HtmlSanitizer {
    public String sanitize(String input) {
        return org.owasp.encoder.Encode.forHtml(input);
    }
}