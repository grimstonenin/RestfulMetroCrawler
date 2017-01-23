package MetroCrawler.Controller;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;


public class CrawlPageForLinks extends MetroCrawler implements Runnable{

    private String link;
    private String threadName;

    //Constructor
    public CrawlPageForLinks(String name){
        threadName = name;
    }

    //check if the status code is in the array for error codes
    protected boolean checkStatusCode(int statusCode){
        if ((getErrorCode().size() == 1) && (getErrorCode().get(0) == 0)){
            return true;
        }
        if(statusCode==200){
            return true;
        }
        return getErrorCode().contains(statusCode);

    }

    //crawlPage function to list pages and src
    protected void crawlPage(Connection.Response response) throws IOException{

        Document doc = response.parse();

        Elements parsedLinks = doc.select("a[href]");

        Elements domElements = doc.select("[src]");

        for(Element element : parsedLinks){
            if(!visitedLinks.contains(element.attr("abs:href"))) {
                if(typeOfLink(element.attr("abs:href"))){
                    futureLinks.offer(element.attr("abs:href"));
                    visitedLinks.add(element.attr("abs:href"));
                }
            }
        }

        for(Element element : domElements){
            if(!visitedSources.contains(element.attr("abs:src"))){
                    futureSources.offer(element.attr("abs:src"));
                    visitedSources.add(element.attr("abs:src"));
            }
        }
    }


    //function to check if we have an external link or not

    protected boolean typeOfLink(String link) {
        if (isFollowExternalLinks()) {
            return true;
        }

        if(link.contains(getBaseLink())){
           // System.out.println("This is thread " + threadName + " Link " + link + " is internal.");
            return true;
        }

        //System.out.println("This is thread " + threadName + " Link " + link + " is external.");
        return false;

    }



    public void run(){

        while(!MetroCrawler.currentLinks.isEmpty()){
            link = currentLinks.poll();
            long startTime = System.currentTimeMillis();
            try {
                org.jsoup.Connection.Response response = Jsoup.connect(link).timeout(getTimeout() * 1000).validateTLSCertificates(false).ignoreContentType(true).execute();
                long responseTime = System.currentTimeMillis() - startTime;
        //        System.out.println("This is thread " + threadName + " Checking link: " + link + " Got response code: " + response.statusCode() + " Response time: " + responseTime);
                if(checkStatusCode(response.statusCode())){
                    resultsLink.add(link + " Response code: " + response.statusCode() + " Response time: " + responseTime);
                }
                if(typeOfLink(link)){
                    crawlPage(response);
                }
            } catch (HttpStatusException httpex){
                long responseTime = System.currentTimeMillis() - startTime;
         //       System.out.println("This is thread " + threadName + " Checking link: " + link + " Got response code: " + httpex.getStatusCode() + " Got error: " + httpex.getMessage() +" Response time: " + responseTime);
                if(checkStatusCode(httpex.getStatusCode())){
                    resultsLink.add(link + " Response code: " + httpex.getStatusCode() + " Response time: " + responseTime);
                }
                continue;
            } catch (IllegalArgumentException illegalURL){
         //       System.out.println("This is thread " + threadName + " Checking link: " + link + " Illegal URL.!!!!!");
                //resultsLink.add(link + " Illegal URL.!!!!!");
            }
            catch(SocketTimeoutException sock){
                long responseTime = System.currentTimeMillis() - startTime;
         //       System.out.println("This is thread " + threadName + " Checking link: " + link + " Got Timeout after " + responseTime + " seconds");
                resultsLink.add(link + " Got Timeout after " + responseTime + " seconds");
            }
            catch (MalformedURLException mal){
        //        System.out.println("This is thread " + threadName + " Checking link: " + link + " Malformed URL");
                resultsLink.add(link + " Malformed URL");
            }
            catch (IOException ioex) {
                ioex.printStackTrace();
                continue;
            }

        }

        while(!currentSources.isEmpty()){
            link = currentSources.poll();
            long startTime = System.currentTimeMillis();

            try {
                Connection.Response response = Jsoup.connect(link).timeout(getTimeout() * 1000).validateTLSCertificates(false).ignoreContentType(true).execute();
                long responseTime = System.currentTimeMillis() - startTime;
          //      System.out.println("This is thread " + threadName + " Checking source: " + link + " Got response code: " + response.statusCode() + " Response time: " + responseTime);
                if(checkStatusCode(response.statusCode())){
                    resultsSrc.add(link + " Response code: " + response.statusCode() + " Response time: " + responseTime);
                }
            } catch (HttpStatusException httpex){
                long responseTime = System.currentTimeMillis() - startTime;
          //      System.out.println("This is thread " + threadName + " Checking source: " + link + " Got response code: " + httpex.getStatusCode() + " Got error: " + httpex.getMessage() +" Response time: " + responseTime);
                if(checkStatusCode(httpex.getStatusCode())){
                    resultsSrc.add(link + " Response code: " + httpex.getStatusCode() + " Response time: " + responseTime);
                }
                continue;
            } catch (IllegalArgumentException illegalURL){
        //        System.out.println("This is thread " + threadName + " Checking source: " + link + " Illegal URL.!!!!!");
               // resultsSrc.add(link + " Illegal URL.!!!!!");
            }
            catch(SocketTimeoutException sock){
                long responseTime = System.currentTimeMillis() - startTime;
         //       System.out.println("This is thread " + threadName + " Checking source: " + link + " Got Timeout after " + responseTime + " seconds");
                resultsSrc.add(link + " Got Timeout after " + responseTime + " seconds");
            }
            catch (MalformedURLException mal){
        //        System.out.println("This is thread " + threadName + " Checking link: " + link + " Malformed URL");
                resultsSrc.add(link + " Malformed URL");
            }
            catch (IOException ioex) {
                ioex.printStackTrace();
                continue;
            }
        }

      //  System.out.println(threadName+" I'm done");

    }
}
