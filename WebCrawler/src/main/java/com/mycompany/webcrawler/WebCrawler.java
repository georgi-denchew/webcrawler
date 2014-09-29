/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.webcrawler;

import com.sun.java.swing.plaf.windows.WindowsTreeUI;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Georgi
 */
public class WebCrawler implements Runnable {

//    private static final String URL_MATCHER = "<a href.*?>";
    private static final String URL_MATCHER = "<a href=\"(.*?)\"";

    private Set<URI> checkedUrls;
    private BlockingQueue<URI> urisToCheck;
    private ConcurrentMap<String, String> paths;

    private URI startLocation;
    private String needle;

    private boolean found = false;
    private URI foundURI = null;

    public WebCrawler(URI startLocation, String needle) {
        this.startLocation = startLocation;
        this.needle = needle;
        checkedUrls = new CopyOnWriteArraySet<URI>();
        urisToCheck = new LinkedBlockingQueue<URI>();
        paths = new ConcurrentHashMap<>();
    }

    public WebCrawler() {
    }

    public List<String> pathLinks() {
        List<String> pathLinks = new ArrayList<>();


        if (this.foundURI != null) {
            String child = this.foundURI.toString();
            pathLinks.add(child);

            while (this.paths.containsKey(child)) {
                String parent = this.paths.get(child);
                pathLinks.add(parent);
                child = parent;
            }
        }

        Collections.reverse(pathLinks);
        
        return pathLinks;
    }

    public String fullPath() {
        String fullPath;

        StringBuilder pathBuilder = new StringBuilder();

        if (this.foundURI != null) {
            String child = this.foundURI.toString();
            pathBuilder.append(child);

            while (this.paths.containsKey(child)) {
                String parent = this.paths.get(child);
                pathBuilder.append(" => ");
                pathBuilder.append(parent);
                child = parent;
            }
        }

        fullPath = pathBuilder.toString();
        return fullPath;
    }

    @Override
    public void run() {
        this.crawl(startLocation, needle);
    }

    public void crawl(URI rootLocation, String needle) {
        if (found) {
            return;
        }

        InputStream inputStream = null;
        try {
            String startLocationString = rootLocation.toString();
            inputStream = new URL(startLocationString).openStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line;
            StringBuilder builder = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                builder.append(line);
                builder.append("\n");
            }
            String pageString = builder.toString();
            //System.out.println(pageString);

            if (pageString.contains(needle)) {
                synchronized (this) {

                    this.found = true;
                    this.foundURI = rootLocation;
                }
            } else {
                addChecked(rootLocation);

                Pattern urlPattern = Pattern.compile(URL_MATCHER);
                Matcher matcher = urlPattern.matcher(pageString);

                while (matcher.find()) {
                    String matchString = matcher.group();
                    String urlString = matchString.replace("<a href=", "");
                    urlString = urlString.replace(">", "");
                    urlString = urlString.replace("\"", "");
                    urlString = urlString.replace("\'", "");

                    URL url = rootLocation.toURL();

                    if (urlString.equals("javascript:;") || urlString.equals("/")) {
                        continue;
                    }

                    URL resultURL = new URL(url, urlString);
                    URI newURI = resultURL.toURI();
                    newURI = newURI.normalize();

                    String newAuthority = newURI.getAuthority();

                    if (newAuthority == null || !newAuthority.equals(rootLocation.getAuthority())) {
                        continue;
                    }
                    boolean toRecreate = false;

                    if (!startLocationString.endsWith("/") && !urlString.startsWith("/")) {
                        toRecreate = true;
                        urlString = "/" + urlString;
                    }

                    String newURIString = newURI.toString();

                    if (newURIString.contains("../")) {
                        newURIString = newURIString.replace("../", "");
                        toRecreate = true;
                    }

                    if (toRecreate) {
                        newURI = new URI(newURIString);
                    }

                    addUriIfNotExisting(newURI, rootLocation);
                }
            }

        } catch (MalformedURLException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            String message = String.format("URI not found: %s", rootLocation.toString());
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, message);
        } catch (URISyntaxException ex) {
            Logger.getLogger(WebCrawler.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }

                URI nextToCheck = poll();

                if (nextToCheck != null && !found) {

                    this.crawl(nextToCheck, needle);
                }
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private  URI poll() {
        return urisToCheck.poll();
    }

    private  void addUriIfNotExisting(URI newURI, URI parent) {
        if (!checkedUrls.contains(newURI)
                && !urisToCheck.contains(newURI)) {
            paths.put(newURI.toString(), parent.toString());
            urisToCheck.add(newURI);
        }
    }

    private  void addChecked(URI rootLocation) {
        checkedUrls.add(rootLocation);
    }

    public URI getStartLocation() {
        return startLocation;
    }

    public void setStartLocation(URI startLocation) {
        this.startLocation = startLocation;
    }

    public String getNeedle() {
        return needle;
    }

    public void setNeedle(String needle) {
        this.needle = needle;
    }

    public URI getFoundURI() {
        return foundURI;
    }

    public void setFoundURI(URI foundURI) {
        this.foundURI = foundURI;
    }

    public boolean isFound() {
        return found;
    }

    public void setFound(boolean found) {
        this.found = found;
    }
}
