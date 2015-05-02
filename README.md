#Web Crawler in Java

**Usage**
--------------
- Expects a seed url to start the crawling


**Features**
--------------
- Can be configured to only crawl specific domains
- Respects robots.txt for a given domain if available
- Crawls only html documents
- Canonicalize the crawled urls
- Following configurations can be specified in the properties file
  - Seed url
  - Specific domains allowed to be crawled 
  - Output file, containing the crawled url's
  - MAX_URLS_TO_VIST can be specified in properties file (default : 100)
  - MAX_URLS_TO_EXTRACT can be specified in properties file (default : 120)
  
**Dependencies**
--------------
- jsoup-1.7.3
- crawler4j-3.3
