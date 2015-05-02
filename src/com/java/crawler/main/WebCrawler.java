package com.java.crawler.main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedWriter;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.java.crawler.util.Utilities;

import edu.uci.ics.crawler4j.url.URLCanonicalizer;

/**
 * @author Swapnil Gupta
 * @purpose Crawler to extract links from webpages, starting from initial seed
 */
public class WebCrawler {
	
	private static File outputFile;
	private static BufferedWriter urlWriter;
	
	private static int extractedUrlsCount;
	private static final String URL_SEPARATOR = "\t";
	private static HashMap<String, String> properties = new HashMap<String, String>();
	                                                                                                           
	private static HashSet<String> allowedDomains;
	private static HashSet<String> uniqueHosts = new HashSet<String>();
	private static HashSet<String> visitedUrls = new HashSet<String>();
	private static HashSet<String> restrictedUrlsInALlowedDomains = new HashSet<String>();
	
	/**
	 * URL Queue implementation : 
	 * 2 separate hash set one for each BFS level one current and another future.
	 * When current frontier becomes empty future_frontier becomes current and iteration continues
	 * When both current and future frontier become empty execution stops
	 * 
	 **/
	private static HashSet<String> currentFrontier = new HashSet<String>();
	private static HashSet<String> futureFrontier = new HashSet<String>();
	
	public WebCrawler () {
		extractedUrlsCount = 0;
		properties = Utilities.loadProperties();
		outputFile = new File (properties.get("OUTPUT_FILE"));
		allowedDomains = new HashSet<String>(Arrays.asList (properties.get("ALLOWED_DOMAINS").split(",")));
		for (String l : allowedDomains){
			System.out.println(l);
		}
	}
	
	public static void main(String[] args) {
		new WebCrawler();                      
		Utilities.initializeFile(outputFile);
		writeCrawledWebPages(getvalidCanonicalURL(properties.get("SEED_URL")));
	}
	
	/**
	 * Write crawled web pages from the given initial seed url.
	 * @param seedUrl initial url to start web crawling
	 * 
	 */
	public static void writeCrawledWebPages(String seedUrl) {
		if (seedUrl == null) {
			System.exit(0);
		}
		try {
			urlWriter = new BufferedWriter(new FileWriter(outputFile.getAbsoluteFile(), true));
			crawlWebPages(seedUrl);
			urlWriter.close();
		} catch(IOException e) {
			e.printStackTrace();
			System.err.println("IO exception occurend while writing: " + outputFile.getAbsolutePath());
		}
	}	
	
	/**
	 * Crawl web pages from the given initial seed url.
	 * @param seedUrl initial url to start web crawling
	 * 
	 */
	public static void crawlWebPages(String seedUrl) throws IOException {
		currentFrontier.add(seedUrl);
		Iterator<String> iterator = currentFrontier.iterator();
		
		while (isBelowMaxUrlsCount(visitedUrls.size(), "MAX_URLS_TO_VIST") && (iterator.hasNext() || !futureFrontier.isEmpty())) {
			if (!iterator.hasNext()) {										
				iterator = updateEmptyFrontier();
			}
			processIndividualUrl(iterator.next());
			iterator.remove();
		}
	}
	
	/**
	 * Process an individual url
	 * @param pageUrl url for current page
	 * @param extractedUrlsCount current count of extracted urls
	 * 
	 */
	public static void processIndividualUrl(String pageUrl) throws IOException {
		visitedUrls.add(pageUrl);
		urlWriter.write(pageUrl);
		urlWriter.newLine();
		extractUrls(pageUrl);
	}
	
	/**
	 * Extract all the urls on current page.
	 * @param pageUrl url for current page
	 * 
	 */
	public static void extractUrls(String pageUrl) throws IOException {
		int CONN_TIMEOUT = Integer.parseInt(properties.get("HTTP_CONNECTION_TIMEOUT"));
		Document pageDocument = Jsoup.connect(pageUrl).userAgent("Mozilla").timeout(CONN_TIMEOUT).get();
		Elements links = pageDocument.select("a[href]");
		
		Iterator<Element> linkIterator = links.iterator();
		while(linkIterator.hasNext() && isBelowMaxUrlsCount(extractedUrlsCount, "MAX_URLS_TO_EXTRACT")) {
			extractedUrlsCount += addUniqueUrlToFrontier(linkIterator.next().attr("abs:href"));
		}
	}
	
	/**
	 * Add a unique url to frontier
	 * @param pageUrl url for current page
	 * @return 1 if url was unique else return 0
	 * 
	 */
	public static int addUniqueUrlToFrontier(String pageUrl) throws IOException {
		String canonicalUrl = getvalidCanonicalURL(pageUrl);
		if (canonicalUrl != null 
				&& !visitedUrls.contains(canonicalUrl) 
				&& !currentFrontier.contains(canonicalUrl) 
				&& !futureFrontier.contains(canonicalUrl)) {
			
			futureFrontier.add(canonicalUrl);
			urlWriter.write(URL_SEPARATOR + canonicalUrl);
			urlWriter.newLine();
			return 1;
		}
		return 0;
	}
		
	/**
	 * Get valid canonical url for given page url
	 * @param pageUrl url for current page
	 * @return canonical url if url was valid else return null
	 * 
	 */
	public static String getvalidCanonicalURL(String pageUrl) {
		try {
			URL url = new URL(pageUrl);
			
			if (Utilities.hasSubDomainInSet(url.getHost(), allowedDomains)) {
				checkAndAddUniqueHost(url);
				return getCannonicalUrl(pageUrl);
			}
		} catch(MalformedURLException ex) {
			System.err.println("Malformed url found " + pageUrl);
			return null;
		}
		return null;
	}
	
	/**
	 * Get canonical url for a url in allowed domain
	 * @param pageUrl url to get its canonical form
	 * @return canonical url if url is a non restricted url from allowed domain else return null
	 */
	public static String getCannonicalUrl(String pageUrl) {
		if (!Utilities.hasSubDomainInSet(pageUrl, restrictedUrlsInALlowedDomains) && Utilities.isHtmlDoc(pageUrl)) {
			return URLCanonicalizer.getCanonicalURL(pageUrl);
		}
		return null;
	}
	
	/**
	 * Update current frontier to future frontier if empty
	 * @return iterator for the updated current frontier
	 * 
	 */
	public static Iterator<String> updateEmptyFrontier() {
		currentFrontier = futureFrontier;
		futureFrontier = new HashSet<String>();
		return currentFrontier.iterator();
	}
	
	/**
	 * Add given host to uniqueHost Set if it does not already exist
	 * @param url : URL, whoose host needs to be added to unique hosts
	 */
	public static void checkAndAddUniqueHost(URL url) {
		String host = url.getHost();
		if (!uniqueHosts.contains(host)) {
			uniqueHosts.add(host);
			restrictedUrlsInALlowedDomains.addAll(Utilities.parseRobotsTxtForHost(url.getProtocol(), host));
		}
	}
	
	/**
	 *Check whether the current count is less than the max count specified in the properties
	 * @param count current count
	 * @param property Property name containing the maximum count
	 * @return true if count is less than equal to max count else return false
	 */
	public static boolean isBelowMaxUrlsCount(int count, String property) {
		return count <= Integer.parseInt(properties.get(property));
	}

}
