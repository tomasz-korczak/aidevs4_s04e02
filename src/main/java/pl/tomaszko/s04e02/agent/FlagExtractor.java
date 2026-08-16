package pl.tomaszko.s04e02.agent;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FlagExtractor {

    private static final Pattern FLAG = Pattern.compile("\\{FLG:[^}]+\\}");

    public String extract(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Matcher matcher = FLAG.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }
}
