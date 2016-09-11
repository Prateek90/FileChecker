

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package csefsck;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.io.FileNotFoundException;
import java.io.FileReader;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.io.FileWriter;

/**
 *
 * @author Prateek
 */
public class Csefsck {

    /**
     * @param args the command line arguments
     */
    
    public static JSONParser parser = new JSONParser();
    public static List data_blocks=new ArrayList();
    
    //Give the directory where the fusedata.X file are stored
    public static String file_loc="F:\\FS\\";
    public static String file_name="fusedata.";
    
    public static void main(String[] args) {
  
        try{
            
            JSONObject super_block = (JSONObject) parser.parse(new FileReader(file_loc.concat(file_name).concat("0")));
            JSONArray freeblk=new JSONArray();
            FileWriter file_write=new FileWriter(file_loc.concat(file_name).concat("0"));
            //System.out.println("Free block are:"+freeblk);
            //JSONObject free=new JSONObject();
            Long devId=(Long)super_block.get("devId");
            
            //Code for testing if Device-id is correct or not
            if(devId!=20){
                System.out.println("Incorrect Device Id:"+devId+"\nExiting FileChecker");
                System.exit(1);
            }
            
            //Code for testing if creationTime is not in the future.
            if((long)super_block.get("creationTime")>System.currentTimeMillis())
                super_block.put("creationTime", System.currentTimeMillis());
            
            Long freeStart=(Long)super_block.get("freeStart");
            Long freeEnd=(Long)super_block.get("freeEnd");
            Long maxBlocks=(Long)super_block.get("maxBlocks");

            //Code that test if freeStart and FreeEndlocations are correct or not.
            if(freeStart != 1){
                System.out.println("Error encountered in freeStart\nIncorrect freeStart:"+freeStart);
                freeStart=1l;
                super_block.put("freeStart", freeStart);
                System.out.println("Correct freeStart:"+freeStart);
            }
            if(freeEnd != (maxBlocks/400)){
                System.out.println("Error encountered in freeEnd field\nIncorrecct freeEnd:"+freeEnd);
                freeEnd=(maxBlocks/400);
                super_block.put("freeStart", freeEnd);
                System.out.println("Correcting freeEnd\nfreeEnd:"+freeEnd);
            }
            
            //Code that checks if Max Blocks provided are positive or not
            if(maxBlocks < 0){
                System.out.println("Inappropriate size of Maximum Blocks:"+maxBlocks);
                System.out.println("Exiting FileChecker");
                System.exit(1);
            }
            
            //Code that test if Number of mounts are positive or not
            Long mounts=(Long)super_block.get("mounted");
            if(mounts < 0){
                System.out.println("Inappropriate number of mounts"+mounts);
                super_block.put("mounted",0);
                System.out.println("Reinitializing mounted to 0");
                
            }
            
            file_write.write(super_block.toJSONString());
            file_write.flush();
            file_write.close();
            Long root=(Long)super_block.get("root");
            String block_name=file_loc.concat(file_name).concat(Long.toString(root));
            JSONObject root_data= (JSONObject) parser.parse(new FileReader(block_name));
            //data_blocks.add(root);
            root_data = processRoot(root_data,root);
            file_write=new FileWriter(block_name);
            file_write.write(root_data.toJSONString());
            file_write.flush();
            file_write.close();
            for(long i = 1 ; i <= maxBlocks; i=i+400){
                block_name=file_loc.concat(file_name).concat(Long.toString((i/400) +1));
                freeblk= (JSONArray) parser.parse(new FileReader(block_name));
                //file_write=new FileWriter(block_name);
               // System.out.println(freeblk);
                for(long j=i;j<i+400;j++){
                    if(j<=25)
                        data_blocks.add(j);
                    
                    if(!freeblk.contains(j)){
                        freeblk.add(j);
                        //System.out.println("Adding"+j);
                }
                }
                //System.out.println(data_blocks);
                freeblk.removeAll(data_blocks);
                //freeblk.remove(data_blocks);
               
                file_write=new FileWriter(block_name);
                file_write.write(freeblk.toJSONString());
                file_write.flush();
                file_write.close();

            }
        }catch(FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }catch (ParseException e) {
            e.printStackTrace();
        }
        
    }

