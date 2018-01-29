package org.wikipedia.ro.parser;

import static org.apache.commons.lang3.StringUtils.trim;

import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.wikipedia.ro.model.WikiPart;
import org.wikipedia.ro.model.WikiTemplate;

public class WikiTemplateParser extends WikiPartParser {

    private static final Pattern START_PATTERN = Pattern.compile("\\{\\{");

    @Override
    public boolean startsWithMe(String wikiText) {
        if (null == wikiText) {
            return false;
        }
        Matcher startMatcher = START_PATTERN.matcher(wikiText);
        return startMatcher.find() && 0 == startMatcher.start();
    }

    @Override
    public void parse(String wikiText, TriFunction<WikiPart, String, String, Void> resumeCallback) {
        WikiTemplate template = new WikiTemplate();
        if (null == wikiText || 0 == wikiText.length()) {
            return;
        }
        
        final char[] chars = wikiText.toCharArray();
        int index = 0;
        StringBuilder templateTitleBuilder = new StringBuilder();
        StringBuilder templateTextBuilder = new StringBuilder("{{");

        if (index >= chars.length - 1) {
            return;
        }
        index += 2;
        while (index < chars.length && chars[index] != '|' && chars[index] != '}') { // skip initial template name
            templateTitleBuilder.append(chars[index]);
            templateTextBuilder.append(chars[index]);
            index++;
        }
        if (chars[index] == '|') {
            templateTextBuilder.append('|');
            index++;
        }
        template.setTemplateTitle(trim(templateTitleBuilder.toString()));

        final Stack<String> automatonStack = new Stack<String>();

        String crtParamName = null;
        int crtParamIndex = 1;
        final StringBuilder crtBuilder = new StringBuilder();
        loop: while (index < chars.length - 1) {
            final char crtChar = chars[index];
            final char nextChar = chars[index + 1];
            templateTextBuilder.append(crtChar);

            switch (crtChar) {
            case '=':
                if (automatonStack.isEmpty() && crtParamName == null) {
                    crtParamName = crtBuilder.toString();
                    crtBuilder.delete(0, crtBuilder.length());
                } else {
                    crtBuilder.append(crtChar);
                }
                index++;
                break;
            case '}':
            case ']':
                if (nextChar == crtChar) {
                    templateTextBuilder.append(nextChar);
                    if (!automatonStack.isEmpty()) {
                        crtBuilder.append(crtChar);
                        crtBuilder.append(crtChar);
                        automatonStack.pop();
                        index += 2;
                    } else {
                        if (null == crtParamName) {
                            crtParamName = String.valueOf(crtParamIndex++);
                        }
                        template.setParam(trim(crtParamName), trim(crtBuilder.toString()));
                        crtBuilder.delete(0, crtBuilder.length());
                        crtParamName = null;
                        template.setLength(2 + index);
                        break loop;
                    }
                    break;
                } else {
                    crtBuilder.append(crtChar);
                    index++;
                }

                break;
            case '|':
                if (automatonStack.isEmpty()) {
                    if (null == crtParamName) {
                        crtParamName = String.valueOf(crtParamIndex++);
                    }
                    template.setParam(trim(crtParamName), trim(crtBuilder.toString()));
                    crtParamName = null;
                    crtBuilder.delete(0, crtBuilder.length());
                } else {
                    crtBuilder.append(crtChar);
                }
                index++;
                break;
            case '{':
            case '[':
                if (nextChar == crtChar) {
                    automatonStack.push("" + crtChar + nextChar);
                    crtBuilder.append(crtChar);
                    crtBuilder.append(nextChar);
                    templateTextBuilder.append(nextChar);
                    index += 2;
                } else {
                    crtBuilder.append(crtChar);
                    index++;
                }
                break;
            default:
                crtBuilder.append(crtChar);
                index++;
            }
        }
        String initialTemplateText = templateTextBuilder.toString();
        if (initialTemplateText.matches("\\{\\{\\s*" + template.getTemplateTitle() + "\\s*\\}\\}")) {
            template.removeParam("1");
        }
        template.setInitialText(initialTemplateText);
        resumeCallback.apply(template, initialTemplateText, wikiText.substring(initialTemplateText.length()));
    }

}