/*
 *  Copyright (c) Jan 31, 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */



package kasun.test.testingMaven;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class GettingBlameCommit extends CallingAPI {

    private String jsonOutPutFileOfSearchCommitAPI="jsonOutPutFileOfSearchCommitAPI.json";
    private String urlForObtainingCommits,urlForGetingFilesChanged;

    protected ArrayList<String> fileNames = new ArrayList<String>();
    protected List<ArrayList<String>> lineRangesChanged= new ArrayList<ArrayList<String>>();      // for saving the line no that are changed




    private String repoLocation[];


    public String getUrlForSearchingCommits() {
        return urlForObtainingCommits;
    }

    public void setUrlForSearchingCommits(String commitHash) {
        this.urlForObtainingCommits = "https://api.github.com/search/commits?q=hash%3A"+commitHash;
    }

    public String getUrlForGetingFilesChanged() {
        return urlForGetingFilesChanged;
    }
    public void setUrlForGetingFilesChanged(String repoName,String commitHash) {
        this.urlForGetingFilesChanged ="http://api.github.com/repos/"+repoName+"/commits/"+commitHash;
    }









    //================ obtaining PR for each commit and saving them in a file ===================================
    public void obtainingRepoNamesForCommitHashes() throws IOException{


        for(String commitHash: patchInformation_svnRevisionpublic){

            setUrlForSearchingCommits(commitHash);


            //calling the API calling method
            callingTheAPI(getUrlForSearchingCommits(),jsonOutPutFileOfSearchCommitAPI,true,true,false);
            saveRepoNamesInAnArray(commitHash);

        }



    }

    //================================= saving the  Repo Names in the array and calling to Get files content========================================

    public void saveRepoNamesInAnArray(String commitHash){
        try{


            JSONObject rootJsonObject= (JSONObject)parser.parse(new FileReader(location+jsonOutPutFileOfSearchCommitAPI));
            JSONArray jsonArrayOfItems= (JSONArray)rootJsonObject.get("items");

            // setting the size of the repoLocationArray
            repoLocation= new String [jsonArrayOfItems.size()];

            for(int i=0; i<jsonArrayOfItems.size();i++){
                JSONObject jsonObject = (JSONObject) jsonArrayOfItems.get(i);

                JSONObject repositoryJsonObject= (JSONObject)jsonObject.get("repository");

                //adding the repo name to the array
                repoLocation[i]= (String)repositoryJsonObject.get("full_name");

            }
        }

        catch(FileNotFoundException e){
            e.printStackTrace();
        }
        catch(ParseException e){
            e.printStackTrace();
        }
        catch(IOException e){
            e.printStackTrace();
        }

        //        for running through the repoName Array 
        for(int i =0; i< repoLocation.length;i++){

            //clearing all the data in the current fileNames and lineRangesChanged arraylists for each repository
            fileNames.clear();
            lineRangesChanged.clear();


            callingToGetFilesChanged(repoLocation[i],commitHash); 


            //            calling the graphql API for getting blame information
            callingGraphqlApi(repoLocation[i],commitHash);

        }


    }


    //============================== calling Single commit API to get files changed in a commit=======================

    public void callingToGetFilesChanged(String repoLocation, String commitHash){

        //        setting the URL for calling github single commit API

        setUrlForGetingFilesChanged(repoLocation,commitHash);

        //file name for saving the output
        String savingLocation= repoLocation+"/"+commitHash+".json";

        //saving the commit details for the commit hash on the relevant repository
        try {
            callingTheAPI(getUrlForGetingFilesChanged(), savingLocation, true,false,false);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } 


        //----calling savingRelaventFileNamesAndEditLineNumbers method to read the above saved json output----
        savingRelaventFileNamesAndEditLineNumbers(savingLocation);





    }

    //    ======saving relevant file names and their edit line numbers =================================

    public void savingRelaventFileNamesAndEditLineNumbers(String savingLocation){

        //read the json output
        try {

            JSONObject rootJsonObject=(JSONObject) parser.parse(new FileReader(location+savingLocation));
            Object checkObject= rootJsonObject.get("files");
            //checking if the json stream received for element "files" is a jsonObject or a jsonArray

            if(checkObject instanceof JSONObject ){
                // if it is a JSONObject then only one file has been changed from the commit

                // then casting the checkObject to a JSONObject
                JSONObject filesJsonObject=(JSONObject)checkObject;
                String fileName=(String)filesJsonObject.get("filename");
                fileNames.add(fileName);

                //filtering only the line no that are modified

                String patch= (String)filesJsonObject.get("patch");
                String lineChanges[]=StringUtils.substringsBetween(patch,"@@", "@@");


            }
            else if (checkObject instanceof JSONArray){
                //                 more than one file has been changed by the relevant commit
                JSONArray fileJsonArray= (JSONArray)checkObject;

                // to save one file at a time
                for(int i =0; i< fileJsonArray.size();i++){
                    JSONObject tempJsonObject= (JSONObject) fileJsonArray.get(i);
                    String fileName= (String)tempJsonObject.get("filename");
                    //saving the file name in the filename arraylist
                    fileNames.add(fileName);

                    //filtering only the line ranges that are modified and saving to a string array
                    String patch= (String)tempJsonObject.get("patch");
                    String lineChanges[]= StringUtils.substringsBetween(patch,"@@","@@");

                    //filtering only the lines that existed in the previous file and saving them in to the same array
                    for (int j=0; j<lineChanges.length;j++){

                        String tempString= lineChanges[i];
                        String tempStringWithLinesBeingModified = StringUtils.substringBetween(tempString,"-"," +");

                        int intialLineNo= Integer.parseInt(StringUtils.substringBefore(tempStringWithLinesBeingModified, ","));
                        int tempEndLineNo= Integer.parseInt(StringUtils.substringAfter(tempStringWithLinesBeingModified,","));
                        int endLineNo= intialLineNo+ (tempEndLineNo-1);

                        // storing the line ranges that are being modified in the same array by replacing values
                        lineChanges[j]=intialLineNo+","+endLineNo;

                    }

                    ArrayList<String> tempArrayList= new ArrayList<String>(Arrays.asList(lineChanges));

                    //adding to the array list which keep track of the line ranges which are being changed to the main arrayList
                    lineRangesChanged.add(tempArrayList);





                    System.out.println("done");




                }

                System.out.println(fileNames);
                System.out.println(lineRangesChanged);


            }






        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }





    }

    //============================= calling the graphql of GitHub through running curl command ==========================================

    //        public void callingGraphqlApi(String repoLocation,String commitHash){
    //    
    //            String owner= StringUtils.substringBefore(repoLocation,"/");
    //            String repositoryName= StringUtils.substringAfter(repoLocation,"/");
    //    
    //    
    //    
    //    
    //            // url for graphql API
    //            String apiUrl= "https://api.github.com/graphql";
    //    
    //    
    //            //        using an iterator for looping through the arraylists of filenames
    //    
    //    
    //            Iterator iteratorForFileNames= fileNames.iterator();
    //            while(iteratorForFileNames.hasNext()){
    //    
    //                String fileName= (String)iteratorForFileNames.next();
    //    
    //    
    //                String[] curlCommand = {"curl", "-H" ,"Authorization: Bearer "+getToken(),"-H","Accept:application/json","-X", "POST", "-d", "{\"query\": \"query { repository(owner: \\\""+owner+"\\\", name: \\\""+repositoryName+"\\\") { object(expression:\\\""+commitHash+"\\\") { ... on Commit { blame(path: \\\""+fileName+"\\\") { ranges { startingLine endingLine age commit { history(first: 2) { edges { node { message url } } } author { name email } } } } } } } }\"}" , apiUrl};
    //                
    //    //            creating a processBuilder instance for running the curl command with the given set of arguments
    //                ProcessBuilder processBuilder= new ProcessBuilder(curlCommand);
    //                Process p;
    //                
    //                
    //                try{
    //                    p= processBuilder.start(); // staring new subprocess with uses the default working directory and environment variables
    //                    BufferedReader bufferedReader= new BufferedReader(new InputStreamReader(p.getInputStream()));   //obtaining the output of the subprocess through the Parent Process inputStream
    //                    StringBuilder stringBuilder= new StringBuilder();
    //                    String line=null;
    //                    while((line=bufferedReader.readLine())!= null){
    //                        stringBuilder.append(line);
    //                        stringBuilder.append(System.getProperty("line.separator"));
    //                        
    //                        
    //                        
    //                    }
    //                    System.out.println(stringBuilder.toString());
    //                    
    //                }
    //                catch(IOException e){
    //                    e.printStackTrace();
    //                }
    //                
    //                
    //                
    //               
    //                
    //               
    //                
    //                
    //    
    //    
    //    
    //            }
    //    
    //    //        String[] command = {"curl", "-H" ,"Authorization: Bearer"+getToken(),"-H","Accept:application/json","-X", "POST", "-d", "{\"query\": \"query { repository(owner: \\\"wso2-extensions\\\", name: \\\"identity-inbound-auth-oauth\\\") { object(expression:\\\"83253ce50f189db30c54f13afa5d99021e2d7ece\\\") { ... on Commit { blame(path: \\\"components/org.wso2.carbon.identity.oauth.endpoint/src/main/java/org/wso2/carbon/identity/oauth/endpoint/authz/OAuth2AuthzEndpoint.java\\\") { ranges { startingLine endingLine age commit { message url history(first: 2) { edges { node { message url } } } author { name email } } } } } } } }\"}" , apiUrl};
    //    
    //        }


















    //        public void callGraphqlApi(String repoLocation,String commitHash){
    //        
    //            CloseableHttpClient httpClientForGraphql = null;
    //            CloseableHttpResponse httpResponseFromGraphql= null;
    //    
    //    
    //            // same token for calling github API are used for the calling the github Graphql API
    //    
    //            httpClientForGraphql=HttpClients.createDefault();
    //            HttpPost httpPost= new HttpPost("https://api.github.com/graphql");
    //    
    //            //       =============== work in here=============================
    //    
    //    
    //            JSONObject jsonObject=  new JSONObject();
    //            JSONObject p = new JSONObject();
    //            p.put("login", null);
    //    
    //            jsonObject.put("viewer", p);
    //    
    //    
    //    
    //            //        String query="{\"query\": \"query {repository(owner:\"wso2\",name:\"product-is\"){description}}\"}";
    //    
    //            //        String query= "{\"query\":\"query { repository(owner: \"wso2-extensions\", name:\"identity-inbound-auth-oauth\") { object(expression: \"83253ce50f189db30c54f13afa5d99021e2d7ece\") { ... on Commit { blame(path: \"components/org.wso2.carbon.identity.oauth.endpoint/src/main/java/org/wso2/carbon/identity/oauth/endpoint/authz/OAuth2AuthzEndpoint.java\") { ranges { startingLine endingLine age commit { message url history(first: 2) { edges { node { message url } } } author { name email } } } } } } } }\"}";
    //    
    //    
    //            // passing the access token
    //            h
    //    //        httpPost.addHeader("content-type","application/x-www-form-urlencoded");
    //            httpPost.addHeader("Accept","application/json");
    //    
    //    
    //    
    //            try {
    //    
    //                //            StringEntity params= new StringEntity(query);
    //    
    //    
    //                StringEntity params= new StringEntity(jsonObject.toString());
    //    
    //                httpPost.setEntity(params);
    //                httpResponseFromGraphql= httpClientForGraphql.execute(httpPost);
    //    
    //    
    //                System.out.println("break point before");   
    //    
    //    
    //            } catch (UnsupportedEncodingException e) {
    //                // TODO Auto-generated catch block
    //                e.printStackTrace();
    //            }
    //    
    //    
    //            catch (ClientProtocolException e) {
    //                // TODO Auto-generated catch block
    //                e.printStackTrace();
    //            } catch (IOException e) {
    //                // TODO Auto-generated catch block
    //                e.printStackTrace();
    //            }
    //    
    //            System.out.println("break point before reading the content of the response");
    //    
    //            try{
    //                BufferedReader bufferedReader= new BufferedReader(new InputStreamReader(httpResponseFromGraphql.getEntity().getContent()));
    //    
    //                String line;
    //                StringBuilder stringBuilder= new StringBuilder();
    //    
    //                while((line=bufferedReader.readLine())!= null){
    //                    stringBuilder.append(line);
    //    
    //    
    //    
    //                }
    //    
    //                System.out.println(stringBuilder.toString());
    //            }
    //    
    //            catch(IOException e){
    //                e.printStackTrace();
    //            }
    //    
    //    
    //            System.out.println("Ended ");
    //    
    //    
    //    
    //    
    //    
    //    
    //        }

    //============================= calling the graphql of GitHub through apache httpClient ==========================================

    public void callingGraphqlApi(String repoLocation, String commitHash){


        String owner= StringUtils.substringBefore(repoLocation,"/");
        String repositoryName= StringUtils.substringAfter(repoLocation,"/");



        CloseableHttpClient client= null;
        CloseableHttpResponse response= null;

        client= HttpClients.createDefault();
        HttpPost httpPost= new HttpPost("https://api.github.com/graphql");

        httpPost.addHeader("Authorization","Bearer "+getToken());
        httpPost.addHeader("Accept","application/json");


        //      using an iterator for looping through the arraylists of filenames
        Iterator iteratorForFileNames = fileNames.iterator();

        while (iteratorForFileNames.hasNext()){
            String fileName= (String)iteratorForFileNames.next();



            //        String temp="{viewer {email login }}";

            String temp="{repository(owner:\\\""+owner+"\\\",name:\\\""+repositoryName+"\\\"){object(expression:\\\""+commitHash+"\\\"){ ... on Commit{blame(path:\\\""+fileName+"\\\"){ranges{startingLine endingLine age commit{history(first: 2) { edges { node {  message url } } } author { name email } } } } } } } }";
            //        String temp="{repository(owner:\\\"wso2\\\",name:\\\"product-is\\\"){description}}";



            try {

                StringEntity entity= new StringEntity("{\"query\":\"query "+temp+"\"}");


                httpPost.setEntity(entity);
                response= client.execute(httpPost);

            }

            catch(UnsupportedEncodingException e){
                e.printStackTrace();
            }
            catch(ClientProtocolException e){
                e.printStackTrace();
            }
            catch(IOException e){
                e.printStackTrace();
            }

            try{
                BufferedReader reader= new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                String line= null;
                StringBuilder builder= new StringBuilder();
                while((line=reader.readLine())!= null){

                    builder.append(line);

                }
                System.out.println(builder.toString());
            }
            catch(Exception e){
                e.printStackTrace();
            }




        }



    }


}