    //Code for processing of root directory.
    public static JSONObject processRoot(JSONObject root, long root_block){
        
        root=timeCheck(root);
        
        String block_name=new String();
        JSONArray root_arr=(JSONArray) root.get("filename_to_inode_dict");
        JSONObject root_ele=new JSONObject();
        JSONObject new_block=new JSONObject();
        JSONObject new_direc=new JSONObject();
        JSONObject new_file=new JSONObject();
        Long block_num;
        data_blocks.add(root_block);
        //System.out.println(root_block);
 
        //Code for traversing and testing each element of the JSONArray
        try{
            for(int i=0;i<root_arr.size();i++){
                root_ele=(JSONObject)root_arr.get(i);
                block_num=(Long)root_ele.get("location");
                if(root_ele.get("name").equals(".")){
                    if((long)root_ele.get("location")!=root_block){
                        System.out.println("Wrong location for \".\" in root directory");
                        root_ele.put("location", root_block);
                        System.out.println("Root location for \".\" corrected to "+root_block);
                    }
                }
                else if(root_ele.get("name").equals("..")){
                    if((long)root_ele.get("location")!=root_block){
                        System.out.println("Wrong location for \"..\" in root directory\nWrong Location:"+(long)root_ele.get("location"));
                        root_ele.put("location", root_block);
                        System.out.println("Root location for \"..\" corrected to "+root_block);
                    }
                }
                else if(root_ele.get("type").equals("d")){
                    block_num=(Long)root_ele.get("location");
                    //System.out.println("abcd"+block_num);
                    block_name=file_loc.concat(file_name).concat(Long.toString(block_num));
                    new_direc= (JSONObject) parser.parse(new FileReader(block_name));
                    FileWriter file_direc=new FileWriter(block_name);
                    new_direc=processDirec(new_direc,block_num,root_block);
                    file_direc.write(new_direc.toJSONString());
                    file_direc.flush();
                    file_direc.close();
                }
                else if(root_ele.get("type").equals("f")){
                    block_num=(Long)root_ele.get("location");
                    //System.out.println("jsan"+block_num);
                    block_name=file_loc.concat(file_name).concat(Long.toString(block_num));
                    new_file= (JSONObject) parser.parse(new FileReader(block_name));
                    FileWriter file_f=new FileWriter(block_name);
                    new_file=processFile(new_file,block_num);
                    file_f.write(new_file.toJSONString());
                    file_f.flush();
                    file_f.close();
                }
                
            }
            root.put("filename_to_inode_dict",root_arr);
            
        }catch(FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }catch (ParseException e) {
            e.printStackTrace();
        }
        return root;
    }
    
    //Code for checking directory entry of a directory
    public static JSONObject processDirec(JSONObject new_block,Long current_add,Long parent){
        new_block=timeCheck(new_block);
        String block_name = new String();
        JSONArray current_direc = (JSONArray) new_block.get("filename_to_inode_dict");
        JSONObject current_ele = new JSONObject();
        JSONObject blocks = new JSONObject();
        JSONObject new_direc = new JSONObject();
        JSONObject new_file = new JSONObject();
        data_blocks.add(current_add);
        //System.out.println(current_add);
        Long block_no;
        try{
            for(int i=0;i < current_direc.size();i++){
                current_ele = (JSONObject)current_direc.get(i);
                //block_no=(Long)current_ele.get("location");
                if(current_ele.get("name").equals(".")){
                    if((long)current_ele.get("location")!= current_add){
                        System.out.println("Wrong location for \".\" in fusedata."+ current_add +" directory");
                        current_ele.put("location", current_add);
                        System.out.println("location for \".\" corrected to "+current_add);
                    }
                }
                else if(current_ele.get("name").equals("..")){
                    if((long)current_ele.get("location")!=parent){
                        System.out.println("Wrong location for \"..\" in fusedata."+ current_add +" directory");
                        current_ele.put("location", parent);
                        System.out.println("Location for \"..\" corrected to "+parent);
                    }
                }
                else if(current_ele.get("type").equals("d")){
                    block_no=(Long)current_ele.get("location");
                    block_name=file_loc.concat(file_name).concat(Long.toString(block_no));
                    new_direc= (JSONObject) parser.parse(new FileReader(block_name));
                    FileWriter file_direc=new FileWriter(block_name);
                    new_direc = processDirec(new_direc,block_no,current_add);
                    file_direc.write(new_direc.toJSONString());
                    file_direc.flush();
                    file_direc.close();
                }
                else if(current_ele.get("type").equals("f")){
                    block_no=(Long)current_ele.get("location");
                    block_name=file_loc.concat(file_name).concat(Long.toString(block_no));
                    new_file= (JSONObject) parser.parse(new FileReader(block_name));
                    FileWriter file_f=new FileWriter(block_name);
                    new_file = processFile(new_file,block_no);
                    file_f.write(new_file.toJSONString());
                    file_f.flush();
                    file_f.close();
                }
            }
        new_block.put("filename_to_inode_dict",current_direc);
        }catch(FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }catch (ParseException e) {
            e.printStackTrace();
        }
        return new_block;
    }
    
