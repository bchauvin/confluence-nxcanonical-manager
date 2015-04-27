package org.nuxeo.confluence.nxcanonical.macro;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.pages.AbstractPage;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NxCanonicalMacro implements Macro {

    /** Just use the body as description and send in the author */
    @Override
    public String execute(Map<String, String> parameters, String body,
            ConversionContext context) throws MacroExecutionException {
        Map<String, String> properties = Maps.newHashMap();

        if (body != null && body.length() > 5) {
            properties.put("description",
                    noCR(VelocityContext.convertToText(body)));
            putImage(properties, body);
        }

        ContentEntityObject ceo = context.getEntity();
        if (ceo instanceof AbstractPage) {
            String author = ceo.getLastModifierName();
            properties.put("author", noCR(author));
        }

        putIfNotBlank(properties, parameters, "canonicalUrl");

        Map<String, Object> contextMap = MacroUtils.defaultVelocityContext();
        contextMap.put("properties", properties);

        return VelocityContext.BEGIN_TAG
                + VelocityUtils.getRenderedTemplate("nxcanonicalmacro.vm",
                        contextMap) + VelocityContext.END_TAG;
    }

    Pattern IMG_TAG = Pattern.compile("<img [^>]*src=\"([^\"]+)\"[^>]*>");

    private void putImage(Map<String, String> properties, String body) {
        Matcher matchedElement = IMG_TAG.matcher(body);
        if (matchedElement.find()) {
            String src = matchedElement.group(1);
            if (StringUtils.isNotBlank(src)) {
                properties.put("imagesrc", src);
            }
        }
    }

    private void putIfNotBlank(Map<String, String> properties,
            Map<String, String> parameters, String key) {
        String value = parameters.get(key);
        if (StringUtils.isNotBlank(value)) {
            properties.put(key, value);
        }
    }

    Pattern ALL_CR = Pattern.compile("[\\n\\r]");

    private String noCR(String text) {
        if (text == null) {
            return "";
        }
        return ALL_CR.matcher(text).replaceAll(" ");
    }

    public static class Tag {
        String tag;

        String attr1Value;

        String attr2Value;

        public static Tag Meta_itemprop(String itemprop, String value) {
            return new Tag("meta_itemprop", itemprop, value);
        }

        public static Tag Meta_name(String name, String value) {
            return new Tag("meta_name", name, value);
        }

        public static Tag Meta_property(String prop, String value) {
            return new Tag("meta_property", prop, value);
        }

        public static Tag Link_rel(String rel, String href) {
            return new Tag("link_rel", rel, href);
        }

        public Tag(String tag, String attr1Value, String attr2Value) {
            this.tag = tag;
            this.attr1Value = attr1Value;
            this.attr2Value = attr2Value;
        }

        public String getTag() {
            return tag;
        }

        public String getAttr1Value() {
            return attr1Value;
        }

        public String getAttr2Value() {
            return attr2Value;
        }
    }

    @Override
    public BodyType getBodyType() {
        return BodyType.RICH_TEXT;
    }

    @Override
    public OutputType getOutputType() {
        return OutputType.BLOCK;
    }

}
