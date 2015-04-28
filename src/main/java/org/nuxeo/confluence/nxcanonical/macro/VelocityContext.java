package org.nuxeo.confluence.nxcanonical.macro;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.setup.sitemesh.SitemeshPageBodyRenderable;
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.util.profiling.VelocitySitemeshPage;
import com.google.common.collect.Maps;

public class VelocityContext {
    private final PageManager pageManager;

    private String metadataKey = "nxCanonicalUrl";

    final static String BEGIN_TAG = "<!-- NxCanonicalManager:";

    final static String END_TAG = ":NxCanonicalManager -->";

    final static Pattern EACH_PROP = Pattern.compile("(.*?)=(.*)");

    public VelocityContext(PageManager pageManager) {
        this.pageManager = pageManager;
    }

    public boolean canPrintNxCanonical(VelocitySitemeshPage page) {
        if (page == null) {
            return false;
        }

        if (page.getProperty("page.canonical") != null
            &&
            (page.getProperty("page.canonical").contains("/display/")
            || page.getProperty("page.canonical").contains("/pages/"))
            ) {
            return true;
        }

        return false;
    }

    public String printNxCanonical(VelocitySitemeshPage page, Object body) {
        if (page == null || body == null) {
            return null;
        }

        @SuppressWarnings("unchecked")
        Map<String, String> meshProperties = page.getProperties();
        if (meshProperties == null) {
            return null;
        }

        Map<String, String> metadata = getNxCanonical(meshProperties, body);
        if (metadata == null) {
            return null;
        }

        // Don't do anything if in latest space
        if (metadata.get("isLatestSpace") != null
                && metadata.get("isLatestSpace").equals("1")) {
            return null;
        }

        // Use same page in latest space
        if (metadata.get(metadataKey) != null) {
            return metadata.get(metadataKey);
        }

        // Use macro value
        if (metadata.get("canonicalUrl") != null) {
            return metadata.get("canonicalUrl");
        }

        // We don't have anything: use default canonical
        return null;
    }

    private Map<String, String> getNxCanonical(
            Map<String, String> meshProperties, Object body) {
        Map<String, String> metadata = Maps.newHashMap();

        String spaceKey = meshProperties.get("page.spacekey");
        boolean isLatestSpace = !spaceKey.matches("^.*\\d+$");

        metadata.put("isLatestSpace", "0");
        if (isLatestSpace) {
            metadata.put("isLatestSpace", "1");
        }

        String baseUrl = "http://doc.nuxeo.com/display/";
        String shortSpaceName = spaceKey.replaceFirst("\\d+", "");
        String pageTitle = meshProperties.get("title");

        // Try to get the page in the current space
        if (pageManager.getPage(shortSpaceName, pageTitle) != null) {
            // Spaces in title have to be replaced with + signs only after page
            // has been checked
            // Otherwise it will not be found
            metadata.put(metadataKey, baseUrl + shortSpaceName + "/"
                    + pageTitle.replaceAll(" ", "+"));
        }

        // Try to get page from the macro's value
        if (body instanceof SitemeshPageBodyRenderable) {

            try {
                long start = System.nanoTime();
                StringWriter w = new StringWriter();
                ((SitemeshPageBodyRenderable) body).render(null, w);
                String bodyHtml = w.toString();

                String[] parts = bodyHtml.split(BEGIN_TAG);
                for (int i = 1; i < parts.length; i++) {
                    if (!parts[i].contains(END_TAG)) {
                        continue;
                    }
                    String part = parts[i].substring(0,
                            parts[i].indexOf(END_TAG));

                    Matcher propMatcher = EACH_PROP.matcher(part);
                    while (propMatcher.find()) {
                        String value = GeneralUtil.unescapeEntities(propMatcher.group(2));
                        String prop = propMatcher.group(1);
                        metadata.put(prop, value);
                    }
                }
                long end = System.nanoTime();

                metadata.put(
                        "nxcanonical-rendertime",
                        TimeUnit.MICROSECONDS.convert(end - start,
                                TimeUnit.NANOSECONDS) + "us");
            } catch (RuntimeException re) {
                // Do nothing, say nothing
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Get default canonical
        metadata.put("defaultCanonicalUrl",
                meshProperties.get("page.canonical"));

        return metadata;
    }

    private static Pattern ALL_HTML_TAGS = Pattern.compile("<.*?>");

    public static String convertToText(String body) {
        String bodyWithoutTags = ALL_HTML_TAGS.matcher(body).replaceAll("");
        String bodyWithoutSpaces = bodyWithoutTags.replaceAll("\\s+", " ").trim();
        String bodyWithoutHtmlEntities = GeneralUtil.unescapeEntities(bodyWithoutSpaces);
        return bodyWithoutHtmlEntities;
    }
}
