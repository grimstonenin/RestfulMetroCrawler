package MetroCrawler.Controller;



import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.awt.*;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;

@Path("/MetroCrawlerRESTFul")
public class MetroCrawlerRESTFul {


    ArrayList<Integer> errors = new ArrayList<>();

    @DefaultValue("") @QueryParam("link") String link;
    @DefaultValue("1") @QueryParam("depth") int depth;
    @DefaultValue("10") @QueryParam("timeout") int timeout;
    @DefaultValue("0") @QueryParam("errorcode") String errorCode;
    @DefaultValue("0") @QueryParam("crawltime") int crawlTime;
    @DefaultValue("false") @QueryParam("followexternal") boolean follow;

    private void initiateCrawl(){
        MetroCrawler m = new MetroCrawler(link);
        m.setDepth(depth);
        m.setTimeout(timeout);

       if(errorCode.equals("0")){
            errors.add(0);
        }
        else {
            if(!errorCode.contains(",")){
                errors.add(Integer.parseInt(errorCode));
            }
            else {
                ArrayList<String> temp = new ArrayList<>(Arrays.asList(errorCode.split(",")));
                for (String s : temp){
                    errors.add(Integer.parseInt(s));
                }

            }
        }

        m.setCrawlTime(crawlTime);
        m.setFollowExternalLinks(follow);



    }
    //this function checks if the supplied parameters are fine
    public String checkParams(){
        return "test";
    }


    @GET
    @Path("/crawl")
    @Produces(MediaType.TEXT_HTML)
    public String crawl(){



        return "ok";
    }





}
