package io.bdrc.cudl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.json.XML;
import org.jsoup.Jsoup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVWriter;

public class CSVBuilder {

    public static String[] headers=
                         {"rid",
                          "abstractText",
                          "abstractShort",
                          "title",
                          "mainTitle",
                          "altTitle",
                          "manifestUrl",
                          "material_support",
                          "origNotBefore",
                          "origNotAfter",
                          "origPlaceKey",
                          "origPlaceName",
                          "script",
                          "lang",
                          "paperDimHeight",
                          "paperDimWidth",
                          "textDimHeight",
                          "textDimWidth"};

    public static String[] getCsvLineAsList(String id) throws IOException{
        String[] csvLine=new String[18];
        JsonNode node=CUDLData.getNodeFromFile(CUDLData.rootDir+"/json/"+id+".json");
        csvLine[0]= id;
        csvLine[1] = Jsoup.parse(node.findValue("abstractText").get(0).asText()).text();
        csvLine[2]= node.findValue("abstractShort").get(0).asText();
        csvLine[3]= node.findValue("title").get(0).asText();
        return AddXmlData(csvLine,id);
    }

    public static String[] AddXmlData(String[] csvLine, String id) throws IOException {
        String xml=CUDLData.getXMLFileContents(CUDLData.rootDir+"/xml/"+id+".xml");
        String json=XML.toJSONObject(xml).toString();
        ObjectMapper mapper=new ObjectMapper();
        JsonNode node=mapper.readTree(json);
        //Should we process this record (does it have images ?)
        if(node.at("/TEI/facsimile").findValue("graphic")!=null) {
            csvLine[6]="https://cudl.lib.cam.ac.uk/iiif/"+id;
            String alt="";
            List<JsonNode> title=node.at("/TEI/teiHeader/fileDesc/sourceDesc/msDesc/msContents/msItem").findValues("title");
            for(JsonNode n:title.get(0)) {
                if(n.findValue("type")==null) {
                    if(n.findValue("content")!=null) {
                        csvLine[4]=n.findValue("content").asText();
                    }
                }else {
                    alt=alt+","+n.findValue("content").asText();
                }
            }
            if(!alt.equals("")) {
                csvLine[5]=alt.substring(1);
            }
            JsonNode support=node.at("/TEI/teiHeader/fileDesc/sourceDesc/msDesc/physDesc/objectDesc/supportDesc/material");
            csvLine[7]=parseNodeValue(support);
            JsonNode history=node.at("/TEI/teiHeader/fileDesc/sourceDesc/msDesc/history/origin");
            if(history!=null) {
                if(history.findValue("origDate")!=null) {
                    csvLine[8]=parseNodeValue(history.findValue("origDate").findValue("notBefore"));
                    csvLine[9]=parseNodeValue(history.findValue("origDate").findValue("notAfter"));
                }
                if(history.findValue("origPlace")!=null) {
                    csvLine[10]=parseNodeValue(history.findValue("origPlace").findValue("key"));
                    csvLine[11]=parseNodeValue(history.findValue("origPlace").findValue("content"));
                }
            }
            JsonNode script=node.at("/TEI/teiHeader/fileDesc/sourceDesc/msDesc/physDesc/handDesc/handNote/script");
            csvLine[12]=parseNodeValue(script);
            JsonNode textLang=node.at("/TEI/teiHeader/fileDesc/sourceDesc/msDesc/msContents/msItem/textLang/mainLang");
            csvLine[13]=parseNodeValue(textLang);
            JsonNode paperHeight=node.at("/TEI/teiHeader/fileDesc/sourceDesc/msDesc/physDesc/objectDesc/supportDesc/support");
            csvLine[14]=getDimension(parseNodeValue(paperHeight.findPath("dimensions").findPath("height").findValue("quantity")));
            csvLine[15]=getDimension(parseNodeValue(paperHeight.findPath("dimensions").findPath("width").findValue("quantity")));
            JsonNode textHeight=node.at("/TEI/teiHeader/fileDesc/sourceDesc/msDesc/physDesc/objectDesc/layoutDesc/layout");
            csvLine[16]=getDimension(parseNodeValue(textHeight.findPath("dimensions").findPath("height").findValue("content")));
            csvLine[17]=getDimension(parseNodeValue(textHeight.findPath("dimensions").findPath("width").findValue("content")));
            return csvLine;
        }
        return null;
    }

    private static String parseNodeValue(JsonNode node) {
        if(node!=null) {
            return node.asText();
        }else {
            return "";
        }
    }

    public static String getDimension(String dim) {
        if(!dim.contains("-")) {
            return dim;
        }else {
            return dim.split("-")[1];
        }
    }

    public static void buildCsv() throws IOException {
        List<String> files=getJsonFiles();
        Collections.sort(files);
        CSVWriter writer = new CSVWriter(new FileWriter(CUDLData.rootDir+"cudl.csv"));
        writer.writeNext(headers);
        for(String s:files) {
            if(s.endsWith(".json")) {
                String[] list= getCsvLineAsList(s.substring(0, s.indexOf(".")));
                if(list!=null) {
                    writer.writeNext(list);
                }
            }
        }
        writer.close();
    }

    private static List<String> getJsonFiles(){
        List<String> files=null;
        File file = new File(CUDLData.rootDir+"json");
        if (file.isDirectory()) {
            String names[] = file.list();
            files=Arrays.asList(names);
        }
        return files;
    }

    public static void main(String[] args) throws IOException {
        /*System.out.println(CSVBuilder.getCsvLineAsList("MS-ADD-01405"));
        System.out.println(CSVBuilder.getCsvLineAsList("MS-ADD-01333"));
        System.out.println(CSVBuilder.getJsonFiles());*/
        CSVBuilder.buildCsv();
    }

}
