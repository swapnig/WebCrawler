package com.java.crawler.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Scanner;

public class Utilities {
	
	private static final String HTML_CONTENT_TYPE = "text/html";
	private static final String URL_SEPARATOR = " ";
	HashSet<String> allowedDomains = new HashSet<String>();
	
	/**
	 * Parse the robots.txt for the given domain if it exists, add all the urls from robots.txt to restrictedUrls list
	 * @param protocol to connect to given host
	 * @param host for parsing robots.txt
	 * 
	 */
	public static HashSet<String> parseRobotsTxtForHost(String protocol, String host) {
		HashSet<String> restrictedUrls = new HashSet<String>();
		try {
			URL robotURL = new URL(protocol, host, "robots.txt");
			HttpURLConnection connection = (HttpURLConnection) robotURL.openConnection();
			connection.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 6.1; WOW64) "
					+ "Chrome/23.0.1271.95 Safari/537.11");
			
			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				Scanner robotsScanner = new Scanner(connection.getInputStream());
				
				while (robotsScanner.hasNextLine()) {	
					String[] tokens = robotsScanner.nextLine().split(URL_SEPARATOR);
					if (tokens[0].contains("Disallow:")){
						String restrictedUrl = host + tokens[1].toLowerCase();
						if(!restrictedUrls.contains(restrictedUrl)) {
							restrictedUrls.add(restrictedUrl);
						}
					}
				}
				robotsScanner.close();
			}
		} catch (IOException e) { 
			System.err.println("IO exception for host: " + host);
		}
		return restrictedUrls;
	}
	
	/**
	 * Request http header to ensure that the url is of a valid html page
	 * @param pageUrl url which is checked for being a valid url
	 * @return true if pageUrl corresponds to a html document else returns false
	 * 
	 */
	public static boolean isHtmlDoc(String pageUrl) {
		try {
		      HttpURLConnection.setFollowRedirects(false);
		      HttpURLConnection con = (HttpURLConnection) new URL(pageUrl).openConnection();
		      
		      con.setRequestProperty("User-Agent", 
		    		  "Mozilla/5.0 (Windows NT 6.1; WOW64) Chrome/23.0.1271.95 Safari/537.11");
		      con.setRequestMethod("HEAD");
		      
		      String contentType = con.getContentType();
		      return ((con.getResponseCode() == HttpURLConnection.HTTP_OK) && contentType.contains(HTML_CONTENT_TYPE));
		} catch (Exception e) { 
			System.err.println("Invalid url: " + pageUrl);
			return false;
		}
	}
	
	/**
	 * Verify whether url is from one of the given domain list
	 * @param url url to be checked against list of given domains
	 * @param domainSet a list of domains
	 * @return true if url is one of the given domains else return false
	 * 
	 */
	public static boolean hasSubDomainInSet(String url, HashSet<String> domainSet) {
		for(String domain : domainSet) {												
			if(url.contains(domain)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Initialize the text file for recording links
	 * @param file file to be initialized
	 * 
	 */
	public static void initializeFile(File file) {
		try {
			if (!file.exists()) {
				file.createNewFile();
			}
			new FileOutputStream(file, false).close();
		} catch (IOException e) { 
			System.err.println("I/O exception occured in opening: " + file.getAbsolutePath());
		}
	}
	
	/**
	 * Load all the properties from the properties file
	 * @return A map containing all the configuration properties
	 */
	public static HashMap<String, String> loadProperties() {
		Properties properties = new Properties();
		HashMap<String, String> propertiesMap = new HashMap<String, String>();
		try {
			properties.load(new FileInputStream("resources/crawler.properties"));
		} catch (IOException e) {
			System.err.println("I/O exception occured in opening: properties file");
		}
		propertiesMap.put("OUTPUT_FILE", properties.getProperty("OUTPUT_FILE"));
		propertiesMap.put("MAX_URLS_TO_VIST", properties.getProperty("MAX_URLS_TO_VIST"));
		propertiesMap.put("MAX_URLS_TO_EXTRACT", properties.getProperty("MAX_URLS_TO_EXTRACT"));
		propertiesMap.put("SEED_URL", properties.getProperty("SEED_URL"));
		propertiesMap.put("HTTP_CONNECTION_TIMEOUT", properties.getProperty("HTTP_CONNECTION_TIMEOUT"));
		propertiesMap.put("ALLOWED_DOMAINS", properties.getProperty("ALLOWED_DOMAINS"));
		
		return propertiesMap;
	}
}