    //Code for checking file entry of file.
    public static JSONObject processFile(JSONObject file,Long block_num){
        file=timeCheck(file);
        long size_file = (long)file.get("size");
        JSONArray loc=new JSONArray();
        data_blocks.add(block_num);
        //System.out.println(block_num);
        if(size_file > 4096l){
            loc=(JSONArray)file.get("location");
        }
        else if(size_file < 4096l){

            loc.add(file.get("location"));
        }
        
        //System.out.println(loc);
        
      if(size_file > 4096l){
            if((long)file.get("indirect")==1l && loc.size()<2){
                System.out.println("Wrong value of indirect in fusedata."+block_num+" : "+(long)file.get("indirect"));
                file.put("indirect",0l);
                System.out.println("Correcting indirect error,New Indirect Value:"+(int)file.get("indirect"));
                
            }
      }
            if(size_file < 4096l)
            {
                if((long)file.get("indirect")!= 0l){
                    System.out.println("Indirect should be 0 as file size is less than 4096 in fusedata."+block_num);
                    file.put("indirect",0l);
                    System.out.println("New Indirect Value: "+file.get("indirect"));
                }
                if(size_file < 0l){
                    System.out.println("Size of file cannot be non-positive in fusedata."+block_num);
                    file.put("size", 1l);
                    System.out.println("Size of file modified to 1");
                }
            }
            if((long)file.get("indirect")!=0l){
                if(size_file > (long)(4096 * loc.size())){
                    System.out.println("Incorrect file size in fusedata."+block_num+" : "+ size_file);
                    file.put("size",(long)(4096*loc.size()));
                    System.out.println("New File size:"+4096*loc.size());
                }
                if(size_file < (long)(4096 * (loc.size()-1))){
                    System.out.println("Incorrect file size in fusedata."+block_num+" : "+size_file);
                    file.put("size", (long)(4096*loc.size()-1));
                    System.out.println("New File size:"+(4096*loc.size()-1));
                }
            }
            if(loc.size()!= ((size_file/4096)+1))
            {
                System.out.println("Inappropriate file size detected in fusedata."+block_num+"\nTruncating file size to fit in the given blocks");
                file.put("size", (long)(loc.size()*4096));
                System.out.println("New File size:"+loc.size()*4096);
            }
        
        return file;
    }
    
    //Code for Checking if creation time,accesstime or modified time are not in future.
    public static JSONObject timeCheck(JSONObject obj){
        Long epochs=System.currentTimeMillis();
     
        Long ctime=(Long)obj.get("ctime");

        Long atime=(Long)obj.get("atime");

        Long mtime=(Long)obj.get("mtime");

        if(ctime > epochs){
            ctime = epochs;
            obj.put("ctime",ctime);
        }
        if(atime>epochs){
            atime=epochs;
            obj.put("atime",atime);
        }
        if(mtime>epochs){
            mtime=epochs;
            obj.put("mtime",mtime);
        }
        return obj;
    }
    
}


