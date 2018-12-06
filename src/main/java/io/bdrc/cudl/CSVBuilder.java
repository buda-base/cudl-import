package io.bdrc.cudl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.XML;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVWriter;

public class CSVBuilder {

    public static ArrayList<ArrayList<String>> getCsvLineAsList(String id) throws IOException{
        ArrayList<ArrayList<String>> res= new ArrayList<ArrayList<String>>();
        ArrayList<String> csvLine=new ArrayList<>();
        ArrayList<String> firstLine=new ArrayList<>();
        JsonNode node=CUDLData.getNodeFromFile(CUDLData.rootDir+"/json/"+id+".json");
        csvLine.add(0, id);
        firstLine.add(0, "rid");
        csvLine.add(1, node.findValue("abstractText").get(0).asText());
        firstLine.add(1, "abstractText");
        csvLine.add(2, node.findValue("abstractShort").get(0).asText());
        firstLine.add(2, "abstractShort");
        csvLine.add(3, node.findValue("title").get(0).asText());
        firstLine.add(3, "title");
        res.add(0,firstLine);
        res.add(1,csvLine);
        return AddXmlData(res,id);
    }

    public static ArrayList<ArrayList<String>> AddXmlData(ArrayList<ArrayList<String>> list, String id) throws IOException {
        ArrayList<String> csvLine=list.get(1);
        ArrayList<String> firstLine=list.get(0);
        int index=list.get(0).size();
        String xml=CUDLData.getXMLFileContents(CUDLData.rootDir+"/xml/"+id+".xml");
        String json=XML.toJSONObject(xml).toString();
        ObjectMapper mapper=new ObjectMapper();
        JsonNode node=mapper.readTree(json);
        //Should we process this record (does it have images ?)
        if(node.at("/TEI/facsimile").findValue("graphic")!=null) {
            firstLine.add(index,"manifestUrl");
            csvLine.add(index,"https://cudl.lib.cam.ac.uk/iiif/"+id);
            index++;
            List<JsonNode> title=node.at("/TEI/teiHeader/fileDesc/sourceDesc/msDesc/msContents/msItem").findValues("title");
            for(JsonNode n:title.get(0)) {
                if(n.findValue("type")==null) {
                    if(n.findValue("content")!=null) {
                        firstLine.add(index,"mainTitle");
                        csvLine.add(index,n.findValue("content").asText());
                        index++;
                    }
                }else {
                    firstLine.add(index,"altTitle");
                    csvLine.add(index,n.findValue("content").asText());
                    index++;
                }
            }
            list.add(0,firstLine);
            list.add(1,csvLine);
            return list;
        }
        return null;
    }

    public static void buildCsv() throws IOException {
        List<String> files=getJsonFiles();
        ArrayList<ArrayList<String>> lines=new ArrayList<>();
        CSVWriter writer = new CSVWriter(new FileWriter(CUDLData.rootDir+"cudl.csv"));
        boolean header=false;
        for(String s:files) {
            if(s.endsWith(".json")) {
                ArrayList<ArrayList<String>> list= getCsvLineAsList(s.substring(0, s.indexOf(".")));
                if(list!=null) {
                    if(!header && list.get(0).size()==7) {
                        lines.add(0,list.get(0));
                        header=true;
                    }
                    lines.add(list.get(1));
                }
            }
        }
        for(ArrayList<String> ls:lines) {
            writer.writeNext(ls.toArray(new String[0]));
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
