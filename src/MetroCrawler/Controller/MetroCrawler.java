package MetroCrawler.Controller;



import org.codehaus.jackson.*;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.map.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MetroCrawler {

    private String link;
    private static int depth = 1;
    private static int timeout = 10;
    private static ArrayList<Integer> errorCode = new ArrayList<>();
    private static int crawlTime = 0;
    private static boolean followExternalLinks = false;
    private int currentDepth = 1;
    private static String baseLink;
    private static File jsonOutput = new File("json_output.txt");
    private static ObjectMapper mapper = new ObjectMapper();
    private ResultObject result = new ResultObject();



    private static long startCrawlTime = System.currentTimeMillis();

    protected static Set<String> visitedLinks = Collections.synchronizedSet(new HashSet<>());
    protected static Set<String> visitedSources = Collections.synchronizedSet(new HashSet<>());

    protected static ConcurrentLinkedQueue<String> currentLinks = new ConcurrentLinkedQueue<>();
    protected static ConcurrentLinkedQueue<String> futureLinks = new ConcurrentLinkedQueue<>();

    protected static ConcurrentLinkedQueue<String> currentSources = new ConcurrentLinkedQueue<>();
    protected static ConcurrentLinkedQueue<String> futureSources = new ConcurrentLinkedQueue<>();

    protected static List<String> resultsLink = Collections.synchronizedList(new ArrayList<>());
    protected static List<String> resultsSrc = Collections.synchronizedList(new ArrayList<>());

    //Constructors
    public MetroCrawler() {

    }

    public MetroCrawler(String link) {
        this.link = link;
        try {
            URL u = new URL(link);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

    }

    //Getters and setters
    public  synchronized String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public synchronized static int getDepth() {
        return depth;
    }

    public static void setDepth(int depth) {
        MetroCrawler.depth = depth;
    }

    public static synchronized int getTimeout() {
        return timeout;
    }

    public static void setTimeout(int timeout) {
        MetroCrawler.timeout = timeout;
    }

    public static synchronized ArrayList<Integer> getErrorCode() {
        return errorCode;
    }

    public static void setErrorCode(ArrayList<Integer> e) {
        MetroCrawler.errorCode = e;
    }

    public static synchronized int getCrawlTime() {
        return crawlTime;
    }

    public static void setCrawlTime(int crawlTime) {
        MetroCrawler.crawlTime = crawlTime;
    }

    public static synchronized boolean isFollowExternalLinks() {
        return followExternalLinks;
    }

    public static synchronized void setFollowExternalLinks(boolean followExternalLinks) {
        MetroCrawler.followExternalLinks = followExternalLinks;
    }

    public static synchronized String getBaseLink(){

        return baseLink;
    }

    public void crawl() throws IOException {

        //begin seed
        Document doc = Jsoup.connect(link).timeout(timeout * 1000).validateTLSCertificates(false).get();

        Elements elements = doc.select("a[href]");

        Elements domElements = doc.select("[src]");

        baseLink = doc.baseUri().substring((doc.baseUri().indexOf('/')+2));

        if(baseLink.startsWith("www")){
            baseLink = baseLink.substring(4);
        }

        result.setLink(link);

       // System.out.println("Base link is "+getBaseLink());

       // System.out.println("Frontier is " + currentDepth);
       // System.out.println();

        for (Element element : elements) {
            if (visitedLinks.contains(element.attr("abs:href"))) {
                continue;
            }
            visitedLinks.add(element.attr("abs:href"));
            currentLinks.offer(element.attr("abs:href"));
        }

        for (Element element : domElements) {
            if (visitedSources.contains(element.attr("abs:src"))) {
                continue;
            }
            visitedSources.add(element.attr("abs:src"));
            currentSources.offer(element.attr("abs:src"));

        }

//begin threads
        while (currentDepth <= depth) {

         //   System.out.println();
         //  System.out.println("Frontier is " + currentDepth);
         //  System.out.println();

            ExecutorService executor = Executors.newFixedThreadPool(5);

            for (int i = 1; i <= 5; i++) {
                executor.execute(new CrawlPageForLinks("Thread " + i));
            }
            executor.shutdown();

            try {
                if(crawlTime != 0) {
                    executor.awaitTermination(crawlTime, TimeUnit.MINUTES);
                }
                else{
                    executor.awaitTermination(30,TimeUnit.MINUTES);
                }
            } catch (InterruptedException e) {
          //      System.out.println("Exceeded crawl time.");
                executor.shutdownNow();
            }

            while(!executor.isTerminated()){
           //     System.out.println("Waiting for threads to finish.");
                try {
                    Thread.currentThread().sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

//set new frontier

            ArrayList toFile = new ArrayList<>();

            for (String s : resultsLink){
                toFile.add(s);
            }

            for (String s: resultsSrc){
                toFile.add(s);
            }

            result.setFrontier(toFile);


            resultsLink.clear();
            resultsSrc.clear();

          //  System.out.println();
          // System.out.println("Setting new frontier.");

            for(String item : futureLinks){
               // System.out.println("Moving futureLink " + item);
                currentLinks.offer(item);

            }

            futureLinks.clear();

            for (String item: futureSources){
               // System.out.println("Moving futureSources " + item);
                currentSources.offer(item);
            }

            futureSources.clear();

            currentDepth = currentDepth + 1;

        }

        result.setCrawlTime(System.currentTimeMillis()-startCrawlTime);

       // System.out.println();
       // System.out.println("Total crawl time is " + (System.currentTimeMillis()-startCrawlTime));

       // System.out.println();
       // System.out.println("Results for links are:");
        for(String s : resultsLink){
            System.out.println(s);
        }

       // System.out.println();
       // System.out.println("Results for sources are:");
        for(String s: resultsSrc){
        //    System.out.println(s);
        }
        mapper.setVisibility(JsonMethod.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.writeValue(jsonOutput,result);
    }
//class that will be marshaled into JSON format
    class ResultObject{

        String link;
        long crawlTime;
        ArrayList<ArrayList> frontier = new ArrayList<ArrayList>();

        public void setLink(String s){
            link = s;
        }

        public void setCrawlTime(long time){
            crawlTime = time;
        }

        public void setFrontier(ArrayList a){
            frontier.add(a);
        }


    }

}

