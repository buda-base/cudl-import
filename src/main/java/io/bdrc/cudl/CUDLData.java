package io.bdrc.cudl;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CUDLData {

    public static String rootDir=null;
    public static Properties props;
    public static HashMap<String,JsonNode> JSON_INF;
    public static HashMap<String,String> XML_INF;

    static {
        props=new Properties();
        try {
            props.load(CUDLData.class.getClassLoader().getResourceAsStream("cudl.properties"));
            rootDir=props.getProperty("rootDir");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    static void loadData() throws ClientProtocolException, IOException {
        int start=1;
        int end=20;
        boolean ok=loadJsonInfo(start, end);
        while(ok) {
            start=start+20;
            end=end+20;
            ok=loadJsonInfo(start, end);
        }
        Set<String> ids=JSON_INF.keySet();
        for(String id:ids) {
            System.out.println("Processing >>"+id);
            loadXMLInfo(id);
        }
    }

    private static void loadXMLInfo(String id) throws ClientProtocolException, IOException {
        if(XML_INF==null) {
            XML_INF=new HashMap<>();
        }
        String url="https://services.cudl.lib.cam.ac.uk/v1/metadata/tei/"+id;
        HttpClient client=HttpClientBuilder.create().build();
        HttpGet get=new HttpGet(url);
        get.addHeader("Accept","application/xml");
        HttpResponse resp=client.execute(get);
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        resp.getEntity().writeTo(baos);
        String xml=baos.toString();
        baos.close();
        writeFile(id+".xml","xml",xml);
        XML_INF.put(id,xml);
    }


    private static boolean loadJsonInfo(int start, int end) throws ClientProtocolException, IOException {
        ObjectMapper mapper=new ObjectMapper();
        if(JSON_INF==null) {
            JSON_INF=new HashMap<>();
        }
        String url="https://cudl.lib.cam.ac.uk/search/JSON?fileID=&keyword=Bauddha&start="+start+"&end="+end;
        HttpClient client=HttpClientBuilder.create().build();
        HttpGet get=new HttpGet(url);
        get.addHeader("Accept","application/json");
        HttpResponse resp=client.execute(get);
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        resp.getEntity().writeTo(baos);
        String json_resp=baos.toString();
        baos.close();
        JsonNode node=mapper.readTree(json_resp);
        List<JsonNode> nodes=node.findValues("item");
        if(nodes.isEmpty()) {
            return false;
        }
        for(JsonNode item:nodes) {
            JSON_INF.put(item.findValue("id").get(0).asText(),item);
            writeFile(item.findValue("id").get(0).asText()+".json","json",item.toString());
        }
        return true;
    }

    public static JsonNode getNodeFromFile(String filename) throws IOException {
        ObjectMapper mapper=new ObjectMapper();
        return mapper.readTree(FileUtils.readFileToString(new File(filename),"UTF-8"));
    }

    public static String getXMLFileContents(String filename) throws IOException {
        return FileUtils.readFileToString(new File(filename),"UTF-8");
    }

    private static void writeFile(String filename, String dir, String content) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(rootDir+"/"+dir+"/"+filename));
        writer.write(content);
        writer.close();
    }

    /*public static void main(String[] args) throws ClientProtocolException, IOException {
        //CUDLData.loadData();
        System.out.println(CUDLData.getNodeFromFile(rootDir+"/json/MS-ADD-01333.json"));
    }*/

}
