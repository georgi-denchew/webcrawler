/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.webcrawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Georgi
 */
public class Main {

    public static void main(String[] args) {

        String startLocation = "http://fmi.wikidot.com";

        String needle = "Докажете, че:";

        try {
            URI uri = new URI(startLocation);
            WebCrawler runnable = new WebCrawler(uri, needle);

            for (int i = 0; i < 40; i++) {
                Thread thread = new Thread(runnable);
                thread.start();
            }

            while (!runnable.isFound()) {
                Thread.sleep(100);
            }

            System.out.println();
            boolean found = runnable.isFound();

            String resultString = null;
            if (found) {
                List<String> pathLinks = runnable.pathLinks();
                resultString = String.format("//=> %s //=> %s", pathLinks.get(pathLinks.size() - 1), pathLinks.toString());
            } else {
                resultString = String.format("The needle \"%s\" is not found in URI %s and its' children", needle, startLocation);
            }
            System.out.println(resultString);

        } catch (URISyntaxException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
