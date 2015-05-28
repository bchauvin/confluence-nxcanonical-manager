# Nuxeo Canonical URL Manager For Confluence

## What It Is
This is a fork from the SEO Manager plugin for Confluence: https://marketplace.atlassian.com/plugins/com.playsql.seo-manager

It has been simplified to handle only one thing: Nuxeo documentation's canonical urls management.

## How To Use It
In the doctheme's jar, in the doctheme/decorators/main.vmd file, add the following before the /head tag:

```
<!-- nxcanonical manager -->
    #if($sitemeshPage && $body && $nxcanonical.canPrintNxCanonical($sitemeshPage))
      #if($nxcanonical.printNxCanonical($sitemeshPage, $body))
        <link rel="canonical" href="$nxcanonical.printNxCanonical($sitemeshPage, $body)" />
        <link rel="shortlink" href="No tiny URL for this page">
        #else
        $!sitemeshPage.getProperty("page.canonical")
      #end
    #end
```
